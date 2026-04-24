package com.hogudeul.memorybox.mapper;

import com.hogudeul.memorybox.model.RequestCommentRow;
import com.hogudeul.memorybox.model.RequestPostRow;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface RequestMapper {

    List<RequestPostRow> findRequestPosts();

    RequestPostRow findRequestPostById(@Param("requestId") Long requestId);

    List<RequestCommentRow> findCommentsByRequestId(@Param("requestId") Long requestId);

    int insertRequestPost(@Param("userId") Long userId,
                          @Param("title") String title,
                          @Param("content") String content);

    int insertRequestComment(@Param("requestId") Long requestId,
                             @Param("userId") Long userId,
                             @Param("content") String content);

    RequestPostRow findLatestRequestPostByUserId(@Param("userId") Long userId);

    RequestCommentRow findLatestRequestCommentByUserId(@Param("requestId") Long requestId,
                                                       @Param("userId") Long userId);
}
