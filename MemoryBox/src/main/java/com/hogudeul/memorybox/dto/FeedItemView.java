package com.hogudeul.memorybox.dto;

import java.util.List;

/**
 * 메인 피드 렌더링용 DTO.
 */
public class FeedItemView {

    private Long id;
    private String mediaType;
    private String thumbnailUrl;
    private String previewUrl;
    private String title;
    private String author;
    private int shotYear;
    private String uploadedAt;
    private int likeCount;
    private int commentCount;
    private boolean likedByMe;
    private String[] tags;
    private String albumName;
    private String takenAt;
    private String displayAt;
    private String relativeUploadedAt;
    private boolean recent;
    private List<FeedMediaItemView> mediaItems;

    public FeedItemView(Long id, String mediaType, String thumbnailUrl, String previewUrl, String title, String author,
                        int shotYear, String uploadedAt, int likeCount, int commentCount, boolean likedByMe, String[] tags,
                        String albumName, String takenAt, String displayAt, String relativeUploadedAt, boolean recent,
                        List<FeedMediaItemView> mediaItems) {
        this.id = id;
        this.mediaType = mediaType;
        this.thumbnailUrl = thumbnailUrl;
        this.previewUrl = previewUrl;
        this.title = title;
        this.author = author;
        this.shotYear = shotYear;
        this.uploadedAt = uploadedAt;
        this.likeCount = likeCount;
        this.commentCount = commentCount;
        this.likedByMe = likedByMe;
        this.tags = tags;
        this.albumName = albumName;
        this.takenAt = takenAt;
        this.displayAt = displayAt;
        this.relativeUploadedAt = relativeUploadedAt;
        this.recent = recent;
        this.mediaItems = mediaItems;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getMediaType() { return mediaType; }
    public void setMediaType(String mediaType) { this.mediaType = mediaType; }
    public String getThumbnailUrl() { return thumbnailUrl; }
    public void setThumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }
    public String getPreviewUrl() { return previewUrl; }
    public void setPreviewUrl(String previewUrl) { this.previewUrl = previewUrl; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }
    public int getShotYear() { return shotYear; }
    public void setShotYear(int shotYear) { this.shotYear = shotYear; }
    public String getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(String uploadedAt) { this.uploadedAt = uploadedAt; }
    public int getLikeCount() { return likeCount; }
    public void setLikeCount(int likeCount) { this.likeCount = likeCount; }
    public int getCommentCount() { return commentCount; }
    public void setCommentCount(int commentCount) { this.commentCount = commentCount; }
    public boolean isLikedByMe() { return likedByMe; }
    public void setLikedByMe(boolean likedByMe) { this.likedByMe = likedByMe; }
    public String[] getTags() { return tags; }
    public void setTags(String[] tags) { this.tags = tags; }
    public String getAlbumName() { return albumName; }
    public void setAlbumName(String albumName) { this.albumName = albumName; }
    public String getTakenAt() { return takenAt; }
    public void setTakenAt(String takenAt) { this.takenAt = takenAt; }
    public String getDisplayAt() { return displayAt; }
    public void setDisplayAt(String displayAt) { this.displayAt = displayAt; }
    public String getRelativeUploadedAt() { return relativeUploadedAt; }
    public void setRelativeUploadedAt(String relativeUploadedAt) { this.relativeUploadedAt = relativeUploadedAt; }
    public boolean isRecent() { return recent; }
    public void setRecent(boolean recent) { this.recent = recent; }
    public List<FeedMediaItemView> getMediaItems() { return mediaItems; }
    public void setMediaItems(List<FeedMediaItemView> mediaItems) { this.mediaItems = mediaItems; }
}
