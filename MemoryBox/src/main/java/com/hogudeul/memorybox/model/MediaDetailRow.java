package com.hogudeul.memorybox.model;

import java.time.LocalDateTime;

public class MediaDetailRow {

    private Long mediaId;
    private String title;
    private String mediaType;
    private LocalDateTime uploadedAt;
    private LocalDateTime takenAt;
    private String albumName;
    private String displayName;
    private String mediumStorageKey;
    private String smallStorageKey;
    private String originalStorageKey;
    private String originalFileName;
    private String originalMimeType;
    private Integer likeCount;
    private Integer commentCount;
    private Integer likedByMe;
    private String tagsCsv;

    public Long getMediaId() {
        return mediaId;
    }

    public void setMediaId(Long mediaId) {
        this.mediaId = mediaId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMediaType() {
        return mediaType;
    }

    public void setMediaType(String mediaType) {
        this.mediaType = mediaType;
    }

    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(LocalDateTime uploadedAt) {
        this.uploadedAt = uploadedAt;
    }

    public LocalDateTime getTakenAt() {
        return takenAt;
    }

    public void setTakenAt(LocalDateTime takenAt) {
        this.takenAt = takenAt;
    }

    public String getAlbumName() {
        return albumName;
    }

    public void setAlbumName(String albumName) {
        this.albumName = albumName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getMediumStorageKey() {
        return mediumStorageKey;
    }

    public void setMediumStorageKey(String mediumStorageKey) {
        this.mediumStorageKey = mediumStorageKey;
    }

    public String getSmallStorageKey() {
        return smallStorageKey;
    }

    public void setSmallStorageKey(String smallStorageKey) {
        this.smallStorageKey = smallStorageKey;
    }

    public String getOriginalStorageKey() {
        return originalStorageKey;
    }

    public void setOriginalStorageKey(String originalStorageKey) {
        this.originalStorageKey = originalStorageKey;
    }

    public String getOriginalFileName() {
        return originalFileName;
    }

    public void setOriginalFileName(String originalFileName) {
        this.originalFileName = originalFileName;
    }

    public String getOriginalMimeType() {
        return originalMimeType;
    }

    public void setOriginalMimeType(String originalMimeType) {
        this.originalMimeType = originalMimeType;
    }

    public Integer getLikeCount() {
        return likeCount;
    }

    public void setLikeCount(Integer likeCount) {
        this.likeCount = likeCount;
    }

    public Integer getCommentCount() {
        return commentCount;
    }

    public void setCommentCount(Integer commentCount) {
        this.commentCount = commentCount;
    }

    public Integer getLikedByMe() {
        return likedByMe;
    }

    public void setLikedByMe(Integer likedByMe) {
        this.likedByMe = likedByMe;
    }

    public String getTagsCsv() {
        return tagsCsv;
    }

    public void setTagsCsv(String tagsCsv) {
        this.tagsCsv = tagsCsv;
    }
}
