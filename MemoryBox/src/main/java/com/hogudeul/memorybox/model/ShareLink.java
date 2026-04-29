package com.hogudeul.memorybox.model;

import java.time.LocalDateTime;

public class ShareLink {

    private Long shareId;
    private Long batchId;
    private String shareToken;
    private Long createdBy;
    private String shareScope;
    private String allowComments;
    private String allowDownload;
    private LocalDateTime expiresAt;
    private String revokedYn;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getShareId() { return shareId; }
    public void setShareId(Long shareId) { this.shareId = shareId; }
    public Long getBatchId() { return batchId; }
    public void setBatchId(Long batchId) { this.batchId = batchId; }
    public String getShareToken() { return shareToken; }
    public void setShareToken(String shareToken) { this.shareToken = shareToken; }
    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
    public String getShareScope() { return shareScope; }
    public void setShareScope(String shareScope) { this.shareScope = shareScope; }
    public String getAllowComments() { return allowComments; }
    public void setAllowComments(String allowComments) { this.allowComments = allowComments; }
    public String getAllowDownload() { return allowDownload; }
    public void setAllowDownload(String allowDownload) { this.allowDownload = allowDownload; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
    public String getRevokedYn() { return revokedYn; }
    public void setRevokedYn(String revokedYn) { this.revokedYn = revokedYn; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
