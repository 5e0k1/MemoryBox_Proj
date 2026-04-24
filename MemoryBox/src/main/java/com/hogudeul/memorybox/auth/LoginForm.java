package com.hogudeul.memorybox.auth;

import jakarta.validation.constraints.NotBlank;

public class LoginForm {

    @NotBlank
    private String loginId;

    @NotBlank
    private String password;

    private boolean rememberMe;

    public String getLoginId() {
        return loginId;
    }

    public void setLoginId(String loginId) {
        this.loginId = loginId;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isRememberMe() {
        return rememberMe;
    }

    public void setRememberMe(boolean rememberMe) {
        this.rememberMe = rememberMe;
    }
}
