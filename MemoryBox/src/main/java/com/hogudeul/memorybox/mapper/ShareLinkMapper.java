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

    ShareLink findByBatchIdAndCreatedBy(@Param("batchId") Long batchId,
                                        @Param("createdBy") Long createdBy);

    List<ShareLink> findValidLinksByBatchId(@Param("batchId") Long batchId);
}
