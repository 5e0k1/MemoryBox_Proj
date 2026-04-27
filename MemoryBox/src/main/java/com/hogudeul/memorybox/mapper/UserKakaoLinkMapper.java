package com.hogudeul.memorybox.mapper;

import com.hogudeul.memorybox.model.UserKakaoLink;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserKakaoLinkMapper {

    int countByUserId(@Param("userId") Long userId);

    UserKakaoLink findByUserId(@Param("userId") Long userId);

    UserKakaoLink findByKakaoUserId(@Param("kakaoUserId") Long kakaoUserId);

    int insert(UserKakaoLink link);

    int updateByUserId(UserKakaoLink link);

    int updateByKakaoUserId(UserKakaoLink link);
}
