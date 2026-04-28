package com.hogudeul.memorybox.model;

import java.time.LocalDateTime;

public class SearchMediaItemRow {
    private Long batchId;
    private Long mediaId;
    private String mediaType;
    private String smallStorageKey;
    private String mediumStorageKey;
    private String previewStorageKey;
    private String originalFileName;
    private LocalDateTime uploadedAt;
    private LocalDateTime takenAt;
    private String title;
    private String displayName;
    private Integer batchMediaCount;

    public Long getBatchId() { return batchId; }
    public void setBatchId(Long batchId) { this.batchId = batchId; }
    public Long getMediaId() { return mediaId; }
    public void setMediaId(Long mediaId) { this.mediaId = mediaId; }
    public String getMediaType() { return mediaType; }
    public void setMediaType(String mediaType) { this.mediaType = mediaType; }
    public String getSmallStorageKey() { return smallStorageKey; }
    public void setSmallStorageKey(String smallStorageKey) { this.smallStorageKey = smallStorageKey; }
    public String getMediumStorageKey() { return mediumStorageKey; }
    public void setMediumStorageKey(String mediumStorageKey) { this.mediumStorageKey = mediumStorageKey; }
    public String getPreviewStorageKey() { return previewStorageKey; }
    public void setPreviewStorageKey(String previewStorageKey) { this.previewStorageKey = previewStorageKey; }
    public String getOriginalFileName() { return originalFileName; }
    public void setOriginalFileName(String originalFileName) { this.originalFileName = originalFileName; }
    public LocalDateTime getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(LocalDateTime uploadedAt) { this.uploadedAt = uploadedAt; }
    public LocalDateTime getTakenAt() { return takenAt; }
    public void setTakenAt(LocalDateTime takenAt) { this.takenAt = takenAt; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public Integer getBatchMediaCount() { return batchMediaCount; }
    public void setBatchMediaCount(Integer batchMediaCount) { this.batchMediaCount = batchMediaCount; }
}
