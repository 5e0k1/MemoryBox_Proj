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

    @Transactional
    public void upsert(UserKakaoLink link) {
        int count = userKakaoLinkMapper.countByUserId(link.getUserId());
        if (count > 0) {
            userKakaoLinkMapper.update(link);
            return;
        }
        userKakaoLinkMapper.insert(link);
    }
}
