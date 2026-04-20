package com.hogudeul.memorybox.dto;

public class MediaDetailView {

    private final Long mediaId;
    private final String title;
    private final String mediaType;
    private final String uploadedAt;
    private final String takenAt;
    private final String albumName;
    private final String authorName;
    private final String displayImageUrl;
    private final String displayVariantType;
    private final String downloadUrl;
    private final String[] tags;
    private final int likeCount;
    private final int commentCount;
    private final boolean likedByMe;
    private final boolean downloadable;

    public MediaDetailView(Long mediaId,
                           String title,
                           String mediaType,
                           String uploadedAt,
                           String takenAt,
                           String albumName,
                           String authorName,
                           String displayImageUrl,
                           String displayVariantType,
                           String downloadUrl,
                           String[] tags,
                           int likeCount,
                           int commentCount,
                           boolean likedByMe,
                           boolean downloadable) {
        this.mediaId = mediaId;
        this.title = title;
        this.mediaType = mediaType;
        this.uploadedAt = uploadedAt;
        this.takenAt = takenAt;
        this.albumName = albumName;
        this.authorName = authorName;
        this.displayImageUrl = displayImageUrl;
        this.displayVariantType = displayVariantType;
        this.downloadUrl = downloadUrl;
        this.tags = tags;
        this.likeCount = likeCount;
        this.commentCount = commentCount;
        this.likedByMe = likedByMe;
        this.downloadable = downloadable;
    }

    public Long getMediaId() { return mediaId; }
    public String getTitle() { return title; }
    public String getMediaType() { return mediaType; }
    public String getUploadedAt() { return uploadedAt; }
    public String getTakenAt() { return takenAt; }
    public String getAlbumName() { return albumName; }
    public String getAuthorName() { return authorName; }
    public String getDisplayImageUrl() { return displayImageUrl; }
    public String getDisplayVariantType() { return displayVariantType; }
    public String getDownloadUrl() { return downloadUrl; }
    public String[] getTags() { return tags; }
    public int getLikeCount() { return likeCount; }
    public int getCommentCount() { return commentCount; }
    public boolean isLikedByMe() { return likedByMe; }
    public boolean isDownloadable() { return downloadable; }
}
