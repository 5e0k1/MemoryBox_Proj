package com.hogudeul.memorybox.mapper;

import com.hogudeul.memorybox.model.ShareLink;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ShareLinkMapper {

    int insertShareLink(ShareLink shareLink);

    ShareLink findByToken(@Param("token") String token);

    ShareLink findActiveByToken(@Param("token") String token);

    int revokeShareLink(@Param("shareId") Long shareId,
                        @Param("createdBy") Long createdBy);

    ShareLink findByMediaIdAndCreatedBy(@Param("mediaId") Long mediaId,
                                        @Param("createdBy") Long createdBy);

    List<ShareLink> findValidLinksByMediaId(@Param("mediaId") Long mediaId);
}
