package com.hogudeul.memorybox.storage;

import com.hogudeul.memorybox.config.StorageProperties;
import org.springframework.stereotype.Component;

@Component
public class StorageUrlResolver {

    private final StorageProperties storageProperties;

    public StorageUrlResolver(StorageProperties storageProperties) {
        this.storageProperties = storageProperties;
    }

    public String resolvePublicUrl(String storageKey) {
        if (isBlank(storageKey)) {
            return "";
        }

        String normalizedKey = storageKey.replace('\\', '/');
        if (isS3Type()) {
            return join(normalizeBaseUrl(storageProperties.getCloudfront().getDomain()), normalizedKey);
        }

        String baseUrl = storageProperties.getPublicBaseUrl();
        if (isBlank(baseUrl)) {
            baseUrl = "/files";
        }
        return join(baseUrl, normalizedKey);
    }

    public String resolveDownloadUrl(Long mediaId, String originalStorageKey) {
        if (isS3Type() && !isBlank(originalStorageKey)) {
            return resolvePublicUrl(originalStorageKey);
        }
        return "/feed/" + mediaId + "/download";
    }

    private boolean isS3Type() {
        return "s3".equalsIgnoreCase(storageProperties.getType());
    }

    private String normalizeBaseUrl(String value) {
        if (isBlank(value)) {
            return "";
        }
        String trimmed = value.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }

    private String join(String baseUrl, String key) {
        if (isBlank(baseUrl)) {
            return "/" + key;
        }
        return baseUrl + "/" + key;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
