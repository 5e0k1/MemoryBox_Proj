package com.hogudeul.memorybox.auth;

public class LoginUserSession {

    private Long userId;
    private String loginId;
    private String displayName;
    private String role;

    public LoginUserSession(Long userId, String loginId, String displayName, String role) {
        this.userId = userId;
        this.loginId = loginId;
        this.displayName = displayName;
        this.role = role;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getLoginId() {
        return loginId;
    }

    public void setLoginId(String loginId) {
        this.loginId = loginId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
