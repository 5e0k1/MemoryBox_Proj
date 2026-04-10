package com.hogudeul.memorybox.upload;

import java.util.ArrayList;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public class MultiPhotoUploadForm {

    private List<MultipartFile> imageFiles = new ArrayList<>();
    private String takenAt;
    private Long albumId;
    private List<String> fileTags = new ArrayList<>();

    public List<MultipartFile> getImageFiles() {
        return imageFiles;
    }

    public void setImageFiles(List<MultipartFile> imageFiles) {
        this.imageFiles = imageFiles;
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

    public List<String> getFileTags() {
        return fileTags;
    }

    public void setFileTags(List<String> fileTags) {
        this.fileTags = fileTags;
    }
}
