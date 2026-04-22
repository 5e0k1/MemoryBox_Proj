package com.hogudeul.memorybox.mapper;

import com.hogudeul.memorybox.model.FeedRow;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface FeedMapper {

    List<FeedRow> findFeedRows(@Param("mediaType") String mediaType,
                               @Param("author") String author,
                               @Param("album") String album,
                               @Param("tag") String tag,
                               @Param("sort") String sort,
                               @Param("userId") Long userId,
                               @Param("likedOnly") boolean likedOnly,
                               @Param("mineOnly") boolean mineOnly,
                               @Param("limit") int limit,
                               @Param("offset") int offset);

    List<FeedRow> findImageFeedRows();
}
