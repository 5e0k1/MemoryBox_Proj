package com.hogudeul.memorybox.service;

import com.hogudeul.memorybox.mapper.UploadMapper;
import com.hogudeul.memorybox.model.MediaVariant;
import com.hogudeul.memorybox.storage.StorageService;
import com.hogudeul.memorybox.upload.StorageCategory;
import com.hogudeul.memorybox.upload.StoredFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.hogudeul.memorybox.config.AppProperties;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

@Service
public class VideoProcessingService {

    private static final Logger log = LoggerFactory.getLogger(VideoProcessingService.class);

    private final UploadMapper uploadMapper;
    private final StorageService storageService;
    private final String ffmpegCommand;
    private volatile String resolvedFfmpegCommand;

    public VideoProcessingService(UploadMapper uploadMapper,
                                  StorageService storageService,
                                  AppProperties appProperties) {
        this.uploadMapper = uploadMapper;
        this.storageService = storageService;
        String ffmpeg = appProperties.getFfmpeg().getCommand();
        this.ffmpegCommand = ffmpeg == null || ffmpeg.isBlank() ? "ffmpeg" : ffmpeg.trim();
        this.resolvedFfmpegCommand = this.ffmpegCommand;
    }

    @Async("mediaTaskExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void generateVideoVariantsAsync(Long mediaId, String originalStorageKey, String originalName) {
        Path tempInputPath = null;
        Path tempThumb = null;
        Path tempPreview = null;

        String ffmpeg = resolvedFfmpegPath();
        if (!isExecutable(ffmpeg)) {
            log.error("[video-processing] ffmpeg 실행 파일을 찾지 못했습니다. mediaId={}, configured='{}'. app.ffmpeg.command 값을 확인해 주세요.", mediaId, ffmpegCommand);
            return;
        }

        try {
            tempInputPath = storageService.downloadToTempFile(originalStorageKey, "memorybox-original-", ".tmp");
            tempThumb = Files.createTempFile("memorybox-thumb-", ".jpg");
            tempPreview = Files.createTempFile("memorybox-preview-", ".mp4");
            generateThumb(mediaId, tempInputPath, tempThumb, originalName);
        } catch (Exception e) {
            log.error("[video-processing] THUMB 생성 실패(1차). mediaId={}, originalStorageKey={}, tempInputPath={}, ffmpegPath={}, stderr={}",
                    mediaId, originalStorageKey, tempInputPath, ffmpeg, e.getMessage(), e);
            try {
                if (tempInputPath == null) {
                    tempInputPath = storageService.downloadToTempFile(originalStorageKey, "memorybox-original-", ".tmp");
                }
                if (tempThumb == null) {
                    tempThumb = Files.createTempFile("memorybox-thumb-", ".jpg");
                }
                generateThumbFromStart(mediaId, tempInputPath, tempThumb, originalName);
            } catch (Exception secondEx) {
                log.error("[video-processing] THUMB 생성 실패(2차 fallback). mediaId={}, originalStorageKey={}, tempInputPath={}, ffmpegPath={}, stderr={}",
                        mediaId, originalStorageKey, tempInputPath, ffmpeg, secondEx.getMessage(), secondEx);
            }
        }

        try {
            if (tempInputPath == null) {
                tempInputPath = storageService.downloadToTempFile(originalStorageKey, "memorybox-original-", ".tmp");
            }
            if (tempPreview == null) {
                tempPreview = Files.createTempFile("memorybox-preview-", ".mp4");
            }
            generatePreview(mediaId, tempInputPath, tempPreview, originalName);
        } catch (Exception e) {
            log.error("[video-processing] PREVIEW 생성 실패. mediaId={}, originalStorageKey={}, tempInputPath={}, ffmpegPath={}, stderr={}",
                    mediaId, originalStorageKey, tempInputPath, ffmpeg, e.getMessage(), e);
        } finally {
            deleteTempFile(tempInputPath);
            deleteTempFile(tempThumb);
            deleteTempFile(tempPreview);
        }
    }

    protected void generateThumb(Long mediaId, Path originalPath, Path tempThumb, String originalName) throws IOException {
        runFfmpeg(buildThumbCommand(originalPath, tempThumb, "00:00:01"), mediaId, "THUMB");

        byte[] bytes = Files.readAllBytes(tempThumb);
        StoredFile thumb = storageService.store(bytes, originalName, "jpg", "image/jpeg", StorageCategory.THUMB, LocalDate.now());
        BufferedImage image = ImageIO.read(tempThumb.toFile());
        Integer width = image != null ? image.getWidth() : null;
        Integer height = image != null ? image.getHeight() : null;
        saveVariant(mediaId, "THUMB", thumb, width, height, null);
        log.info("[video-processing] THUMB 생성 완료. mediaId={}, key={}", mediaId, thumb.getStorageKey());
    }

    protected void generateThumbFromStart(Long mediaId, Path originalPath, Path tempThumb, String originalName) throws IOException {
        runFfmpeg(buildThumbCommand(originalPath, tempThumb, "00:00:00"), mediaId, "THUMB-FALLBACK");
        byte[] bytes = Files.readAllBytes(tempThumb);
        StoredFile thumb = storageService.store(bytes, originalName, "jpg", "image/jpeg", StorageCategory.THUMB, LocalDate.now());
        BufferedImage image = ImageIO.read(tempThumb.toFile());
        Integer width = image != null ? image.getWidth() : null;
        Integer height = image != null ? image.getHeight() : null;
        saveVariant(mediaId, "THUMB", thumb, width, height, null);
        log.info("[video-processing] THUMB fallback 생성 완료. mediaId={}, key={}", mediaId, thumb.getStorageKey());
    }

