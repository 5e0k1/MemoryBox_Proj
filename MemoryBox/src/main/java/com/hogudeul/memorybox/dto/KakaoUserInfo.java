package com.hogudeul.memorybox.dto;

public class KakaoUserInfo {

    private final Long kakaoUserId;
    private final String nickname;

    public KakaoUserInfo(Long kakaoUserId, String nickname) {
        this.kakaoUserId = kakaoUserId;
        this.nickname = nickname;
    }

    public Long getKakaoUserId() {
        return kakaoUserId;
    }

    public String getNickname() {
        return nickname;
    }
}
