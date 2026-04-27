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
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class KakaoAuthService {

    private static final Logger log = LoggerFactory.getLogger(KakaoAuthService.class);
    private static final String FRIENDLY_AUTO_LINK_FAIL_MESSAGE = "등록된 사용자와 카카오 계정을 자동으로 연결하지 못했습니다. 관리자에게 문의하세요.";
    private static final String FRIENDLY_ALREADY_LINKED_MESSAGE = "이미 다른 카카오 계정이 연결된 사용자입니다.";
    private static final String FRIENDLY_KAKAO_FAIL_MESSAGE = "카카오 로그인 처리에 실패했습니다. 잠시 후 다시 시도해 주세요.";

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
            log.info("[kakao-auth] token exchange failed. tokenResponseNull={}, accessTokenMissing={}",
                    token == null, token != null && isBlank(token.getAccessToken()));
            return KakaoLoginResult.fail(FailureCode.KAKAO_LINK_DB_ERROR, FRIENDLY_KAKAO_FAIL_MESSAGE);
        }

        KakaoUserInfo userInfo = kakaoService.requestUserInfo(token.getAccessToken());
        if (userInfo == null || userInfo.getKakaoUserId() == null) {
            log.info("[kakao-auth] user info fetch failed. userInfoNull={}, kakaoUserIdMissing={}",
                    userInfo == null, userInfo != null && userInfo.getKakaoUserId() == null);
            return KakaoLoginResult.fail(FailureCode.KAKAO_LINK_DB_ERROR, "카카오 사용자 정보를 확인할 수 없습니다.");
        }

        String kakaoNickname = userInfo.getNickname();
        String normalizedNickname = normalizeNickname(kakaoNickname);
        String matchingName = extractMatchingName(kakaoNickname);
        log.info("[kakao-auth] user info. kakao_user_id={}, kakao_nickname={}, normalized_nickname={}, matchName={}",
                userInfo.getKakaoUserId(), kakaoNickname, normalizedNickname, matchingName);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime accessTokenExpiresAt = resolveExpiresAt(token.getExpiresIn(), now);
        LocalDateTime refreshTokenExpiresAt = resolveExpiresAt(token.getRefreshTokenExpiresIn(), now);

        UserKakaoLink existingByKakao = userKakaoLinkMapper.findByKakaoUserId(userInfo.getKakaoUserId());
        log.info("[kakao-auth] existing link by kakao_user_id. kakao_user_id={}, found={}",
                userInfo.getKakaoUserId(), summarizeLink(existingByKakao));
        if (existingByKakao != null) {
            UserAccount linkedUser = userMapper.findByUserId(existingByKakao.getUserId());
            if (linkedUser == null || !"N".equalsIgnoreCase(linkedUser.getDelYn())) {
                log.info("[kakao-auth] linked user invalid. kakao_user_id={}, user_id={}, userExists={}, delYn={}",
                        userInfo.getKakaoUserId(),
                        existingByKakao.getUserId(),
                        linkedUser != null,
                        linkedUser == null ? "(null)" : linkedUser.getDelYn());
                return KakaoLoginResult.fail(FailureCode.KAKAO_USER_NOT_MATCHED, FRIENDLY_AUTO_LINK_FAIL_MESSAGE);
            }
            try {
                int updateCount = updateExistingLinkByKakaoId(userInfo, token, accessTokenExpiresAt, refreshTokenExpiresAt, now);
                userMapper.updateLastLoginAt(linkedUser.getUserId());
                log.info("[kakao-auth] existing link updated. kakao_user_id={}, user_id={}, updateCount={}",
                        userInfo.getKakaoUserId(), linkedUser.getUserId(), updateCount);
            } catch (Exception e) {
                log.error("[kakao-auth] link update failed. kakao_user_id={}, user_id={}",
                        userInfo.getKakaoUserId(), existingByKakao.getUserId(), e);
                return KakaoLoginResult.fail(FailureCode.KAKAO_LINK_DB_ERROR, FRIENDLY_KAKAO_FAIL_MESSAGE);
            }
            return KakaoLoginResult.success(linkedUser);
        }

        if (isBlank(matchingName)) {
            log.info("[kakao-auth] match failed. reason=empty_matchName, kakao_user_id={}, kakao_nickname={}, normalized_nickname={}, matchName={}",
                    userInfo.getKakaoUserId(), kakaoNickname, normalizedNickname, matchingName);
            return KakaoLoginResult.fail(FailureCode.KAKAO_USER_NOT_MATCHED, FRIENDLY_AUTO_LINK_FAIL_MESSAGE);
        }

        List<UserAccount> matches = userMapper.findActiveByDisplayName(matchingName);
        log.info("[kakao-auth] user match lookup. kakao_user_id={}, matchName={}, matchedCount={}, matchedUsers={}",
                userInfo.getKakaoUserId(),
                matchingName,
                matches.size(),
                summarizeUsers(matches));
        if (matches.isEmpty()) {
            log.info("[kakao-auth] match failed. reason=no_user, kakao_user_id={}, kakao_nickname={}, normalized_nickname={}, matchName={}",
                    userInfo.getKakaoUserId(), kakaoNickname, normalizedNickname, matchingName);
            return KakaoLoginResult.fail(FailureCode.KAKAO_USER_NOT_MATCHED, FRIENDLY_AUTO_LINK_FAIL_MESSAGE);
        }
        if (matches.size() > 1) {
            log.info("[kakao-auth] match failed. reason=duplicated_users, kakao_user_id={}, matchName={}, matchedUsers={}",
                    userInfo.getKakaoUserId(), matchingName, summarizeUsers(matches));
            return KakaoLoginResult.fail(FailureCode.KAKAO_USER_DUPLICATED, FRIENDLY_AUTO_LINK_FAIL_MESSAGE);
        }

        UserAccount matchedUser = matches.get(0);
        UserKakaoLink existingByUser = userKakaoLinkMapper.findByUserId(matchedUser.getUserId());
        log.info("[kakao-auth] existing link by user_id. user_id={}, found={}",
                matchedUser.getUserId(), summarizeLink(existingByUser));
        if (existingByUser != null) {
            return KakaoLoginResult.fail(FailureCode.KAKAO_ALREADY_LINKED_OTHER, FRIENDLY_ALREADY_LINKED_MESSAGE);
        }

        UserKakaoLink newLink = buildLink(userInfo, token, matchedUser.getUserId(), accessTokenExpiresAt, refreshTokenExpiresAt, now);
        try {
            int insertCount = userKakaoLinkMapper.insert(newLink);
            userMapper.updateLastLoginAt(matchedUser.getUserId());
            log.info("[kakao-auth] new link inserted. kakao_user_id={}, user_id={}, insertCount={}",
                    userInfo.getKakaoUserId(), matchedUser.getUserId(), insertCount);
        } catch (Exception e) {
            log.error("[kakao-auth] link insert failed. kakao_user_id={}, user_id={}",
                    userInfo.getKakaoUserId(), matchedUser.getUserId(), e);
            return KakaoLoginResult.fail(FailureCode.KAKAO_LINK_DB_ERROR, FRIENDLY_KAKAO_FAIL_MESSAGE);
        }
        return KakaoLoginResult.success(matchedUser);
    }

    private int updateExistingLinkByKakaoId(KakaoUserInfo userInfo,
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
        return userKakaoLinkMapper.updateByKakaoUserId(link);
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
        String compact = normalizeNickname(kakaoNickname);
        if (isBlank(compact)) {
            return "";
        }
        if (compact.length() <= 2) {
            return compact;
        }
        return compact.substring(compact.length() - 2);
    }

    private String normalizeNickname(String kakaoNickname) {
        if (kakaoNickname == null) {
            return "";
        }
        return kakaoNickname.replaceAll("\\s+", "");
    }

    private String summarizeUsers(List<UserAccount> users) {
        if (users == null || users.isEmpty()) {
            return "[]";
        }
        return users.stream()
                .map(user -> "{user_id=" + user.getUserId() + ", display_name=" + user.getDisplayName() + "}")
                .collect(Collectors.joining(", ", "[", "]"));
    }

    private String summarizeLink(UserKakaoLink link) {
        if (link == null) {
            return "none";
        }
        return "{user_id=" + link.getUserId()
                + ", kakao_user_id=" + link.getKakaoUserId()
                + ", link_status=" + link.getLinkStatus()
                + "}";
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
        private final FailureCode failureCode;

        private KakaoLoginResult(boolean success,
                                 UserAccount userAccount,
                                 String message,
                                 FailureCode failureCode) {
            this.success = success;
            this.userAccount = userAccount;
            this.message = message;
            this.failureCode = failureCode;
        }

        public static KakaoLoginResult success(UserAccount userAccount) {
            return new KakaoLoginResult(true, userAccount, null, null);
        }

        public static KakaoLoginResult fail(FailureCode failureCode, String message) {
            return new KakaoLoginResult(false, null, message, failureCode);
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

        public FailureCode getFailureCode() {
            return failureCode;
        }
    }

    public enum FailureCode {
        KAKAO_USER_NOT_MATCHED,
        KAKAO_USER_DUPLICATED,
        KAKAO_ALREADY_LINKED_OTHER,
        KAKAO_LINK_DB_ERROR
    }
}
