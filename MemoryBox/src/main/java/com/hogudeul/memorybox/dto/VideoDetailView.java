package com.hogudeul.memorybox.dto;

public class VideoDetailView {
    private final Long mediaId;
    private final Long batchId;
    private final String title;
    private final String author;
    private final String uploadedAt;
    private final String playbackUrl;
    private final String posterUrl;
    private final String downloadUrl;

    public VideoDetailView(Long mediaId, Long batchId, String title, String author, String uploadedAt, String playbackUrl, String posterUrl, String downloadUrl) {
        this.mediaId = mediaId;
        this.batchId = batchId;
        this.title = title;
        this.author = author;
        this.uploadedAt = uploadedAt;
        this.playbackUrl = playbackUrl;
        this.posterUrl = posterUrl;
        this.downloadUrl = downloadUrl;
    }
    public Long getMediaId() { return mediaId; }
    public Long getBatchId() { return batchId; }
    public String getTitle() { return title; }
    public String getAuthor() { return author; }
    public String getUploadedAt() { return uploadedAt; }
    public String getPlaybackUrl() { return playbackUrl; }
    public String getPosterUrl() { return posterUrl; }
    public String getDownloadUrl() { return downloadUrl; }
}
