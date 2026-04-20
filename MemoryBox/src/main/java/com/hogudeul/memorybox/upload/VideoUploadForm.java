package com.hogudeul.memorybox.upload;

import java.util.ArrayList;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public class VideoUploadForm {

    private MultipartFile videoFile;
    private String title;
    private String takenAt;
    private Long albumId;
    private List<Long> selectedTagIds = new ArrayList<>();
    private String newTags;

    public MultipartFile getVideoFile() {
        return videoFile;
    }

    public void setVideoFile(MultipartFile videoFile) {
        this.videoFile = videoFile;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTakenAt() {
        return takenAt;
    }

    public void setTakenAt(String takenAt) {
        this.takenAt = takenAt;
    }

    public Long getAlbumId() {
        return albumId;
    }

    public void setAlbumId(Long albumId) {
        this.albumId = albumId;
    }

    public List<Long> getSelectedTagIds() {
        return selectedTagIds;
    }

    public void setSelectedTagIds(List<Long> selectedTagIds) {
        this.selectedTagIds = selectedTagIds;
    }

    public String getNewTags() {
        return newTags;
    }

    public void setNewTags(String newTags) {
        this.newTags = newTags;
    }
}
