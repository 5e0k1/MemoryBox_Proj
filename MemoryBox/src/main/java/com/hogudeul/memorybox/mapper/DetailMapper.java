package com.hogudeul.memorybox.mapper;

import com.hogudeul.memorybox.model.CommentRow;
import com.hogudeul.memorybox.model.MediaDetailRow;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface DetailMapper {

    MediaDetailRow findDetailByBatchId(@Param("batchId") Long batchId,
                                       @Param("userId") Long userId);

    MediaDetailRow findDetailByMediaId(@Param("mediaId") Long mediaId,
                                       @Param("userId") Long userId);

    List<MediaDetailRow> findDetailsByMediaIds(@Param("mediaIds") List<Long> mediaIds,
                                               @Param("userId") Long userId);

    List<CommentRow> findCommentsByBatchId(@Param("batchId") Long batchId);

    CommentRow findCommentById(@Param("commentId") Long commentId);

    Long selectNextCommentId();

    int insertComment(@Param("commentId") Long commentId,
                      @Param("batchId") Long batchId,
                      @Param("parentId") Long parentId,
                      @Param("userId") Long userId,
                      @Param("content") String content);

    int insertLike(@Param("batchId") Long batchId,
                   @Param("userId") Long userId);

    int deleteLike(@Param("batchId") Long batchId,
                   @Param("userId") Long userId);

    Long selectNextDownloadLogId();

    int insertDownloadLog(@Param("dlId") Long dlId,
                          @Param("userId") Long userId,
                          @Param("mediaId") Long mediaId,
                          @Param("batchId") Long batchId,
                          @Param("downloadType") String downloadType,
                          @Param("ipAddr") String ipAddr,
                          @Param("userAgent") String userAgent,
                          @Param("successYn") String successYn,
                          @Param("failReason") String failReason);

    int updateBatchMeta(@Param("batchId") Long batchId,
                        @Param("userId") Long userId,
                        @Param("title") String title,
                        @Param("albumId") Long albumId);

    int deleteBatchTags(@Param("batchId") Long batchId);

    int insertBatchTag(@Param("batchId") Long batchId,
                       @Param("tagId") Long tagId);
}
