package com.hogudeul.memorybox.mapper;

import com.hogudeul.memorybox.model.MediaItem;
import com.hogudeul.memorybox.model.MediaVariant;
import com.hogudeul.memorybox.model.Tag;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UploadMapper {

    Long selectNextMediaItemId();

    Long selectNextMediaVariantId();

    Long selectNextTagId();

    Long selectNextMediaTagId();

    int insertMediaItem(MediaItem mediaItem);

    int insertMediaVariant(MediaVariant mediaVariant);

    Tag findTagByNormalizedName(@Param("normalizedName") String normalizedName);

    List<Tag> findActiveTagsByUserId(@Param("userId") Long userId);

    List<Tag> findActiveTagsByIds(@Param("tagIds") List<Long> tagIds);

    int insertTag(Tag tag);

    int insertMediaTag(@Param("mdId") Long mdId,
                       @Param("mediaId") Long mediaId,
                       @Param("tagId") Long tagId);
}
