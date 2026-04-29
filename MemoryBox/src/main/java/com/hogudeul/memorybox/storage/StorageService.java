package com.hogudeul.memorybox.storage;

import com.hogudeul.memorybox.upload.StorageCategory;
import com.hogudeul.memorybox.upload.StoredFile;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Map;
import org.springframework.web.multipart.MultipartFile;

public interface StorageService {

    StoredFile store(MultipartFile multipartFile, StorageCategory category, LocalDate date) throws IOException;

    StoredFile store(MultipartFile multipartFile,
                     StorageCategory category,
                     LocalDate date,
                     Map<String, String> metadata) throws IOException;

    StoredFile store(byte[] bytes,
                     String originalName,
                     String extension,
                     String mimeType,
                     StorageCategory category,
                     LocalDate date) throws IOException;

    StoredFile store(byte[] bytes,
                     String originalName,
                     String extension,
                     String mimeType,
                     StorageCategory category,
                     LocalDate date,
                     Map<String, String> metadata) throws IOException;

    void delete(String storageKey);

    Path downloadToTempFile(String storageKey, String prefix, String suffix) throws IOException;
}
