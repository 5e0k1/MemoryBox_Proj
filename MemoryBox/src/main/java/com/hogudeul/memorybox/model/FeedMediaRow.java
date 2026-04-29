package com.hogudeul.memorybox.model;

public class FeedMediaRow {
    private Long batchId;
    private Long mediaId;
    private String mediaType;
    private String smallStorageKey;
    private String mediumStorageKey;
    private String previewStorageKey;
    private String originalFileName;
    private Integer sortOrder;

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
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
}
