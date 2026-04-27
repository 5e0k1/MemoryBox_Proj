package com.hogudeul.memorybox.dto;

public class FeedMediaItemView {
    private final Long mediaId;
    private final String mediaType;
    private final String smallUrl;
    private final String mediumUrl;
    private final String previewUrl;
    private final String originalFileName;
    private final int sortOrder;

    public FeedMediaItemView(Long mediaId, String mediaType, String smallUrl, String mediumUrl,
                             String previewUrl, String originalFileName, int sortOrder) {
        this.mediaId = mediaId;
        this.mediaType = mediaType;
        this.smallUrl = smallUrl;
        this.mediumUrl = mediumUrl;
        this.previewUrl = previewUrl;
        this.originalFileName = originalFileName;
        this.sortOrder = sortOrder;
    }

    public Long getMediaId() { return mediaId; }
    public String getMediaType() { return mediaType; }
    public String getSmallUrl() { return smallUrl; }
    public String getMediumUrl() { return mediumUrl; }
    public String getPreviewUrl() { return previewUrl; }
    public String getOriginalFileName() { return originalFileName; }
    public int getSortOrder() { return sortOrder; }
}
