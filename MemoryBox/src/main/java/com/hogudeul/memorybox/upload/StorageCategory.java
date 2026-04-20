package com.hogudeul.memorybox.upload;

public enum StorageCategory {
    ORIGINAL("original"),
    SMALL("small"),
    MEDIUM("medium"),
    VIDEO("video"),
    THUMB("thumb");

    private final String dir;

    StorageCategory(String dir) {
        this.dir = dir;
    }

    public String getDir() {
        return dir;
    }
}
