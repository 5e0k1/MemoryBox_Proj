package com.hogudeul.memorybox.auth;

import com.hogudeul.memorybox.model.UserAccount;

public record LoginResult(boolean success, String message, UserAccount userAccount) {

    public static LoginResult success(UserAccount userAccount) {
        return new LoginResult(true, null, userAccount);
    }

    public static LoginResult fail(String message) {
        return new LoginResult(false, message, null);
    }
}
