package com.hogudeul.memorybox.dto;

import java.util.List;

public class SearchMediaItemView {
    private final Long id;
    private final Long batchId;
    private final String mediaType;
    private final String smallUrl;
    private final String mediumUrl;
    private final String previewUrl;
    private final String downloadUrl;
    private final String title;
    private final String author;
    private final String takenAt;
    private final String relativeUploadedAt;
    private final int batchMediaCount;
    private final List<FeedMediaItemView> batchMediaItems;

    public SearchMediaItemView(Long id, Long batchId, String mediaType, String smallUrl,
                               String mediumUrl, String previewUrl, String downloadUrl,
                               String title, String author, String takenAt, String relativeUploadedAt,
                               int batchMediaCount, List<FeedMediaItemView> batchMediaItems) {
        this.id = id;
        this.batchId = batchId;
        this.mediaType = mediaType;
        this.smallUrl = smallUrl;
        this.mediumUrl = mediumUrl;
        this.previewUrl = previewUrl;
        this.downloadUrl = downloadUrl;
        this.title = title;
        this.author = author;
        this.takenAt = takenAt;
        this.relativeUploadedAt = relativeUploadedAt;
        this.batchMediaCount = batchMediaCount;
        this.batchMediaItems = batchMediaItems;
    }

    public Long getId() { return id; }
    public Long getBatchId() { return batchId; }
    public String getMediaType() { return mediaType; }
    public String getSmallUrl() { return smallUrl; }
    public String getMediumUrl() { return mediumUrl; }
    public String getPreviewUrl() { return previewUrl; }
    public String getDownloadUrl() { return downloadUrl; }
    public String getTitle() { return title; }
    public String getAuthor() { return author; }
    public String getTakenAt() { return takenAt; }
    public String getRelativeUploadedAt() { return relativeUploadedAt; }
    public int getBatchMediaCount() { return batchMediaCount; }
    public List<FeedMediaItemView> getBatchMediaItems() { return batchMediaItems; }
}
