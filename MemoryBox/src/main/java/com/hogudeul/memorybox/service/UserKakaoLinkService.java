package com.hogudeul.memorybox.service;

import com.hogudeul.memorybox.mapper.UserKakaoLinkMapper;
import com.hogudeul.memorybox.model.UserKakaoLink;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserKakaoLinkService {

    private final UserKakaoLinkMapper userKakaoLinkMapper;

    public UserKakaoLinkService(UserKakaoLinkMapper userKakaoLinkMapper) {
        this.userKakaoLinkMapper = userKakaoLinkMapper;
    }

    public boolean isLinked(Long userId) {
        if (userId == null) {
            return false;
        }
        return userKakaoLinkMapper.countByUserId(userId) > 0;
    }

    @Transactional
    public void upsert(UserKakaoLink link) {
        int count = userKakaoLinkMapper.countByUserId(link.getUserId());
        if (count > 0) {
            userKakaoLinkMapper.updateByUserId(link);
            return;
        }
        userKakaoLinkMapper.insert(link);
    }
}
