package com.hogudeul.memorybox.controller;

import com.hogudeul.memorybox.auth.LoginUserSession;
import com.hogudeul.memorybox.config.KakaoProperties;
import com.hogudeul.memorybox.model.UserAccount;
import com.hogudeul.memorybox.service.AuthService;
import com.hogudeul.memorybox.service.KakaoAuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;

@Controller
public class KakaoAuthController {

    private static final Logger log = LoggerFactory.getLogger(KakaoAuthController.class);
    private static final String KAKAO_AUTHORIZE_URL = "https://kauth.kakao.com/oauth/authorize";
    private final KakaoProperties kakaoProperties;
    private final KakaoAuthService kakaoAuthService;
    private final AuthService authService;

    public KakaoAuthController(KakaoProperties kakaoProperties,
                               KakaoAuthService kakaoAuthService,
                               AuthService authService) {
        this.kakaoProperties = kakaoProperties;
        this.kakaoAuthService = kakaoAuthService;
        this.authService = authService;
    }

    @GetMapping("/auth/kakao/login")
    public String login() {
        String authorizeUrl = UriComponentsBuilder.fromHttpUrl(KAKAO_AUTHORIZE_URL)
                .queryParam("response_type", "code")
                .queryParam("client_id", kakaoProperties.getClientId())
                .queryParam("redirect_uri", resolveLoginRedirectUri())
                .toUriString();
        return "redirect:" + authorizeUrl;
    }

    @GetMapping("/auth/kakao/callback")
    public String callback(@RequestParam(required = false) String code,
                           @RequestParam(required = false) String error,
                           HttpServletRequest request,
                           HttpServletResponse response) {
        if (error != null) {
            return redirectLoginWithError("카카오 로그인에 실패했습니다. 다시 시도해 주세요.");
        }
        if (code == null || code.isBlank()) {
            return redirectLoginWithError("카카오 로그인 인증코드를 확인할 수 없습니다.");
        }

        KakaoAuthService.KakaoLoginResult loginResult = kakaoAuthService.authenticate(code);
        if (!loginResult.isSuccess()) {
            log.info("[kakao-auth] callback failed. failureCode={}, message={}",
                    loginResult.getFailureCode(), loginResult.getMessage());
            return redirectLoginWithError(loginResult.getMessage());
        }

        UserAccount user = loginResult.getUserAccount();
        HttpSession session = request.getSession(true);
        session.setAttribute("loginUser", new LoginUserSession(
                user.getUserId(),
                user.getLoginId(),
                user.getDisplayName(),
                user.getRole()
        ));
        session.setMaxInactiveInterval(60 * 30);
        authService.markSessionAccessUpdatedNow(session);
        authService.handleLoginSuccess(user.getUserId(), false, response);
        return "redirect:/feed";
    }

    private String redirectLoginWithError(String message) {
        String encoded = UriUtils.encode(message, StandardCharsets.UTF_8);
        return "redirect:/login?kakaoError=" + encoded;
    }

    private String resolveLoginRedirectUri() {
        if (kakaoProperties.getLoginRedirectUri() == null || kakaoProperties.getLoginRedirectUri().isBlank()) {
            return kakaoProperties.getRedirectUri();
        }
        return kakaoProperties.getLoginRedirectUri();
    }
}
