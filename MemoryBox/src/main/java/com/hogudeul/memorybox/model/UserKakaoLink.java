package com.hogudeul.memorybox.model;

import java.time.LocalDateTime;

public class UserKakaoLink {

    private Long userId;
    private Long kakaoUserId;
    private String accessToken;
    private String refreshToken;
    private LocalDateTime accessTokenExpiresAt;
    private LocalDateTime refreshTokenExpiresAt;
    private String useKakaoNotify;
    private String kakaoNickname;
    private String linkStatus;
    private LocalDateTime lastLoginAt;
    private LocalDateTime tokenUpdatedAt;
    private LocalDateTime linkedAt;
    private LocalDateTime updatedAt;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getKakaoUserId() {
        return kakaoUserId;
    }

    public void setKakaoUserId(Long kakaoUserId) {
        this.kakaoUserId = kakaoUserId;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public LocalDateTime getAccessTokenExpiresAt() {
        return accessTokenExpiresAt;
    }

    public void setAccessTokenExpiresAt(LocalDateTime accessTokenExpiresAt) {
        this.accessTokenExpiresAt = accessTokenExpiresAt;
    }

    public LocalDateTime getRefreshTokenExpiresAt() {
        return refreshTokenExpiresAt;
    }

    public void setRefreshTokenExpiresAt(LocalDateTime refreshTokenExpiresAt) {
        this.refreshTokenExpiresAt = refreshTokenExpiresAt;
    }

    public String getUseKakaoNotify() {
        return useKakaoNotify;
    }

    public void setUseKakaoNotify(String useKakaoNotify) {
        this.useKakaoNotify = useKakaoNotify;
    }

    public String getKakaoNickname() {
        return kakaoNickname;
    }

    public void setKakaoNickname(String kakaoNickname) {
        this.kakaoNickname = kakaoNickname;
    }

    public String getLinkStatus() {
        return linkStatus;
    }

    public void setLinkStatus(String linkStatus) {
        this.linkStatus = linkStatus;
    }

    public LocalDateTime getLastLoginAt() {
        return lastLoginAt;
    }

    public void setLastLoginAt(LocalDateTime lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }

    public LocalDateTime getTokenUpdatedAt() {
        return tokenUpdatedAt;
    }

    public void setTokenUpdatedAt(LocalDateTime tokenUpdatedAt) {
        this.tokenUpdatedAt = tokenUpdatedAt;
    }

    public LocalDateTime getLinkedAt() {
        return linkedAt;
    }

    public void setLinkedAt(LocalDateTime linkedAt) {
        this.linkedAt = linkedAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
