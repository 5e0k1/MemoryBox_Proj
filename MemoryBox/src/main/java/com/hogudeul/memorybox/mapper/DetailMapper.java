package com.hogudeul.memorybox.mapper;

import com.hogudeul.memorybox.model.CommentRow;
import com.hogudeul.memorybox.model.MediaDetailRow;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface DetailMapper {

    MediaDetailRow findDetailByMediaId(@Param("mediaId") Long mediaId,
                                       @Param("userId") Long userId);

    List<MediaDetailRow> findDetailsByMediaIds(@Param("mediaIds") List<Long> mediaIds,
                                               @Param("userId") Long userId);

    List<CommentRow> findCommentsByMediaId(@Param("mediaId") Long mediaId);

    CommentRow findCommentById(@Param("commentId") Long commentId);

    Long selectNextCommentId();

    int insertComment(@Param("commentId") Long commentId,
                      @Param("mediaId") Long mediaId,
                      @Param("parentId") Long parentId,
                      @Param("userId") Long userId,
                      @Param("content") String content);

    int insertLike(@Param("mediaId") Long mediaId,
                   @Param("userId") Long userId);

    int deleteLike(@Param("mediaId") Long mediaId,
                   @Param("userId") Long userId);

    Long selectNextDownloadLogId();

    int insertDownloadLog(@Param("dlId") Long dlId,
                          @Param("userId") Long userId,
                          @Param("mediaId") Long mediaId,
                          @Param("ipAddr") String ipAddr,
                          @Param("userAgent") String userAgent,
                          @Param("successYn") String successYn,
                          @Param("failReason") String failReason);
}
