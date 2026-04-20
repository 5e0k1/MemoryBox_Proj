package com.hogudeul.memorybox.mapper;

import com.hogudeul.memorybox.model.FeedRow;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface FeedMapper {

    List<FeedRow> findImageFeedRows();
}
