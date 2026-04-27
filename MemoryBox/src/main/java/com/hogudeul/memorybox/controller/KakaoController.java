package com.hogudeul.memorybox.controller;

import com.hogudeul.memorybox.auth.LoginUserSession;
import com.hogudeul.memorybox.config.KakaoProperties;
import com.hogudeul.memorybox.dto.KakaoUserInfo;
import com.hogudeul.memorybox.dto.KakaoTokenResponse;
import com.hogudeul.memorybox.model.UserKakaoLink;
import com.hogudeul.memorybox.service.KakaoService;
import com.hogudeul.memorybox.service.UserKakaoLinkService;
import jakarta.servlet.http.HttpSession;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.view.RedirectView;
import org.springframework.web.util.UriComponentsBuilder;

@Controller
public class KakaoController {

    private static final Logger log = LoggerFactory.getLogger(KakaoController.class);
    private static final String KAKAO_AUTHORIZE_URL = "https://kauth.kakao.com/oauth/authorize";
    private final KakaoProperties kakaoProperties;
    private final KakaoService kakaoService;
    private final UserKakaoLinkService userKakaoLinkService;

    public KakaoController(KakaoProperties kakaoProperties,
                           KakaoService kakaoService,
                           UserKakaoLinkService userKakaoLinkService) {
        this.kakaoProperties = kakaoProperties;
        this.kakaoService = kakaoService;
        this.userKakaoLinkService = userKakaoLinkService;
    }

    @GetMapping("/kakao/connect")
    public RedirectView connect() {
        String authorizeUrl = UriComponentsBuilder
                .fromHttpUrl(KAKAO_AUTHORIZE_URL)
                .queryParam("response_type", "code")
                .queryParam("client_id", kakaoProperties.getClientId())
                .queryParam("redirect_uri", kakaoProperties.getRedirectUri())
                .toUriString();
        return new RedirectView(authorizeUrl);
    }

    @GetMapping("/kakao/callback")
    public String callback(@RequestParam String code, HttpSession session) {
        LoginUserSession loginUser = (LoginUserSession) session.getAttribute("loginUser");
        if (loginUser == null) {
            return "redirect:/login?expired=true";
        }

        Long userId = loginUser.getUserId();
        log.info("Kakao authorization code received: {}", code);

        KakaoTokenResponse tokenResponse = kakaoService.requestToken(code, kakaoProperties.getRedirectUri());
        if (tokenResponse == null) {
            log.warn("Kakao token response is null. userId={}", userId);
            return "redirect:/mypage";
        }
        log.info("Kakao token issued for userId={}. accessToken={}, refreshToken={}",
                userId,
                maskToken(tokenResponse.getAccessToken()),
                maskToken(tokenResponse.getRefreshToken()));

        KakaoUserInfo userInfo = kakaoService.requestUserInfo(tokenResponse.getAccessToken());
        if (userInfo == null || userInfo.getKakaoUserId() == null) {
            log.warn("Kakao user info response is invalid. userId={}", userId);
            return "redirect:/mypage";
        }

        Long kakaoUserId = userInfo.getKakaoUserId();
        log.info("Kakao user info received. kakaoUserId={}", kakaoUserId);

        UserKakaoLink link = new UserKakaoLink();
        link.setUserId(userId);
        link.setKakaoUserId(kakaoUserId);
        link.setAccessToken(tokenResponse.getAccessToken());
        link.setRefreshToken(tokenResponse.getRefreshToken());
        link.setAccessTokenExpiresAt(resolveExpiresAt(tokenResponse.getExpiresIn()));
        link.setRefreshTokenExpiresAt(resolveExpiresAt(tokenResponse.getRefreshTokenExpiresIn()));
        link.setUseKakaoNotify("Y");

        userKakaoLinkService.upsert(link);
        log.info("Kakao link saved. userId={}, kakaoUserId={}", userId, kakaoUserId);

        return "redirect:/mypage";
    }

    private LocalDateTime resolveExpiresAt(Long expiresInSeconds) {
        if (expiresInSeconds == null || expiresInSeconds <= 0) {
            return null;
        }
        return LocalDateTime.now().plusSeconds(expiresInSeconds);
    }

    private String maskToken(String token) {
        if (token == null || token.isBlank()) {
            return "(empty)";
        }
        int visibleLength = Math.min(token.length(), 16);
        return token.substring(0, visibleLength) + "...";
    }
}
