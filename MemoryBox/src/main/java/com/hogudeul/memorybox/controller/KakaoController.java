package com.hogudeul.memorybox.controller;

import com.hogudeul.memorybox.config.KakaoProperties;
import com.hogudeul.memorybox.dto.KakaoTokenResponse;
import com.hogudeul.memorybox.service.KakaoService;
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

    public KakaoController(KakaoProperties kakaoProperties, KakaoService kakaoService) {
        this.kakaoProperties = kakaoProperties;
        this.kakaoService = kakaoService;
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
    public String callback(@RequestParam String code) {
        log.info("Kakao authorization code received: {}", code);

        KakaoTokenResponse tokenResponse = kakaoService.requestToken(code);
        if (tokenResponse == null) {
            log.warn("Kakao token response is null");
            return "redirect:/mypage";
        }

        log.info("Kakao access_token issued: {}", maskToken(tokenResponse.getAccessToken()));
        log.info("Kakao refresh_token issued: {}", maskToken(tokenResponse.getRefreshToken()));
        log.info("Kakao expires_in: {}", tokenResponse.getExpiresIn());
        log.info("Kakao refresh_token_expires_in: {}", tokenResponse.getRefreshTokenExpiresIn());

        return "redirect:/mypage";
    }

    private String maskToken(String token) {
        if (token == null || token.isBlank()) {
            return "(empty)";
        }
        int visibleLength = Math.min(token.length(), 16);
        return token.substring(0, visibleLength) + "...";
    }
}
