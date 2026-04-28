package com.hogudeul.memorybox.service;

import com.hogudeul.memorybox.auth.LoginUserSession;
import com.hogudeul.memorybox.config.AuthProperties;
import com.hogudeul.memorybox.mapper.UserMapper;
import com.hogudeul.memorybox.model.UserAccount;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Base64;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RememberMeService {

    public static final String LOGIN_USER_SESSION_KEY = "loginUser";
    public static final String LAST_ACCESS_UPDATED_AT_SESSION_KEY = "lastAccessUpdatedAt";

    private final UserMapper userMapper;
    private final AuthProperties authProperties;

    public RememberMeService(UserMapper userMapper, AuthProperties authProperties) {
        this.userMapper = userMapper;
        this.authProperties = authProperties;
    }

    @Transactional
    public void issueRememberMeToken(Long userId, HttpServletResponse response) {
        String rawToken = generateToken();
        String tokenHash = sha256(rawToken);
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(authProperties.getRememberDays());

        userMapper.updateRememberToken(userId, tokenHash, expiresAt);
        addRememberCookie(response, rawToken, authProperties.getRememberDays() * 24 * 60 * 60);
    }

    @Transactional
    public void clearRememberMeToken(Long userId, HttpServletResponse response) {
        if (userId != null) {
            userMapper.clearRememberToken(userId);
        }
        expireRememberCookie(response);
    }

    @Transactional
    public LoginUserSession tryAutoLogin(HttpServletRequest request, HttpServletResponse response) {
        String rawToken = getRememberCookieValue(request);
        if (rawToken == null || rawToken.isBlank()) {
            return null;
        }

        String tokenHash = sha256(rawToken);
        UserAccount user = userMapper.findByRememberTokenHash(tokenHash);
        if (user == null) {
            user = userMapper.findByRememberTokenLegacyRaw(rawToken);
            if (user != null && user.getRememberTokenExpiresAt() != null && user.getRememberTokenExpiresAt().isAfter(LocalDateTime.now())) {
                userMapper.updateRememberToken(user.getUserId(), tokenHash, user.getRememberTokenExpiresAt());
            }
        }
        if (user == null) {
            expireRememberCookie(response);
            return null;
        }

        if (user.getRememberTokenExpiresAt() == null || user.getRememberTokenExpiresAt().isBefore(LocalDateTime.now())) {
            userMapper.clearRememberToken(user.getUserId());
            expireRememberCookie(response);
            return null;
        }

        // 자동 로그인 시 토큰을 즉시 재발급(회전)하면, 세션이 만료된 직후 동시에 들어온 요청들 간
        // 레이스 컨디션으로 이전 토큰이 무효화되어 간헐적으로 자동로그인이 풀릴 수 있다.
        // (예: 요청 A가 토큰 회전 성공 후, 요청 B가 아직 이전 쿠키로 검증 시도)
        // 따라서 자동 로그인 성공 시에는 기존 토큰을 유지하고, 만료 시점까지 동일 토큰을 사용한다.
        userMapper.updateLastAccessAt(user.getUserId());

        return new LoginUserSession(user.getUserId(), user.getLoginId(), user.getDisplayName(), user.getRole());
    }

    @Transactional
    public void updateLastAccessIfDue(HttpSession session, Long userId) {
        Object lastUpdatedObj = session.getAttribute(LAST_ACCESS_UPDATED_AT_SESSION_KEY);
        LocalDateTime now = LocalDateTime.now();

        if (!(lastUpdatedObj instanceof LocalDateTime lastUpdated)
                || lastUpdated.plusMinutes(authProperties.getLastAccessUpdateMinutes()).isBefore(now)) {
            userMapper.updateLastAccessAt(userId);
            session.setAttribute(LAST_ACCESS_UPDATED_AT_SESSION_KEY, now);
        }
    }

    public void markSessionAccessUpdatedNow(HttpSession session) {
        session.setAttribute(LAST_ACCESS_UPDATED_AT_SESSION_KEY, LocalDateTime.now());
    }

    private String getRememberCookieValue(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }

        for (Cookie cookie : cookies) {
            if (authProperties.getRememberCookieName().equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private String generateToken() {
        byte[] buffer = new byte[48];
        new java.security.SecureRandom().nextBytes(buffer);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(buffer);
    }

    private String sha256(String value) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            byte[] hash = messageDigest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte b : hash) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm is not available", e);
        }
    }

    private void addRememberCookie(HttpServletResponse response, String token, int maxAgeSeconds) {
        ResponseCookie cookie = ResponseCookie.from(authProperties.getRememberCookieName(), token)
                .httpOnly(true)
                .secure(authProperties.isRememberSecure())
                .path("/")
                .sameSite("Lax")
                .maxAge(maxAgeSeconds)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private void expireRememberCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(authProperties.getRememberCookieName(), "")
                .httpOnly(true)
                .secure(authProperties.isRememberSecure())
                .path("/")
                .sameSite("Lax")
                .maxAge(0)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
