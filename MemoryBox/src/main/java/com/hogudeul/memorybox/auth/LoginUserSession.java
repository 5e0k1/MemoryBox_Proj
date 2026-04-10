package com.hogudeul.memorybox.auth;

public record LoginUserSession(Long userId, String loginId, String displayName, String role) {
}
