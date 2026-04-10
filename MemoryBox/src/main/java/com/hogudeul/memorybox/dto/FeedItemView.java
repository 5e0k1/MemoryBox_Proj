package com.hogudeul.memorybox.dto;

/**
 * 메인 피드 정적 목업 렌더링용 DTO.
 * 추후 Entity/Service 계층 연결 전까지 View Model 역할만 수행한다.
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

    public FeedItemView(Long id, String mediaType, String thumbnailUrl, String title, String author,
                        int shotYear, String uploadedAt, int likeCount, int commentCount, String[] tags) {
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
}
