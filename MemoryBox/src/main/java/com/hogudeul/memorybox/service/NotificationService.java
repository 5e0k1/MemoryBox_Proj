package com.hogudeul.memorybox.service;

import com.hogudeul.memorybox.mapper.NotificationMapper;
import com.hogudeul.memorybox.model.NotificationRow;
import com.hogudeul.memorybox.model.WebPushSubscription;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final NotificationMapper notificationMapper;
    private final TimeDisplayService timeDisplayService;
    private final WebPushService webPushService;
    private final WebPushSubscriptionService webPushSubscriptionService;

    public NotificationService(NotificationMapper notificationMapper,
                               TimeDisplayService timeDisplayService,
                               WebPushService webPushService,
                               WebPushSubscriptionService webPushSubscriptionService) {
        this.notificationMapper = notificationMapper;
        this.timeDisplayService = timeDisplayService;
        this.webPushService = webPushService;
        this.webPushSubscriptionService = webPushSubscriptionService;
    }

    @Transactional
    public void notifyUpload(Long actorUserId, Long sampleMediaId, int fileCount) {
        if (actorUserId == null || sampleMediaId == null || fileCount <= 0) {
            return;
        }
        String actorName = safeDisplayName(actorUserId);
        String message = actorName + "님이 " + fileCount + "개의 새로운 파일을 업로드했습니다.";

        for (Long receiverId : notificationMapper.findActiveUserIdsExcept(actorUserId)) {
            createNotification(receiverId, actorUserId, "UPLOAD", "MEDIA", sampleMediaId, message);
        }
    }

    @Transactional
    public void notifyComment(Long actorUserId, Long mediaId, Long commentId) {
        notifyMediaOwnerForComment(actorUserId, mediaId, commentId);
    }

    @Transactional
    public void notifyMediaOwnerForComment(Long actorUserId, Long mediaId, Long commentId) {
        if (actorUserId == null || mediaId == null || commentId == null) {
            return;
        }

        Long receiverId = notificationMapper.findMediaOwnerId(mediaId);
        if (receiverId == null || receiverId.equals(actorUserId)) {
            return;
        }

        String actorName = safeDisplayName(actorUserId);
        String mediaTitle = notificationMapper.findMediaTitleByMediaId(mediaId);
        String message = actorName + "님이 " + summarizeTitle(mediaTitle) + " 게시물에 댓글을 작성했습니다.";
        NotificationRow notification = createNotification(receiverId, actorUserId,
                "MEDIA_COMMENT", "COMMENT", commentId, message);

        sendCommentOrReplyPushSafely(notification, "MemoryBox 댓글 알림", "새 댓글이 달렸어요.");
    }

    @Transactional
    public void notifyReply(Long actorUserId, Long parentCommentId, Long replyCommentId) {
        notifyCommentOwnerForReply(actorUserId, parentCommentId, replyCommentId);
    }

    @Transactional
    public void notifyCommentOwnerForReply(Long actorUserId, Long parentCommentId, Long replyCommentId) {
        if (actorUserId == null || parentCommentId == null || replyCommentId == null) {
            return;
        }

        Long receiverId = notificationMapper.findCommentOwnerId(parentCommentId);
        if (receiverId == null || receiverId.equals(actorUserId)) {
            return;
        }

        String parentContent = notificationMapper.findCommentContentByCommentId(parentCommentId);
        String actorName = safeDisplayName(actorUserId);
        String message = "'" + summarize(parentContent) + "' 댓글에 " + actorName + "님이 답글을 작성했습니다.";
        NotificationRow notification = createNotification(receiverId, actorUserId,
                "MEDIA_REPLY", "COMMENT", replyCommentId, message);

        sendCommentOrReplyPushSafely(notification, "MemoryBox 답글 알림", "새 답글이 달렸어요.");
    }

    @Transactional
    public void notifyAdminsForRequestPost(Long actorUserId, Long requestId) {
        if (actorUserId == null || requestId == null) {
            return;
        }
        String actorName = safeDisplayName(actorUserId);
        String message = actorName + "님이 새 요청을 등록했습니다.";
        for (Long receiverId : notificationMapper.findActiveAdminUserIdsExcept(actorUserId)) {
            createNotification(receiverId, actorUserId, "REQUEST_POST", "REQUEST", requestId, message);
        }
    }

    @Transactional
    public void notifyRequestAuthorForComment(Long actorUserId, Long requestId, Long requestCommentId, String requestTitle) {
        if (actorUserId == null || requestId == null || requestCommentId == null) {
            return;
        }
        Long receiverId = notificationMapper.findRequestOwnerId(requestId);
        if (receiverId == null || receiverId.equals(actorUserId)) {
            return;
        }
        String actorName = safeDisplayName(actorUserId);
        String message = "'" + summarizeTitle(requestTitle) + "'요청에 " + actorName + "님이 댓글을 작성했습니다.";
        createNotification(receiverId, actorUserId,
                "REQUEST_COMMENT", "REQUEST_COMMENT", requestCommentId, message);
    }

    @Transactional
    public int sendPendingUploadPushBatch() {
        List<Long> receiverUserIds = notificationMapper.findPendingUploadPushReceiverUserIds();
        if (receiverUserIds == null || receiverUserIds.isEmpty()) {
            return 0;
        }

        int sentUsers = 0;
        int failedUsers = 0;
        for (Long receiverUserId : receiverUserIds) {
            try {
                int count = notificationMapper.countPendingUploadPushByUserId(receiverUserId);
                if (count <= 0) {
                    continue;
                }

                List<WebPushSubscription> subscriptions = webPushSubscriptionService.findActiveByUserId(receiverUserId);
                if (subscriptions == null || subscriptions.isEmpty()) {
                    continue;
                }

                String title = "MemoryBox";
                String body = "새 사진/영상 " + count + "개가 올라왔어요.";
                boolean pushSent = sendPushToAnySubscription(subscriptions, title, body, "/feed");
                if (!pushSent) {
                    failedUsers++;
                    continue;
                }

                List<NotificationRow> pendingRows = notificationMapper.findPendingUploadNotificationsByUserId(receiverUserId);
                if (pendingRows.isEmpty()) {
                    continue;
                }
                List<Long> notificationIds = pendingRows.stream()
                        .map(NotificationRow::getNotificationId)
                        .collect(Collectors.toList());
                notificationMapper.markPushSentByNotificationIds(notificationIds);
                sentUsers++;
            } catch (Exception e) {
                failedUsers++;
                log.warn("Upload batch push user processing failed. userId={}", receiverUserId, e);
            }
        }

        if (sentUsers > 0 || failedUsers > 0) {
            log.info("Upload batch push summary. targetUsers={}, sentUsers={}, failedUsers={}",
                    receiverUserIds.size(), sentUsers, failedUsers);
        }
        return sentUsers;
    }


    public Map<String, Object> getNotificationPanel(Long userId) {
        Map<String, Object> payload = new LinkedHashMap<>();
        if (userId == null) {
            payload.put("unreadCount", 0);
            payload.put("items", List.of());
            return payload;
        }

        int unreadCount = notificationMapper.countUnreadByUserId(userId);
        List<NotificationRow> rows = notificationMapper.findTopNotificationsByUserId(userId, 20);
        List<Map<String, Object>> items = new ArrayList<>();
        for (NotificationRow row : rows) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("notificationId", row.getNotificationId());
            item.put("message", row.getMessage());
            item.put("isRead", "Y".equalsIgnoreCase(row.getIsRead()));
            item.put("createdAt", row.getCreatedAt() == null ? "" : row.getCreatedAt().format(DATE_TIME_FORMAT));
            item.put("relativeCreatedAt", timeDisplayService.formatRelativeUploadedAt(row.getCreatedAt()));
            items.add(item);
        }

        payload.put("unreadCount", unreadCount);
        payload.put("items", items);
        return payload;
    }

    @Transactional
    public String openNotification(Long userId, Long notificationId) {
        NotificationRow row = notificationMapper.findById(notificationId, userId);
        if (row == null) {
            return "/feed";
        }
        notificationMapper.markAsRead(notificationId, userId);

        if ("COMMENT".equalsIgnoreCase(row.getTargetType())) {
            Long mediaId = notificationMapper.findMediaIdByCommentId(row.getTargetId());
            if (mediaId != null) {
                return "/feed/" + mediaId + "#comment-" + row.getTargetId();
            }
        }
        if ("REQUEST".equalsIgnoreCase(row.getTargetType())) {
            return "/requests/" + row.getTargetId();
        }
        if ("REQUEST_COMMENT".equalsIgnoreCase(row.getTargetType())) {
            Long requestId = notificationMapper.findRequestIdByRequestCommentId(row.getTargetId());
            if (requestId != null) {
                return "/requests/" + requestId;
            }
        }
        if ("MEDIA".equalsIgnoreCase(row.getTargetType())) {
            return "/feed/" + row.getTargetId();
        }
        return "/feed";
    }

    @Transactional
    public boolean markRead(Long userId, Long notificationId) {
        if (userId == null || notificationId == null) {
            return false;
        }
        NotificationRow row = notificationMapper.findById(notificationId, userId);
        if (row == null) {
            return false;
        }
        if ("Y".equalsIgnoreCase(row.getIsRead())) {
            return true;
        }
        return notificationMapper.markAsRead(notificationId, userId) > 0;
    }

    @Transactional
    public boolean deleteNotification(Long userId, Long notificationId) {
        if (userId == null || notificationId == null) {
            return false;
        }
        return notificationMapper.deleteNotification(notificationId, userId) > 0;
    }

    private void sendCommentOrReplyPushSafely(NotificationRow notification, String title, String body) {
        if (notification == null || notification.getNotificationId() == null || notification.getReceiverUserId() == null) {
            return;
        }

        try {
            List<WebPushSubscription> subscriptions = webPushSubscriptionService.findActiveByUserId(notification.getReceiverUserId());
            if (subscriptions == null || subscriptions.isEmpty()) {
                return;
            }

            String targetUrl = resolveCommentTargetUrl(notification);
            boolean pushSent = sendPushToAnySubscription(subscriptions, title, body, targetUrl);
            if (!pushSent) {
                log.warn("Comment/reply push failed. userId={}, notificationId={}",
                        notification.getReceiverUserId(), notification.getNotificationId());
                return;
            }

            notificationMapper.markPushSentByNotificationId(notification.getNotificationId());
            log.info("Comment/reply push sent. userId={}, notificationId={}",
                    notification.getReceiverUserId(), notification.getNotificationId());
        } catch (Exception e) {
            log.warn("Comment/reply push error ignored. userId={}, notificationId={}",
                    notification.getReceiverUserId(), notification.getNotificationId(), e);
        }
    }

    private boolean sendPushToAnySubscription(List<WebPushSubscription> subscriptions,
                                              String title,
                                              String body,
                                              String url) {
        for (WebPushSubscription subscription : subscriptions) {
            if (webPushService.sendPush(subscription, title, body, url)) {
                return true;
            }
        }
        return false;
    }

    private String resolveCommentTargetUrl(NotificationRow notification) {
        if (notification == null || notification.getTargetId() == null) {
            return "/mypage";
        }
        Long mediaId = notificationMapper.findMediaIdByCommentId(notification.getTargetId());
        if (mediaId == null) {
            return "/mypage";
        }
        return "/feed/" + mediaId + "#comment-" + notification.getTargetId();
    }

    private NotificationRow createNotification(Long receiverUserId,
                                               Long actorUserId,
                                               String notificationType,
                                               String targetType,
                                               Long targetId,
                                               String message) {
        NotificationRow notification = new NotificationRow();
        notification.setReceiverUserId(receiverUserId);
        notification.setActorUserId(actorUserId);
        notification.setNotificationType(notificationType);
        notification.setTargetType(targetType);
        notification.setTargetId(targetId);
        notification.setMessage(message);
        notificationMapper.insertNotification(notification);
        return notification;
    }

    private String safeDisplayName(Long userId) {
        String displayName = notificationMapper.findDisplayNameByUserId(userId);
        return (displayName == null || displayName.isBlank()) ? "사용자" : displayName;
    }

    private String summarize(String raw) {
        String trimmed = raw == null ? "" : raw.trim();
        if (trimmed.isBlank()) {
            return "내용";
        }
        if (trimmed.length() <= 12) {
            return trimmed;
        }
        return trimmed.substring(0, 12) + "...";
    }

    private String summarizeTitle(String raw) {
        String trimmed = raw == null ? "" : raw.trim();
        if (trimmed.isBlank()) {
            return "게시물";
        }
        if (trimmed.length() <= 6) {
            return trimmed;
        }
        return trimmed.substring(0, 6) + "...";
    }
}
