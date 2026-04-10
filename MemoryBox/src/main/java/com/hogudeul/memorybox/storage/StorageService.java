package com.hogudeul.memorybox.storage;

import com.hogudeul.memorybox.upload.StorageCategory;
import com.hogudeul.memorybox.upload.StoredFile;
import java.io.IOException;
import java.time.LocalDate;
import org.springframework.web.multipart.MultipartFile;

public interface StorageService {

    StoredFile store(MultipartFile multipartFile, StorageCategory category, LocalDate date) throws IOException;

    StoredFile store(byte[] bytes,
                     String originalName,
                     String extension,
                     String mimeType,
                     StorageCategory category,
                     LocalDate date) throws IOException;

    void delete(String storageKey);
}
