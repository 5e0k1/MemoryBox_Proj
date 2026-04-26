package com.hogudeul.memorybox.controller;

import com.hogudeul.memorybox.config.KakaoProperties;
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

    public KakaoController(KakaoProperties kakaoProperties) {
        this.kakaoProperties = kakaoProperties;
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
        return "redirect:/mypage";
    }
}
