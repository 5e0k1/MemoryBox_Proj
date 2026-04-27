package com.hogudeul.memorybox.mapper;

import com.hogudeul.memorybox.model.UserAccount;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserMapper {

    UserAccount findByLoginId(@Param("loginId") String loginId);

    UserAccount findByUserId(@Param("userId") Long userId);

    List<UserAccount> findActiveByDisplayName(@Param("displayName") String displayName);

    UserAccount findByRememberTokenHash(@Param("rememberTokenHash") String rememberTokenHash);

    int updateLastLoginAt(@Param("userId") Long userId);

    int updateLastAccessAt(@Param("userId") Long userId);

    int updateRememberToken(@Param("userId") Long userId,
                            @Param("rememberTokenHash") String rememberTokenHash,
                            @Param("rememberTokenExpiresAt") LocalDateTime rememberTokenExpiresAt);

    int clearRememberToken(@Param("userId") Long userId);

    int updatePasswordHash(@Param("userId") Long userId,
                           @Param("passwordHash") String passwordHash);
}
