package com.hogudeul.memorybox.storage;

import com.hogudeul.memorybox.config.StorageProperties;
import com.hogudeul.memorybox.upload.StorageCategory;
import com.hogudeul.memorybox.upload.StoredFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Locale;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.core.ResponseInputStream;

@Service
@ConditionalOnProperty(prefix = "app.storage", name = "type", havingValue = "s3")
public class S3StorageService implements StorageService {

    private final StorageProperties storageProperties;
    private final S3Client s3Client;

    public S3StorageService(StorageProperties storageProperties) {
        this.storageProperties = storageProperties;
        this.s3Client = S3Client.builder()
                .region(Region.of(storageProperties.getS3().getRegion()))
                .build();
    }

    @Override
    public StoredFile store(MultipartFile multipartFile, StorageCategory category, LocalDate date) throws IOException {
        try {
            String originalName = multipartFile.getOriginalFilename();
            String extension = extractExtension(originalName);
            String mimeType = multipartFile.getContentType();
            byte[] bytes = multipartFile.getBytes();
            return store(bytes, originalName, extension, mimeType, category, date);
        } catch (IOException e) {
            throw new IOException("Failed to read multipart file bytes for S3 upload.", e);
        }
    }

    @Override
    public StoredFile store(byte[] bytes,
                            String originalName,
                            String extension,
                            String mimeType,
                            StorageCategory category,
                            LocalDate date) {
        String ext = sanitize(extension);
        String saveName = UUID.randomUUID() + (ext.isBlank() ? "" : "." + ext);
        String storageKey = buildStorageKey(category, date, saveName);

        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(storageProperties.getS3().getBucket())
                .key(storageKey)
                .contentType(mimeType)
                .build();

        s3Client.putObject(request, RequestBody.fromBytes(bytes));
        return new StoredFile(storageKey, originalName, saveName, ext, mimeType, bytes.length);
    }

    @Override
    public void delete(String storageKey) {
        try {
            DeleteObjectRequest request = DeleteObjectRequest.builder()
                    .bucket(storageProperties.getS3().getBucket())
                    .key(storageKey)
                    .build();
            s3Client.deleteObject(request);
        } catch (Exception ignore) {
            // 보상 트랜잭션 단계에서는 삭제 실패를 무시하고 DB rollback을 우선한다.
        }
    }

    @Override
    public Path downloadToTempFile(String storageKey, String prefix, String suffix) throws IOException {
        String safePrefix = (prefix == null || prefix.isBlank()) ? "memorybox-" : prefix;
        String safeSuffix = (suffix == null || suffix.isBlank()) ? ".tmp" : suffix;
        Path tempFile = Files.createTempFile(safePrefix, safeSuffix);
        try {
            GetObjectRequest request = GetObjectRequest.builder()
                    .bucket(storageProperties.getS3().getBucket())
                    .key(storageKey)
                    .build();
            try (ResponseInputStream<GetObjectResponse> inputStream = s3Client.getObject(request)) {
                Files.copy(inputStream, tempFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
            return tempFile;
        } catch (Exception e) {
            Files.deleteIfExists(tempFile);
            throw new IOException("S3 object download to temp file failed. key=" + storageKey + ", path=" + tempFile, e);
        }
    }

    private String buildStorageKey(StorageCategory category, LocalDate date, String saveName) {
        String categoryKey = String.format(Locale.ROOT, "%s/%04d/%02d/%02d/%s",
                category.getDir(),
                date.getYear(),
                date.getMonthValue(),
                date.getDayOfMonth(),
                saveName);

        String prefix = storageProperties.getS3().getPrefix();
        if (prefix == null || prefix.isBlank()) {
            return categoryKey;
        }

        String normalizedPrefix = prefix.endsWith("/") ? prefix.substring(0, prefix.length() - 1) : prefix;
        return normalizedPrefix + "/" + categoryKey;
    }

    private String sanitize(String extension) {
        if (extension == null) {
            return "";
        }
        return extension.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    private String extractExtension(String originalName) {
        if (originalName == null || !originalName.contains(".")) {
            return "";
        }
        int idx = originalName.lastIndexOf('.');
        return originalName.substring(idx + 1);
    }
}
