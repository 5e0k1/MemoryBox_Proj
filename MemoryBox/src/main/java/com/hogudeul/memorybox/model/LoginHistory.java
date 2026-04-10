package com.hogudeul.memorybox.model;

import java.time.LocalDateTime;

public class LoginHistory {

    private Long lhId;
    private Long userId;
    private String loginIdInput;
    private LocalDateTime loginAt;
    private String ipAddr;
    private String userAgent;
    private String successYn;

    public Long getLhId() {
        return lhId;
    }

    public void setLhId(Long lhId) {
        this.lhId = lhId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getLoginIdInput() {
        return loginIdInput;
    }

    public void setLoginIdInput(String loginIdInput) {
        this.loginIdInput = loginIdInput;
    }

    public LocalDateTime getLoginAt() {
        return loginAt;
    }

    public void setLoginAt(LocalDateTime loginAt) {
        this.loginAt = loginAt;
    }

    public String getIpAddr() {
        return ipAddr;
    }

    public void setIpAddr(String ipAddr) {
        this.ipAddr = ipAddr;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public String getSuccessYn() {
        return successYn;
    }

    public void setSuccessYn(String successYn) {
        this.successYn = successYn;
    }
}
