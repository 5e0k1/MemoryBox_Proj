package com.hogudeul.memorybox.dto;

/**
 * 메인 피드 렌더링용 DTO.
 */
public class FeedItemView {

    private Long id;
    private String mediaType;
    private String thumbnailUrl;
    private String title;
    private String author;
    private int shotYear;
    private String uploadedAt;
    private int likeCount;
    private int commentCount;
    private String[] tags;
    private String albumName;
    private String takenAt;
    private String displayAt;

    public FeedItemView(Long id, String mediaType, String thumbnailUrl, String title, String author,
                        int shotYear, String uploadedAt, int likeCount, int commentCount, String[] tags,
                        String albumName, String takenAt, String displayAt) {
        this.id = id;
        this.mediaType = mediaType;
        this.thumbnailUrl = thumbnailUrl;
        this.title = title;
        this.author = author;
        this.shotYear = shotYear;
        this.uploadedAt = uploadedAt;
        this.likeCount = likeCount;
        this.commentCount = commentCount;
        this.tags = tags;
        this.albumName = albumName;
        this.takenAt = takenAt;
        this.displayAt = displayAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getMediaType() {
        return mediaType;
    }

    public void setMediaType(String mediaType) {
        this.mediaType = mediaType;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public int getShotYear() {
        return shotYear;
    }

    public void setShotYear(int shotYear) {
        this.shotYear = shotYear;
    }

    public String getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(String uploadedAt) {
        this.uploadedAt = uploadedAt;
    }

    public int getLikeCount() {
        return likeCount;
    }

    public void setLikeCount(int likeCount) {
        this.likeCount = likeCount;
    }

    public int getCommentCount() {
        return commentCount;
    }

    public void setCommentCount(int commentCount) {
        this.commentCount = commentCount;
    }

    public String[] getTags() {
        return tags;
    }

    public void setTags(String[] tags) {
        this.tags = tags;
    }

    public String getAlbumName() {
        return albumName;
    }

    public void setAlbumName(String albumName) {
        this.albumName = albumName;
    }

    public String getTakenAt() {
        return takenAt;
    }

    public void setTakenAt(String takenAt) {
        this.takenAt = takenAt;
    }

    public String getDisplayAt() {
        return displayAt;
    }

    public void setDisplayAt(String displayAt) {
        this.displayAt = displayAt;
    }
}
