package com.hogudeul.memorybox.auth;

import com.hogudeul.memorybox.model.UserAccount;

public class LoginResult {

    private boolean success;
    private String message;
    private UserAccount userAccount;

    public LoginResult(boolean success, String message, UserAccount userAccount) {
        this.success = success;
        this.message = message;
        this.userAccount = userAccount;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public UserAccount getUserAccount() {
        return userAccount;
    }

    public void setUserAccount(UserAccount userAccount) {
        this.userAccount = userAccount;
    }

    public static LoginResult success(UserAccount userAccount) {
        return new LoginResult(true, null, userAccount);
    }

    public static LoginResult fail(String message) {
        return new LoginResult(false, message, null);
    }
}
