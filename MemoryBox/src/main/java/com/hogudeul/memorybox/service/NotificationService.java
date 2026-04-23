package com.hogudeul.memorybox.service;

import com.hogudeul.memorybox.mapper.NotificationMapper;
import com.hogudeul.memorybox.model.NotificationRow;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {

    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final NotificationMapper notificationMapper;
    private final TimeDisplayService timeDisplayService;

    public NotificationService(NotificationMapper notificationMapper, TimeDisplayService timeDisplayService) {
        this.notificationMapper = notificationMapper;
        this.timeDisplayService = timeDisplayService;
    }

    @Transactional
    public void notifyUpload(Long actorUserId, Long sampleMediaId, int fileCount) {
        if (actorUserId == null || sampleMediaId == null || fileCount <= 0) {
            return;
        }
        String actorName = safeDisplayName(actorUserId);
        String message = actorName + "님이 " + fileCount + "개의 새로운 파일을 업로드했습니다.";

        for (Long receiverId : notificationMapper.findActiveUserIdsExcept(actorUserId)) {
            notificationMapper.insertNotification(receiverId, actorUserId,
                    "UPLOAD", "MEDIA", sampleMediaId, message);
        }
    }

    @Transactional
    public void notifyComment(Long actorUserId, Long mediaId, Long commentId, String contentPreview) {
        notifyMediaOwnerForComment(actorUserId, mediaId, commentId, contentPreview);
    }

    @Transactional
    public void notifyMediaOwnerForComment(Long actorUserId, Long mediaId, Long commentId, String contentPreview) {
        if (actorUserId == null || mediaId == null || commentId == null) {
            return;
        }

        Long receiverId = notificationMapper.findMediaOwnerId(mediaId);
        if (receiverId == null || receiverId.equals(actorUserId)) {
            return;
        }

        String actorName = safeDisplayName(actorUserId);
        String message = actorName + "님이 " + summarize(contentPreview) + " 게시물에 댓글을 작성했습니다.";
        notificationMapper.insertNotification(receiverId, actorUserId,
                "MEDIA_COMMENT", "COMMENT", commentId, message);
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

        String actorName = safeDisplayName(actorUserId);
        String message = actorName + "님이 답글을 작성했습니다.";
        notificationMapper.insertNotification(receiverId, actorUserId,
                "MEDIA_REPLY", "COMMENT", replyCommentId, message);
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

    private String safeDisplayName(Long userId) {
        String displayName = notificationMapper.findDisplayNameByUserId(userId);
        return (displayName == null || displayName.isBlank()) ? "사용자" : displayName;
    }

    private String summarize(String raw) {
        String trimmed = raw == null ? "" : raw.trim();
        if (trimmed.isBlank()) {
            return "게시물";
        }
        if (trimmed.length() <= 12) {
            return trimmed;
        }
        return trimmed.substring(0, 12) + "...";
    }
}
