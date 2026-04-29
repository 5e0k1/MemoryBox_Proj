package com.hogudeul.memorybox.dto;

public class DetailMediaItemView {
    private final Long mediaId;
    private final String mediaType;
    private final String smallUrl;
    private final String mediumUrl;
    private final String previewUrl;
    private final String downloadUrl;
    private final int sortOrder;

    public DetailMediaItemView(Long mediaId, String mediaType, String smallUrl, String mediumUrl,
                               String previewUrl, String downloadUrl, int sortOrder) {
        this.mediaId = mediaId;
        this.mediaType = mediaType;
        this.smallUrl = smallUrl;
        this.mediumUrl = mediumUrl;
        this.previewUrl = previewUrl;
        this.downloadUrl = downloadUrl;
        this.sortOrder = sortOrder;
    }

    public Long getMediaId() { return mediaId; }
    public String getMediaType() { return mediaType; }
    public String getSmallUrl() { return smallUrl; }
    public String getMediumUrl() { return mediumUrl; }
    public String getPreviewUrl() { return previewUrl; }
    public String getDownloadUrl() { return downloadUrl; }
    public int getSortOrder() { return sortOrder; }
}
