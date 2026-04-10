package com.hogudeul.memorybox.upload;

public class StoredFile {

    private final String storageKey;
    private final String originalName;
    private final String saveName;
    private final String extension;
    private final String mimeType;
    private final long fileSize;

    public StoredFile(String storageKey, String originalName, String saveName, String extension, String mimeType, long fileSize) {
        this.storageKey = storageKey;
        this.originalName = originalName;
        this.saveName = saveName;
        this.extension = extension;
        this.mimeType = mimeType;
        this.fileSize = fileSize;
    }

    public String getStorageKey() {
        return storageKey;
    }

    public String getOriginalName() {
        return originalName;
    }

    public String getSaveName() {
        return saveName;
    }

    public String getExtension() {
        return extension;
    }

    public String getMimeType() {
        return mimeType;
    }

    public long getFileSize() {
        return fileSize;
    }
}