    protected void generatePreview(Long mediaId, Path originalPath, Path tempPreview, String originalName) throws IOException {
        runFfmpeg(List.of(
                    resolvedFfmpegPath(),
                    "-y",
                    "-ss", "00:00:01",
                    "-t", "4",
                    "-i", originalPath.toString(),
                    "-an",
                    "-vf", "setpts=0.5*PTS,scale=min(480\\,iw):-2",
                    "-r", "24",
                    "-c:v", "libx264",
                    "-preset", "veryfast",
                    "-crf", "30",
                    "-movflags", "+faststart",
                    tempPreview.toString()
            ), mediaId, "PREVIEW");

        byte[] bytes = Files.readAllBytes(tempPreview);
        StoredFile preview = storageService.store(bytes, originalName, "mp4", "video/mp4", StorageCategory.PREVIEW, LocalDate.now());
        saveVariant(mediaId, "PREVIEW", preview, 480, null, 2);
        log.info("[video-processing] PREVIEW 생성 완료. mediaId={}, key={}", mediaId, preview.getStorageKey());
    }

    private void runFfmpeg(List<String> command, Long mediaId, String variantType) throws IOException {
        log.info("[video-processing] ffmpeg 실행. mediaId={}, variant={}, command={}", mediaId, variantType, String.join(" ", command));
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        Process process = pb.start();
        String output;
        try {
            output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            boolean finished = process.waitFor(180, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new IOException("ffmpeg 실행 시간 초과");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("ffmpeg 실행 대기 중 인터럽트 발생", e);
        }

        if (process.exitValue() != 0) {
            throw new IOException("ffmpeg 실패(mediaId=" + mediaId + ", variant=" + variantType + "): " + output);
        }
    }

    private List<String> buildThumbCommand(Path originalPath, Path tempThumb, String seekTime) {
        return List.of(
                resolvedFfmpegPath(),
                "-y",
                "-ss", seekTime,
                "-i", originalPath.toString(),
                "-frames:v", "1",
                "-update", "1",
                "-q:v", "4",
                tempThumb.toString()
        );
    }

    private void saveVariant(Long mediaId,
                             String variantType,
                             StoredFile file,
                             Integer width,
                             Integer height,
                             Integer durationSec) {
        uploadMapper.deleteMediaVariantByType(mediaId, variantType);

        MediaVariant variant = new MediaVariant();
        variant.setVariantId(uploadMapper.selectNextMediaVariantId());
        variant.setMediaId(mediaId);
        variant.setVariantType(variantType);
        variant.setStorageKey(file.getStorageKey());
        variant.setFileOrgName(file.getOriginalName());
        variant.setFileSaveName(file.getSaveName());
        variant.setExtension(file.getExtension());
        variant.setMimeType(file.getMimeType());
        variant.setFileSize(file.getFileSize());
        variant.setWidth(width);
        variant.setHeight(height);
        variant.setDurationSec(durationSec);
        variant.setCreatedAt(LocalDateTime.now());
        uploadMapper.insertMediaVariant(variant);
    }

    private String resolvedFfmpegPath() {
        String cached = resolvedFfmpegCommand;
        if (isExecutable(cached)) {
            return cached;
        }

        synchronized (this) {
            if (isExecutable(resolvedFfmpegCommand)) {
                return resolvedFfmpegCommand;
            }

            String discovered = discoverFfmpegCommand();
            if (discovered != null) {
                resolvedFfmpegCommand = discovered;
                log.info("[video-processing] ffmpeg 실행 경로 자동 탐지 성공: {}", discovered);
                return discovered;
            }
        }
        return ffmpegCommand;
    }

    private String discoverFfmpegCommand() {
        List<String> candidates = new ArrayList<>();
        candidates.add(ffmpegCommand);
        candidates.add("ffmpeg");
        candidates.add("ffmpeg.exe");

        if (isWindows()) {
            String localAppData = System.getenv("LOCALAPPDATA");
            String programFiles = System.getenv("ProgramFiles");
            String programFilesX86 = System.getenv("ProgramFiles(x86)");
            if (localAppData != null) {
                candidates.add(localAppData + "\\Microsoft\\WinGet\\Links\\ffmpeg.exe");
            }
            if (programFiles != null) {
                candidates.add(programFiles + "\\ffmpeg\\bin\\ffmpeg.exe");
            }
            if (programFilesX86 != null) {
                candidates.add(programFilesX86 + "\\ffmpeg\\bin\\ffmpeg.exe");
            }
        }

        for (String candidate : candidates) {
            if (candidate != null && isExecutable(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private boolean isExecutable(String command) {
        if (command == null || command.isBlank()) {
            return false;
        }
        try {
            Process process = new ProcessBuilder(command, "-version")
                    .redirectErrorStream(true)
                    .start();
            boolean finished = process.waitFor(2, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return false;
            }
            return process.exitValue() == 0;
        } catch (Exception ignore) {
            return false;
        }
    }

    private boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
    }

    private void deleteTempFile(Path path) {
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
        }
    }
}
