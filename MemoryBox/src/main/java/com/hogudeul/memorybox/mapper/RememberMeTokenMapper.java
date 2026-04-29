package com.hogudeul.memorybox.mapper;

import com.hogudeul.memorybox.model.RememberMeToken;
import java.time.LocalDateTime;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface RememberMeTokenMapper {

    int insertToken(@Param("userId") Long userId,
                    @Param("tokenHash") String tokenHash,
                    @Param("deviceName") String deviceName,
                    @Param("userAgent") String userAgent,
                    @Param("ipAddr") String ipAddr,
                    @Param("expiresAt") LocalDateTime expiresAt);

    RememberMeToken findActiveToken(@Param("tokenHash") String tokenHash);

    int updateLastUsedAt(@Param("tokenId") Long tokenId);

    int revokeTokenByHash(@Param("tokenHash") String tokenHash);

    int revokeAllTokensByUserId(@Param("userId") Long userId);
}
