package com.hogudeul.memorybox.model;

import java.time.LocalDateTime;

public class MediaVariant {

    private Long variantId;
    private Long mediaId;
    private String variantType;
    private String storageKey;
    private String fileOrgName;
    private String fileSaveName;
    private String extension;
    private String mimeType;
    private Long fileSize;
    private Integer width;
    private Integer height;
    private Integer durationSec;
    private LocalDateTime createdAt;

    public Long getVariantId() { return variantId; }
    public void setVariantId(Long variantId) { this.variantId = variantId; }
    public Long getMediaId() { return mediaId; }
    public void setMediaId(Long mediaId) { this.mediaId = mediaId; }
    public String getVariantType() { return variantType; }
    public void setVariantType(String variantType) { this.variantType = variantType; }
    public String getStorageKey() { return storageKey; }
    public void setStorageKey(String storageKey) { this.storageKey = storageKey; }
    public String getFileOrgName() { return fileOrgName; }
    public void setFileOrgName(String fileOrgName) { this.fileOrgName = fileOrgName; }
    public String getFileSaveName() { return fileSaveName; }
    public void setFileSaveName(String fileSaveName) { this.fileSaveName = fileSaveName; }
    public String getExtension() { return extension; }
    public void setExtension(String extension) { this.extension = extension; }
    public String getMimeType() { return mimeType; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }
    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
    public Integer getWidth() { return width; }
    public void setWidth(Integer width) { this.width = width; }
    public Integer getHeight() { return height; }
    public void setHeight(Integer height) { this.height = height; }
    public Integer getDurationSec() { return durationSec; }
    public void setDurationSec(Integer durationSec) { this.durationSec = durationSec; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
