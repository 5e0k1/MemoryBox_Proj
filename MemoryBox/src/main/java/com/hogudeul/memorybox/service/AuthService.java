package com.hogudeul.memorybox.service;

import com.hogudeul.memorybox.auth.LoginResult;
import com.hogudeul.memorybox.auth.LoginUserSession;
import com.hogudeul.memorybox.mapper.LoginHistoryMapper;
import com.hogudeul.memorybox.mapper.UserMapper;
import com.hogudeul.memorybox.model.LoginHistory;
import com.hogudeul.memorybox.model.UserAccount;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.time.LocalDateTime;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserMapper userMapper;
    private final LoginHistoryMapper loginHistoryMapper;
    private final PasswordEncoder passwordEncoder;
    private final RememberMeService rememberMeService;

    public AuthService(UserMapper userMapper,
                       LoginHistoryMapper loginHistoryMapper,
                       PasswordEncoder passwordEncoder,
                       RememberMeService rememberMeService) {
        this.userMapper = userMapper;
        this.loginHistoryMapper = loginHistoryMapper;
        this.passwordEncoder = passwordEncoder;
        this.rememberMeService = rememberMeService;
    }

    @Transactional
    public LoginResult login(String loginId, String rawPassword, String ipAddr, String userAgent) {
        UserAccount user = userMapper.findByLoginId(loginId);

        if (user == null) {
            saveLoginHistory(null, loginId, ipAddr, userAgent, "N");
            return LoginResult.fail("존재하지 않는 아이디입니다.");
        }

        if (!"N".equalsIgnoreCase(user.getDelYn())) {
            saveLoginHistory(user.getUserId(), loginId, ipAddr, userAgent, "N");
            return LoginResult.fail("비활성화된 계정입니다.");
        }

        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            saveLoginHistory(user.getUserId(), loginId, ipAddr, userAgent, "N");
            return LoginResult.fail("비밀번호가 일치하지 않습니다.");
        }

        userMapper.updateLastLoginAt(user.getUserId());
        saveLoginHistory(user.getUserId(), loginId, ipAddr, userAgent, "Y");
        return LoginResult.success(user);
    }

    @Transactional
    public LoginUserSession tryAutoLogin(HttpServletRequest request, HttpServletResponse response) {
        return rememberMeService.tryAutoLogin(request, response);
    }

    @Transactional
    public void handleLoginSuccess(Long userId, boolean rememberMe, HttpServletResponse response) {
        if (rememberMe) {
            rememberMeService.issueRememberMeToken(userId, response);
            return;
        }
        rememberMeService.clearRememberMeTokenByCurrentCookie(null, response);
    }

    @Transactional
    public void logout(Long userId, HttpServletRequest request, HttpServletResponse response) {
        rememberMeService.clearRememberMeTokenByCurrentCookie(request, response);
    }

    @Transactional
    public String changePassword(Long userId, String currentPassword, String newPassword, String newPasswordConfirm) {
        UserAccount user = userMapper.findByUserId(userId);
        if (user == null) {
            return "사용자 정보를 찾을 수 없습니다.";
        }

        if (!"N".equalsIgnoreCase(user.getDelYn())) {
            return "비활성화된 계정은 비밀번호를 변경할 수 없습니다.";
        }

        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            return "현재 비밀번호가 일치하지 않습니다.";
        }

        if (newPassword == null || newPassword.length() < 8) {
            return "새 비밀번호는 8자 이상이어야 합니다.";
        }

        if (!newPassword.equals(newPasswordConfirm)) {
            return "새 비밀번호와 재확인이 일치하지 않습니다.";
        }

        if (passwordEncoder.matches(newPassword, user.getPasswordHash())) {
            return "현재 비밀번호와 동일한 비밀번호로는 변경할 수 없습니다.";
        }

        userMapper.updatePasswordHash(userId, passwordEncoder.encode(newPassword));
        rememberMeService.revokeAllRememberMeTokens(userId);
        return null;
    }

    @Transactional
    public void clearRememberTokenForSecurityEvent(Long userId) {
        // TODO: 관리자 비밀번호 초기화/계정 잠금 해제 플로우에서 본 메서드를 호출해 기존 자동로그인을 강제 해제하세요.
        rememberMeService.revokeAllRememberMeTokens(userId);
    }

    @Transactional
    public void updateLastAccessIfDue(HttpSession session, Long userId) {
        rememberMeService.updateLastAccessIfDue(session, userId);
    }

    public void markSessionAccessUpdatedNow(HttpSession session) {
        rememberMeService.markSessionAccessUpdatedNow(session);
    }

    private void saveLoginHistory(Long userId, String loginIdInput, String ipAddr, String userAgent, String successYn) {
        LoginHistory loginHistory = new LoginHistory();
        loginHistory.setLhId(loginHistoryMapper.selectNextLoginHistoryId());
        loginHistory.setUserId(userId);
        loginHistory.setLoginIdInput(loginIdInput);
        loginHistory.setLoginAt(LocalDateTime.now());
        loginHistory.setIpAddr(ipAddr);
        loginHistory.setUserAgent(userAgent);
        loginHistory.setSuccessYn(successYn);
        loginHistoryMapper.insert(loginHistory);
    }
}
