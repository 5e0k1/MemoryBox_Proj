package com.hogudeul.memorybox.service;

import com.hogudeul.memorybox.config.KakaoProperties;
import com.hogudeul.memorybox.dto.KakaoUserInfo;
import com.hogudeul.memorybox.dto.KakaoTokenResponse;
import com.hogudeul.memorybox.dto.KakaoUserResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

@Service
public class KakaoService {

    private static final Logger log = LoggerFactory.getLogger(KakaoService.class);
    private static final String KAKAO_TOKEN_URL = "https://kauth.kakao.com/oauth/token";
    private static final String KAKAO_USER_ME_URL = "https://kapi.kakao.com/v2/user/me";

    private final KakaoProperties kakaoProperties;
    private final RestTemplate restTemplate;

    public KakaoService(KakaoProperties kakaoProperties) {
        this.kakaoProperties = kakaoProperties;
        this.restTemplate = new RestTemplate();
    }

    public KakaoTokenResponse requestToken(String code, String redirectUri) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "authorization_code");
        body.add("client_id", kakaoProperties.getClientId());
        body.add("redirect_uri", redirectUri);
        body.add("code", code);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<KakaoTokenResponse> response = restTemplate.exchange(
                    KAKAO_TOKEN_URL,
                    HttpMethod.POST,
                    request,
                    KakaoTokenResponse.class
            );
            return response.getBody();
        } catch (RestClientResponseException e) {
            log.error("Kakao token API failed. status: {}, body: {}", e.getRawStatusCode(), e.getResponseBodyAsString());
            return null;
        } catch (Exception e) {
            log.error("Kakao token API request failed", e);
            return null;
        }
    }

    public KakaoUserInfo requestUserInfo(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        HttpEntity<Void> request = new HttpEntity<>(headers);

        try {
            ResponseEntity<KakaoUserResponse> response = restTemplate.exchange(
                    KAKAO_USER_ME_URL,
                    HttpMethod.GET,
                    request,
                    KakaoUserResponse.class
            );
            KakaoUserResponse body = response.getBody();
            if (body == null || body.getId() == null) {
                return null;
            }
            String nickname = null;
            String nicknameSource = "none";
            if (body.getKakaoAccount() != null && body.getKakaoAccount().getProfile() != null) {
                nickname = body.getKakaoAccount().getProfile().getNickname();
                if (nickname != null && !nickname.isBlank()) {
                    nicknameSource = "kakao_account.profile.nickname";
                }
            }
            if ((nickname == null || nickname.isBlank()) && body.getProperties() != null) {
                nickname = body.getProperties().getNickname();
                if (nickname != null && !nickname.isBlank()) {
                    nicknameSource = "properties.nickname";
                }
            }
            if (nickname == null || nickname.isBlank()) {
                Boolean needsAgreement = body.getKakaoAccount() != null
                        ? body.getKakaoAccount().getProfileNicknameNeedsAgreement()
                        : null;
                log.info("Kakao user nickname missing. kakaoUserId={}, profileNicknameNeedsAgreement={}, propertiesPresent={}, kakaoAccountPresent={}",
                        body.getId(),
                        needsAgreement,
                        body.getProperties() != null,
                        body.getKakaoAccount() != null);
            } else {
                log.info("Kakao user nickname resolved. kakaoUserId={}, source={}", body.getId(), nicknameSource);
            }
            return new KakaoUserInfo(body.getId(), nickname);
        } catch (RestClientResponseException e) {
            log.error("Kakao user API failed. status: {}, body: {}", e.getRawStatusCode(), e.getResponseBodyAsString());
            return null;
        } catch (Exception e) {
            log.error("Kakao user API request failed", e);
            return null;
        }
    }
}
