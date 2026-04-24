package com.hogudeul.memorybox.mapper;

import com.hogudeul.memorybox.model.NotificationRow;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface NotificationMapper {

    int insertNotification(@Param("receiverUserId") Long receiverUserId,
                           @Param("actorUserId") Long actorUserId,
                           @Param("notificationType") String notificationType,
                           @Param("targetType") String targetType,
                           @Param("targetId") Long targetId,
                           @Param("message") String message);

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

    Long findCommentOwnerId(@Param("commentId") Long commentId);

    Long findMediaIdByCommentId(@Param("commentId") Long commentId);
}
