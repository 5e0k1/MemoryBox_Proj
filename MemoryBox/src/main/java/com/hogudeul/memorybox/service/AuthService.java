package com.hogudeul.memorybox.service;

import com.hogudeul.memorybox.auth.LoginResult;
import com.hogudeul.memorybox.mapper.LoginHistoryMapper;
import com.hogudeul.memorybox.mapper.UserMapper;
import com.hogudeul.memorybox.model.LoginHistory;
import com.hogudeul.memorybox.model.UserAccount;
import java.time.LocalDateTime;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserMapper userMapper;
    private final LoginHistoryMapper loginHistoryMapper;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserMapper userMapper, LoginHistoryMapper loginHistoryMapper, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.loginHistoryMapper = loginHistoryMapper;
        this.passwordEncoder = passwordEncoder;
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
