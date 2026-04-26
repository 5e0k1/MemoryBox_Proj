package com.hogudeul.memorybox.mapper;

import com.hogudeul.memorybox.model.NotificationRow;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface NotificationMapper {

    int insertNotification(NotificationRow notification);

    List<NotificationRow> findTopNotificationsByUserId(@Param("userId") Long userId,
                                                       @Param("limit") int limit);

    int countUnreadByUserId(@Param("userId") Long userId);

    NotificationRow findById(@Param("notificationId") Long notificationId,
                             @Param("userId") Long userId);

    int markAsRead(@Param("notificationId") Long notificationId,
                   @Param("userId") Long userId);

    int deleteNotification(@Param("notificationId") Long notificationId,
                           @Param("userId") Long userId);

    List<Long> findActiveUserIdsExcept(@Param("excludeUserId") Long excludeUserId);

    String findDisplayNameByUserId(@Param("userId") Long userId);

    Long findMediaOwnerId(@Param("mediaId") Long mediaId);

    String findMediaTitleByMediaId(@Param("mediaId") Long mediaId);

    Long findCommentOwnerId(@Param("commentId") Long commentId);

    String findCommentContentByCommentId(@Param("commentId") Long commentId);

    Long findMediaIdByCommentId(@Param("commentId") Long commentId);

    List<Long> findActiveAdminUserIdsExcept(@Param("excludeUserId") Long excludeUserId);

    Long findRequestOwnerId(@Param("requestId") Long requestId);

    Long findRequestIdByRequestCommentId(@Param("requestCommentId") Long requestCommentId);

    List<Long> findPendingUploadPushReceiverUserIds();

    int countPendingUploadPushByUserId(@Param("userId") Long userId);

    List<NotificationRow> findPendingUploadNotificationsByUserId(@Param("userId") Long userId);

    int markPushSentByNotificationIds(@Param("notificationIds") List<Long> notificationIds);

    int markPushSentByNotificationId(@Param("notificationId") Long notificationId);
}
