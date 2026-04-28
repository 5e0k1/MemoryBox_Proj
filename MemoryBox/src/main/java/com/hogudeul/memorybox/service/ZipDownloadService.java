package com.hogudeul.memorybox.service;

import com.hogudeul.memorybox.config.StorageProperties;
import jakarta.annotation.PostConstruct;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class ZipDownloadService {

    private static final Logger log = LoggerFactory.getLogger(ZipDownloadService.class);
    private static final DateTimeFormatter FILE_NAME_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final Pattern SAFE_ZIP_NAME = Pattern.compile("^[A-Za-z0-9._-]+\\.zip$");
    private static final long EXPIRE_MILLIS = 60L * 60L * 1000L;

    private final DetailService detailService;
    private final Path zipRoot;

    public ZipDownloadService(DetailService detailService, StorageProperties storageProperties) {
        this.detailService = detailService;
        this.zipRoot = Paths.get(storageProperties.getTempRoot()).resolve("zip").normalize();
    }

    @PostConstruct
    public void onStartup() {
        ensureZipRoot();
        cleanupExpiredFiles();
    }

    public PreparedZip prepareZip(Long userId, Long batchId, List<Long> mediaIds) {
        List<DetailService.DownloadFileInfo> files = resolveDownloadFiles(userId, batchId, mediaIds);
        ensureZipRoot();

        String fileName = buildZipFileName(userId);
        Path zipPath = zipRoot.resolve(fileName).normalize();
        if (!zipPath.startsWith(zipRoot)) {
            throw new DetailService.DownloadException("잘못된 ZIP 경로입니다.");
        }

        writeZipFile(files, zipPath);
        return new PreparedZip(fileName, "/download/zip/" + fileName);
    }

    public Path resolveZipForDownload(String fileName) {
        validateFileName(fileName);
        Path zipPath = zipRoot.resolve(fileName).normalize();
        if (!zipPath.startsWith(zipRoot)) {
            throw new DetailService.DownloadException("허용되지 않은 경로입니다.");
        }
        if (!Files.exists(zipPath) || !Files.isRegularFile(zipPath) || !Files.isReadable(zipPath)) {
            throw new DetailService.DownloadException("요청한 ZIP 파일을 찾을 수 없습니다.");
        }
        return zipPath;
    }

    @Scheduled(fixedDelay = 1800000)
    public void cleanupExpiredFiles() {
        ensureZipRoot();
        long now = System.currentTimeMillis();
        try (var paths = Files.list(zipRoot)) {
            paths.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().toLowerCase().endsWith(".zip"))
                    .forEach(path -> deleteIfExpired(path, now));
        } catch (IOException e) {
            log.warn("temp zip cleanup failed. path={}, msg={}", zipRoot, e.getMessage());
        }
    }

    private void deleteIfExpired(Path path, long now) {
        try {
            long modified = Files.getLastModifiedTime(path).toMillis();
            if (now - modified >= EXPIRE_MILLIS) {
                Files.deleteIfExists(path);
                log.info("deleted expired zip file. file={}", path.getFileName());
            }
        } catch (IOException e) {
            log.warn("failed to delete expired zip file. file={}, msg={}", path.getFileName(), e.getMessage());
        }
    }

    private void ensureZipRoot() {
        try {
            Files.createDirectories(zipRoot);
        } catch (IOException e) {
            throw new DetailService.DownloadException("임시 ZIP 폴더를 생성할 수 없습니다.", e);
        }
    }

    private List<DetailService.DownloadFileInfo> resolveDownloadFiles(Long userId, Long batchId, List<Long> mediaIds) {
        if (batchId != null && mediaIds != null && !mediaIds.isEmpty()) {
            throw new DetailService.DownloadException("batchId 또는 mediaIds 중 하나만 요청해 주세요.");
        }
        if (batchId != null) {
            return detailService.getBatchDownloadFileInfos(batchId, userId);
        }
        return detailService.getDownloadFileInfos(mediaIds, userId);
    }

    private void writeZipFile(List<DetailService.DownloadFileInfo> files, Path zipPath) {
        Map<String, Integer> nameCounter = new HashMap<>();
        try (FileOutputStream fileOutputStream = new FileOutputStream(zipPath.toFile());
             BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(fileOutputStream);
             ZipOutputStream zipOutputStream = new ZipOutputStream(bufferedOutputStream)) {
            for (DetailService.DownloadFileInfo file : files) {
                if (!file.existsReadable()) {
                    throw new DetailService.DownloadException("원본 파일을 찾을 수 없습니다.");
                }
                String zipEntryName = resolveDuplicatedName(file.getFileName(), nameCounter);
                zipOutputStream.putNextEntry(new ZipEntry(zipEntryName));
                try (InputStream inputStream = new BufferedInputStream(file.openInputStream())) {
                    inputStream.transferTo(zipOutputStream);
                }
                zipOutputStream.closeEntry();
            }
            zipOutputStream.finish();
        } catch (IOException e) {
            try {
                Files.deleteIfExists(zipPath);
            } catch (IOException ignored) {
                // ignore
            }
            throw new DetailService.DownloadException("ZIP 생성 중 오류가 발생했습니다.", e);
        }
    }

    private void validateFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            throw new DetailService.DownloadException("파일명이 비어 있습니다.");
        }
        if (fileName.contains("..") || fileName.contains("/") || fileName.contains("\\") || !SAFE_ZIP_NAME.matcher(fileName).matches()) {
            throw new DetailService.DownloadException("잘못된 파일명입니다.");
        }
    }

    private String resolveDuplicatedName(String originalName, Map<String, Integer> nameCounter) {
        String normalized = (originalName == null || originalName.isBlank()) ? "download" : originalName;
        int count = nameCounter.getOrDefault(normalized, 0);
        nameCounter.put(normalized, count + 1);
        if (count == 0) {
            return normalized;
        }

        int dotIndex = normalized.lastIndexOf('.');
        if (dotIndex <= 0 || dotIndex == normalized.length() - 1) {
            return normalized + " (" + count + ")";
        }
        String base = normalized.substring(0, dotIndex);
        String extension = normalized.substring(dotIndex);
        return base + " (" + count + ")" + extension;
    }

    private String buildZipFileName(Long userId) {
        String timestamp = LocalDateTime.now(ZoneId.systemDefault()).format(FILE_NAME_TIME_FORMAT);
        String random = UUID.randomUUID().toString().replace("-", "");
        return userId + "_" + random + "_" + timestamp + ".zip";
    }

    public static class PreparedZip {
        private final String fileName;
        private final String downloadUrl;

        public PreparedZip(String fileName, String downloadUrl) {
            this.fileName = fileName;
            this.downloadUrl = downloadUrl;
        }

        public String getFileName() {
            return fileName;
        }

        public String getDownloadUrl() {
            return downloadUrl;
        }
    }
}
