package com.hogudeul.memorybox.mapper;

import com.hogudeul.memorybox.model.MediaItem;
import com.hogudeul.memorybox.model.MediaVariant;
import com.hogudeul.memorybox.model.Tag;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UploadMapper {

    int insertMediaItem(MediaItem mediaItem);

    int insertMediaVariant(MediaVariant mediaVariant);

    Tag findTagByNormalizedName(@Param("normalizedName") String normalizedName);

    int insertTag(Tag tag);

    int insertMediaTag(@Param("mediaId") Long mediaId, @Param("tagId") Long tagId);
}
