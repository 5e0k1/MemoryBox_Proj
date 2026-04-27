package com.hogudeul.memorybox.dto;

import java.time.LocalDateTime;

public class ShareLinkCreateResponse {

    private String memberUrl;
    private String guestUrl;
    private LocalDateTime expiresAt;

    public ShareLinkCreateResponse(String memberUrl, String guestUrl, LocalDateTime expiresAt) {
        this.memberUrl = memberUrl;
        this.guestUrl = guestUrl;
        this.expiresAt = expiresAt;
    }

    public String getMemberUrl() {
        return memberUrl;
    }

    public String getGuestUrl() {
        return guestUrl;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }
}
