package com.hogudeul.memorybox.mapper;

import com.hogudeul.memorybox.model.UserKakaoLink;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserKakaoLinkMapper {

    int countByUserId(@Param("userId") Long userId);

    int insert(UserKakaoLink link);

    int update(UserKakaoLink link);
}
