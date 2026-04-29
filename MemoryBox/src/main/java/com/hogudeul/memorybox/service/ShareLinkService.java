package com.hogudeul.memorybox.service;

import com.hogudeul.memorybox.config.AppProperties;
import com.hogudeul.memorybox.mapper.ShareLinkMapper;
import com.hogudeul.memorybox.model.ShareLink;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ShareLinkService {

    private static final int DEFAULT_EXPIRES_MINUTES = 60;
    private static final int MAX_EXPIRES_MINUTES = 60 * 24 * 7;
    private static final int TOKEN_BYTE_SIZE = 36;

    private final ShareLinkMapper shareLinkMapper;
    private final SecureRandom secureRandom = new SecureRandom();
    private final String shareBaseUrl;

    public ShareLinkService(ShareLinkMapper shareLinkMapper,
                            AppProperties appProperties) {
        this.shareLinkMapper = shareLinkMapper;
        this.shareBaseUrl = trimTrailingSlash(appProperties.getShare().getBaseUrl());
    }

    public String buildMemberShareUrl(Long batchId) {
        return shareBaseUrl + "/feed/" + batchId;
    }

    public String buildVideoMemberShareUrl(Long mediaId) {
        return shareBaseUrl + "/video/" + mediaId;
    }

    @Transactional
    public ShareLink createGuestShareLink(Long batchId,
                                          Long createdBy,
                                          boolean allowComments,
                                          boolean allowDownload,
                                          Integer expiresMinutes) {
        ShareLink shareLink = new ShareLink();
        shareLink.setBatchId(batchId);
        shareLink.setCreatedBy(createdBy);
        shareLink.setShareScope("GUEST");
        shareLink.setAllowComments(allowComments ? "Y" : "N");
        shareLink.setAllowDownload(allowDownload ? "Y" : "N");
        shareLink.setRevokedYn("N");
        shareLink.setExpiresAt(LocalDateTime.now().plusMinutes(normalizeExpiresMinutes(expiresMinutes)));
        shareLink.setShareToken(generateUniqueToken());

        shareLinkMapper.insertShareLink(shareLink);
        return shareLink;
    }

    public ShareLink findActiveByToken(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        return shareLinkMapper.findActiveByToken(token);
    }

    @Transactional
    public boolean revokeShareLink(Long shareId, Long createdBy) {
        return shareLinkMapper.revokeShareLink(shareId, createdBy) > 0;
    }

    public String buildGuestShareUrl(String token) {
        return shareBaseUrl + "/share/" + token;
    }

    private int normalizeExpiresMinutes(Integer expiresMinutes) {
        if (expiresMinutes == null || expiresMinutes <= 0) {
            return DEFAULT_EXPIRES_MINUTES;
        }
        return Math.min(expiresMinutes, MAX_EXPIRES_MINUTES);
    }

    private String generateUniqueToken() {
        for (int i = 0; i < 10; i++) {
            String token = generateRandomToken();
            if (shareLinkMapper.findByToken(token) == null) {
                return token;
            }
        }
        throw new IllegalStateException("공유 토큰 생성에 실패했습니다.");
    }

    private String generateRandomToken() {
        byte[] bytes = new byte[TOKEN_BYTE_SIZE];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String trimTrailingSlash(String value) {
        if (value == null || value.isBlank()) {
            return "http://localhost:8080";
        }
        String trimmed = value.trim();
        if (trimmed.endsWith("/")) {
            return trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }
}
