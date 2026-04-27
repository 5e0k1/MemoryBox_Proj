package com.hogudeul.memorybox.dto;

public class ShareLinkCreateRequest {

    private Boolean guest;
    private Boolean allowComments;
    private Boolean allowDownload;
    private Integer expiresMinutes;

    public Boolean getGuest() {
        return guest;
    }

    public void setGuest(Boolean guest) {
        this.guest = guest;
    }

    public Boolean getAllowComments() {
        return allowComments;
    }

    public void setAllowComments(Boolean allowComments) {
        this.allowComments = allowComments;
    }

    public Boolean getAllowDownload() {
        return allowDownload;
    }

    public void setAllowDownload(Boolean allowDownload) {
        this.allowDownload = allowDownload;
    }

    public Integer getExpiresMinutes() {
        return expiresMinutes;
    }

    public void setExpiresMinutes(Integer expiresMinutes) {
        this.expiresMinutes = expiresMinutes;
    }
}
