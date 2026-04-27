package com.hogudeul.memorybox.service;

import com.hogudeul.memorybox.config.KakaoProperties;
import com.hogudeul.memorybox.dto.KakaoTokenResponse;
import com.hogudeul.memorybox.dto.KakaoUserInfo;
import com.hogudeul.memorybox.mapper.UserKakaoLinkMapper;
import com.hogudeul.memorybox.mapper.UserMapper;
import com.hogudeul.memorybox.model.UserAccount;
import com.hogudeul.memorybox.model.UserKakaoLink;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class KakaoAuthService {

    private final KakaoService kakaoService;
    private final KakaoProperties kakaoProperties;
    private final UserKakaoLinkMapper userKakaoLinkMapper;
    private final UserMapper userMapper;
    private final KakaoTokenCryptoService kakaoTokenCryptoService;

    public KakaoAuthService(KakaoService kakaoService,
                            KakaoProperties kakaoProperties,
                            UserKakaoLinkMapper userKakaoLinkMapper,
                            UserMapper userMapper,
                            KakaoTokenCryptoService kakaoTokenCryptoService) {
        this.kakaoService = kakaoService;
        this.kakaoProperties = kakaoProperties;
        this.userKakaoLinkMapper = userKakaoLinkMapper;
        this.userMapper = userMapper;
        this.kakaoTokenCryptoService = kakaoTokenCryptoService;
    }

    @Transactional
    public KakaoLoginResult authenticate(String code) {
        KakaoTokenResponse token = kakaoService.requestToken(code, resolveLoginRedirectUri());
        if (token == null || isBlank(token.getAccessToken())) {
            return KakaoLoginResult.fail("카카오 로그인 처리에 실패했습니다. 잠시 후 다시 시도해 주세요.");
        }

        KakaoUserInfo userInfo = kakaoService.requestUserInfo(token.getAccessToken());
        if (userInfo == null || userInfo.getKakaoUserId() == null) {
            return KakaoLoginResult.fail("카카오 사용자 정보를 확인할 수 없습니다.");
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime accessTokenExpiresAt = resolveExpiresAt(token.getExpiresIn(), now);
        LocalDateTime refreshTokenExpiresAt = resolveExpiresAt(token.getRefreshTokenExpiresIn(), now);

        UserKakaoLink existingByKakao = userKakaoLinkMapper.findByKakaoUserId(userInfo.getKakaoUserId());
        if (existingByKakao != null) {
            UserAccount linkedUser = userMapper.findByUserId(existingByKakao.getUserId());
            if (linkedUser == null || !"N".equalsIgnoreCase(linkedUser.getDelYn())) {
                return KakaoLoginResult.fail("연결된 계정을 사용할 수 없습니다. 관리자에게 문의하세요.");
            }
            updateExistingLinkByKakaoId(userInfo, token, accessTokenExpiresAt, refreshTokenExpiresAt, now);
            userMapper.updateLastLoginAt(linkedUser.getUserId());
            return KakaoLoginResult.success(linkedUser);
        }

        String matchingName = extractMatchingName(userInfo.getNickname());
        if (isBlank(matchingName)) {
            return KakaoLoginResult.fail("등록된 사용자와 카카오 계정을 자동으로 연결하지 못했습니다. 관리자에게 문의하세요.");
        }

        List<UserAccount> matches = userMapper.findActiveByDisplayName(matchingName);
        if (matches.size() != 1) {
            return KakaoLoginResult.fail("등록된 사용자와 카카오 계정을 자동으로 연결하지 못했습니다. 관리자에게 문의하세요.");
        }

        UserAccount matchedUser = matches.get(0);
        if (userKakaoLinkMapper.findByUserId(matchedUser.getUserId()) != null) {
            return KakaoLoginResult.fail("이미 다른 카카오 계정이 연결된 사용자입니다.");
        }

        UserKakaoLink newLink = buildLink(userInfo, token, matchedUser.getUserId(), accessTokenExpiresAt, refreshTokenExpiresAt, now);
        userKakaoLinkMapper.insert(newLink);
        userMapper.updateLastLoginAt(matchedUser.getUserId());
        return KakaoLoginResult.success(matchedUser);
    }

    private void updateExistingLinkByKakaoId(KakaoUserInfo userInfo,
                                             KakaoTokenResponse token,
                                             LocalDateTime accessTokenExpiresAt,
                                             LocalDateTime refreshTokenExpiresAt,
                                             LocalDateTime now) {
        UserKakaoLink link = new UserKakaoLink();
        link.setKakaoUserId(userInfo.getKakaoUserId());
        link.setAccessToken(kakaoTokenCryptoService.encrypt(token.getAccessToken()));
        link.setRefreshToken(isBlank(token.getRefreshToken()) ? null : kakaoTokenCryptoService.encrypt(token.getRefreshToken()));
        link.setAccessTokenExpiresAt(accessTokenExpiresAt);
        link.setRefreshTokenExpiresAt(refreshTokenExpiresAt);
        link.setUseKakaoNotify("Y");
        link.setKakaoNickname(userInfo.getNickname());
        link.setLinkStatus("ACTIVE");
        link.setLastLoginAt(now);
        link.setTokenUpdatedAt(now);
        userKakaoLinkMapper.updateByKakaoUserId(link);
    }

    private UserKakaoLink buildLink(KakaoUserInfo userInfo,
                                    KakaoTokenResponse token,
                                    Long userId,
                                    LocalDateTime accessTokenExpiresAt,
                                    LocalDateTime refreshTokenExpiresAt,
                                    LocalDateTime now) {
        UserKakaoLink link = new UserKakaoLink();
        link.setUserId(userId);
        link.setKakaoUserId(userInfo.getKakaoUserId());
        link.setAccessToken(kakaoTokenCryptoService.encrypt(token.getAccessToken()));
        link.setRefreshToken(isBlank(token.getRefreshToken()) ? null : kakaoTokenCryptoService.encrypt(token.getRefreshToken()));
        link.setAccessTokenExpiresAt(accessTokenExpiresAt);
        link.setRefreshTokenExpiresAt(refreshTokenExpiresAt);
        link.setUseKakaoNotify("Y");
        link.setKakaoNickname(userInfo.getNickname());
        link.setLinkStatus("ACTIVE");
        link.setLastLoginAt(now);
        link.setTokenUpdatedAt(now);
        return link;
    }

    private LocalDateTime resolveExpiresAt(Long expiresInSeconds, LocalDateTime baseTime) {
        if (expiresInSeconds == null || expiresInSeconds <= 0) {
            return null;
        }
        return baseTime.plusSeconds(expiresInSeconds);
    }

    private String extractMatchingName(String kakaoNickname) {
        if (isBlank(kakaoNickname)) {
            return "";
        }
        String compact = kakaoNickname.replaceAll("\\s+", "");
        if (compact.length() <= 2) {
            return compact;
        }
        return compact.substring(compact.length() - 2);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String resolveLoginRedirectUri() {
        if (!isBlank(kakaoProperties.getLoginRedirectUri())) {
            return kakaoProperties.getLoginRedirectUri();
        }
        return kakaoProperties.getRedirectUri();
    }

    public static class KakaoLoginResult {
        private final boolean success;
        private final UserAccount userAccount;
        private final String message;

        private KakaoLoginResult(boolean success, UserAccount userAccount, String message) {
            this.success = success;
            this.userAccount = userAccount;
            this.message = message;
        }

        public static KakaoLoginResult success(UserAccount userAccount) {
            return new KakaoLoginResult(true, userAccount, null);
        }

        public static KakaoLoginResult fail(String message) {
            return new KakaoLoginResult(false, null, message);
        }

        public boolean isSuccess() {
            return success;
        }

        public UserAccount getUserAccount() {
            return userAccount;
        }

        public String getMessage() {
            return message;
        }
    }
}
