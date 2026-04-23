package com.hogudeul.memorybox.service;

import com.hogudeul.memorybox.mapper.UploadMapper;
import com.hogudeul.memorybox.model.MediaVariant;
import com.hogudeul.memorybox.storage.StorageService;
import com.hogudeul.memorybox.upload.StorageCategory;
import com.hogudeul.memorybox.upload.StoredFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
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
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class VideoProcessingService {

    private static final Logger log = LoggerFactory.getLogger(VideoProcessingService.class);

    private final UploadMapper uploadMapper;
    private final StorageService storageService;
    private final Path storageRoot;
    private final String ffmpegCommand;

    public VideoProcessingService(UploadMapper uploadMapper,
                                  StorageService storageService,
                                  @Value("${app.storage.local-root:D:/memorybox/upload/}") String storageRoot,
                                  @Value("${app.ffmpeg.command:ffmpeg}") String ffmpegCommand) {
        this.uploadMapper = uploadMapper;
        this.storageService = storageService;
        this.storageRoot = Paths.get(storageRoot).toAbsolutePath().normalize();
        this.ffmpegCommand = ffmpegCommand == null || ffmpegCommand.isBlank() ? "ffmpeg" : ffmpegCommand.trim();
    }

    @Async("mediaTaskExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void generateVideoVariantsAsync(Long mediaId, String originalStorageKey, String originalName) {
        Path originalPath = storageRoot.resolve(originalStorageKey).normalize();
        if (!Files.exists(originalPath)) {
            log.error("[video-processing] 원본 파일을 찾을 수 없습니다. mediaId={}, key={}", mediaId, originalStorageKey);
            return;
        }

        try {
            generateThumb(mediaId, originalPath, originalName);
        } catch (Exception e) {
            log.error("[video-processing] THUMB 생성 실패(1차). mediaId={}", mediaId, e);
            try {
                generateThumbFromStart(mediaId, originalPath, originalName);
            } catch (Exception secondEx) {
                log.error("[video-processing] THUMB 생성 실패(2차 fallback). mediaId={}", mediaId, secondEx);
            }
        }

        try {
            generatePreview(mediaId, originalPath, originalName);
        } catch (Exception e) {
            log.error("[video-processing] PREVIEW 생성 실패. mediaId={}", mediaId, e);
        }
    }

    protected void generateThumb(Long mediaId, Path originalPath, String originalName) throws IOException {
        Path tempDir = Files.createTempDirectory("memorybox-thumb-");
        Path tempThumb = tempDir.resolve("thumb.jpg");
        try {
            runFfmpeg(buildThumbCommand(originalPath, tempThumb, "00:00:01"), mediaId, "THUMB");

            byte[] bytes = Files.readAllBytes(tempThumb);
            StoredFile thumb = storageService.store(bytes, originalName, "jpg", "image/jpeg", StorageCategory.THUMB, LocalDate.now());
            BufferedImage image = ImageIO.read(tempThumb.toFile());
            Integer width = image != null ? image.getWidth() : null;
            Integer height = image != null ? image.getHeight() : null;
            saveVariant(mediaId, "THUMB", thumb, width, height, null);
            log.info("[video-processing] THUMB 생성 완료. mediaId={}, key={}", mediaId, thumb.getStorageKey());
        } finally {
            Files.deleteIfExists(tempThumb);
            Files.deleteIfExists(tempDir);
        }
    }

    protected void generateThumbFromStart(Long mediaId, Path originalPath, String originalName) throws IOException {
        Path tempDir = Files.createTempDirectory("memorybox-thumb-fallback-");
        Path tempThumb = tempDir.resolve("thumb.jpg");
        try {
            runFfmpeg(buildThumbCommand(originalPath, tempThumb, "00:00:00"), mediaId, "THUMB-FALLBACK");
            byte[] bytes = Files.readAllBytes(tempThumb);
            StoredFile thumb = storageService.store(bytes, originalName, "jpg", "image/jpeg", StorageCategory.THUMB, LocalDate.now());
            BufferedImage image = ImageIO.read(tempThumb.toFile());
            Integer width = image != null ? image.getWidth() : null;
            Integer height = image != null ? image.getHeight() : null;
            saveVariant(mediaId, "THUMB", thumb, width, height, null);
            log.info("[video-processing] THUMB fallback 생성 완료. mediaId={}, key={}", mediaId, thumb.getStorageKey());
        } finally {
            Files.deleteIfExists(tempThumb);
            Files.deleteIfExists(tempDir);
        }
    }

    protected void generatePreview(Long mediaId, Path originalPath, String originalName) throws IOException {
        Path tempDir = Files.createTempDirectory("memorybox-preview-");
        Path tempPreview = tempDir.resolve("preview.mp4");
        try {
            runFfmpeg(List.of(
                    ffmpegCommand,
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
        } finally {
            Files.deleteIfExists(tempPreview);
            Files.deleteIfExists(tempDir);
        }
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
                ffmpegCommand,
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
}
