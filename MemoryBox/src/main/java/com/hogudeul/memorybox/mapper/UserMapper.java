package com.hogudeul.memorybox.mapper;

import com.hogudeul.memorybox.model.UserAccount;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserMapper {

    UserAccount findByLoginId(@Param("loginId") String loginId);

    UserAccount findByUserId(@Param("userId") Long userId);

    int updateLastLoginAt(@Param("userId") Long userId);

    int updatePasswordHash(@Param("userId") Long userId,
                           @Param("passwordHash") String passwordHash);
}
