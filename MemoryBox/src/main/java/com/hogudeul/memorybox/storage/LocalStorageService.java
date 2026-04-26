package com.hogudeul.memorybox.storage;

import com.hogudeul.memorybox.upload.StorageCategory;
import com.hogudeul.memorybox.upload.StoredFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.Locale;
import java.util.UUID;
import com.hogudeul.memorybox.config.StorageProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@ConditionalOnProperty(prefix = "app.storage", name = "type", havingValue = "local", matchIfMissing = true)
public class LocalStorageService implements StorageService {

    private final Path root;

    public LocalStorageService(StorageProperties storageProperties) {
        this.root = Paths.get(storageProperties.getLocalRoot()).toAbsolutePath().normalize();
    }

    @Override
    public StoredFile store(MultipartFile multipartFile, StorageCategory category, LocalDate date) throws IOException {
        String originalName = multipartFile.getOriginalFilename();
        String extension = extractExtension(originalName);
        String mimeType = multipartFile.getContentType();
        byte[] bytes = multipartFile.getBytes();
        return store(bytes, originalName, extension, mimeType, category, date);
    }

    @Override
    public StoredFile store(byte[] bytes,
                            String originalName,
                            String extension,
                            String mimeType,
                            StorageCategory category,
                            LocalDate date) throws IOException {
        String ext = sanitizeExtension(extension);
        String saveName = UUID.randomUUID() + (ext.isBlank() ? "" : "." + ext);

        Path relative = Paths.get(
                category.getDir(),
                String.format(Locale.ROOT, "%04d", date.getYear()),
                String.format(Locale.ROOT, "%02d", date.getMonthValue()),
                String.format(Locale.ROOT, "%02d", date.getDayOfMonth()),
                saveName
        );

        Path absolute = root.resolve(relative);
        Files.createDirectories(absolute.getParent());
        Files.write(absolute, bytes);
        String storageKey = relative.toString().replace('\\', '/');

        return new StoredFile(storageKey, originalName, saveName, ext, mimeType, bytes.length);
    }

    @Override
    public void delete(String storageKey) {
        try {
            Path target = root.resolve(storageKey).normalize();
            if (Files.exists(target)) {
                Files.delete(target);
            }
        } catch (Exception ignore) {
            // 보상 트랜잭션 단계에서는 삭제 실패를 무시하고 DB rollback을 우선한다.
        }
    }

    private String sanitizeExtension(String extension) {
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
