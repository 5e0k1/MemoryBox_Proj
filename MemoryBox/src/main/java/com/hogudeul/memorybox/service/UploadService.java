package com.hogudeul.memorybox.service;

import com.hogudeul.memorybox.mapper.AlbumMapper;
import com.hogudeul.memorybox.mapper.UploadMapper;
import com.hogudeul.memorybox.model.AlbumOption;
import com.hogudeul.memorybox.model.MediaItem;
import com.hogudeul.memorybox.model.MediaVariant;
import com.hogudeul.memorybox.model.Tag;
import com.hogudeul.memorybox.storage.StorageService;
import com.hogudeul.memorybox.upload.MultiPhotoUploadForm;
import com.hogudeul.memorybox.upload.SinglePhotoUploadForm;
import com.hogudeul.memorybox.upload.StorageCategory;
import com.hogudeul.memorybox.upload.StoredFile;
import com.hogudeul.memorybox.upload.UploadException;
import com.hogudeul.memorybox.upload.VideoUploadForm;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import javax.imageio.ImageIO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class UploadService {

    private static final DateTimeFormatter TAKEN_AT_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

    private final UploadMapper uploadMapper;
    private final AlbumMapper albumMapper;
    private final StorageService storageService;

    public UploadService(UploadMapper uploadMapper, AlbumMapper albumMapper, StorageService storageService) {
        this.uploadMapper = uploadMapper;
        this.albumMapper = albumMapper;
        this.storageService = storageService;
    }

    public List<AlbumOption> getActiveAlbums(Long userId) {
        return albumMapper.findActiveAlbumsByUserId(userId);
    }

    @Transactional
    public void uploadSinglePhoto(Long userId, SinglePhotoUploadForm form) {
        if (form.getImageFile() == null || form.getImageFile().isEmpty()) {
            throw new UploadException("업로드할 이미지 파일을 선택해 주세요.");
        }
        processPhoto(userId, form.getAlbumId(), form.getTitle(), form.getTakenAt(), form.getTags(), form.getImageFile());
    }

    @Transactional
    public void uploadMultiplePhotos(Long userId, MultiPhotoUploadForm form) {
        if (form.getImageFiles() == null || form.getImageFiles().isEmpty()) {
            throw new UploadException("업로드할 이미지 파일을 1개 이상 선택해 주세요.");
        }

        int created = 0;
        for (int i = 0; i < form.getImageFiles().size(); i++) {
            MultipartFile file = form.getImageFiles().get(i);
            if (file == null || file.isEmpty()) {
                continue;
            }
            String perFileTags = i < form.getFileTags().size() ? form.getFileTags().get(i) : null;
            processPhoto(userId, form.getAlbumId(), null, form.getTakenAt(), perFileTags, file);
            created++;
        }
        if (created == 0) {
            throw new UploadException("유효한 이미지 파일이 없습니다.");
        }
    }

    @Transactional
    public void uploadVideo(Long userId, VideoUploadForm form) {
        if (form.getVideoFile() == null || form.getVideoFile().isEmpty()) {
            throw new UploadException("업로드할 동영상 파일을 선택해 주세요.");
        }
        requireAlbum(form.getAlbumId());
        validateMimeType(form.getVideoFile(), "video/");

        List<String> savedKeys = new ArrayList<>();
        try {
            LocalDateTime takenAt = parseTakenAt(form.getTakenAt());
            MediaItem mediaItem = createMediaItem(userId, form.getAlbumId(), "VIDEO", form.getTitle(), takenAt);

            StoredFile original = storageService.store(form.getVideoFile(), StorageCategory.VIDEO, LocalDate.now());
            savedKeys.add(original.getStorageKey());
            uploadMapper.insertMediaVariant(buildVariant(mediaItem.getMediaId(), "ORIGINAL", original, null, null, null));

            // TODO: ffmpeg 도입 후 첫 프레임 썸네일 생성으로 교체 예정.
            // TODO: 추후 S3 direct upload 적용 시 영상 원본/썸네일 처리를 비동기 파이프라인으로 분리 예정.
            StoredFile thumb = createVideoPlaceholderThumb(form.getVideoFile().getOriginalFilename());
            savedKeys.add(thumb.getStorageKey());
            uploadMapper.insertMediaVariant(buildVariant(mediaItem.getMediaId(), "THUMB", thumb, 320, 180, null));

            bindTags(mediaItem.getMediaId(), form.getTags());
        } catch (Exception e) {
            rollbackFiles(savedKeys);
            if (e instanceof UploadException) {
                throw (UploadException) e;
            }
            throw new UploadException("동영상 업로드 처리 중 오류가 발생했습니다.", e);
        }
    }

    private void processPhoto(Long userId,
                              Long albumId,
                              String title,
                              String takenAtRaw,
                              String tags,
                              MultipartFile file) {
        requireAlbum(albumId);
        validateMimeType(file, "image/");

        List<String> savedKeys = new ArrayList<>();
        try {
            LocalDateTime takenAt = parseTakenAt(takenAtRaw);
            MediaItem mediaItem = createMediaItem(userId, albumId, "IMAGE", title, takenAt);

            StoredFile original = storageService.store(file, StorageCategory.ORIGINAL, LocalDate.now());
            savedKeys.add(original.getStorageKey());

            BufferedImage source = ImageIO.read(file.getInputStream());
            if (source == null) {
                throw new UploadException("이미지 파일을 읽을 수 없습니다.");
            }

            StoredFile small = saveResizedWebp(file.getOriginalFilename(), source, 720, StorageCategory.SMALL);
            savedKeys.add(small.getStorageKey());
            StoredFile medium = saveResizedWebp(file.getOriginalFilename(), source, 1280, StorageCategory.MEDIUM);
            savedKeys.add(medium.getStorageKey());

            uploadMapper.insertMediaVariant(buildVariant(mediaItem.getMediaId(), "ORIGINAL", original, source.getWidth(), source.getHeight(), null));
            uploadMapper.insertMediaVariant(buildVariant(mediaItem.getMediaId(), "SMALL", small, smallImageWidth(source, 720), smallImageHeight(source, 720), null));
            uploadMapper.insertMediaVariant(buildVariant(mediaItem.getMediaId(), "MEDIUM", medium, smallImageWidth(source, 1280), smallImageHeight(source, 1280), null));

            bindTags(mediaItem.getMediaId(), tags);
        } catch (Exception e) {
            rollbackFiles(savedKeys);
            if (e instanceof UploadException) {
                throw (UploadException) e;
            }
            throw new UploadException("이미지 업로드 처리 중 오류가 발생했습니다.", e);
        }
    }

    private StoredFile saveResizedWebp(String originalName, BufferedImage source, int targetWidth, StorageCategory category) throws IOException {
        BufferedImage resized = resizePreserveRatio(source, targetWidth);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        boolean writeOk = ImageIO.write(resized, "webp", out);
        if (!writeOk) {
            throw new UploadException("WebP 변환에 실패했습니다. webp-imageio 설정을 확인해 주세요.");
        }
        return storageService.store(out.toByteArray(), originalName, "webp", "image/webp", category, LocalDate.now());
    }

    private StoredFile createVideoPlaceholderThumb(String originalName) throws IOException {
        BufferedImage image = new BufferedImage(320, 180, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = image.createGraphics();
        g2.setColor(new Color(30, 30, 30));
        g2.fillRect(0, 0, 320, 180);
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("SansSerif", Font.BOLD, 28));
        g2.drawString("VIDEO", 110, 100);
        g2.dispose();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", out);
        return storageService.store(out.toByteArray(), originalName, "jpg", "image/jpeg", StorageCategory.THUMB, LocalDate.now());
    }

    private MediaItem createMediaItem(Long userId, Long albumId, String mediaType, String title, LocalDateTime takenAt) {
        MediaItem mediaItem = new MediaItem();
        mediaItem.setUserId(userId);
        mediaItem.setAlbumId(albumId);
        mediaItem.setMediaType(mediaType);
        mediaItem.setTitle(blankToNull(title));
        mediaItem.setUploadedAt(LocalDateTime.now());
        mediaItem.setTakenAt(takenAt);
        uploadMapper.insertMediaItem(mediaItem);
        return mediaItem;
    }

    private MediaVariant buildVariant(Long mediaId,
                                      String variantType,
                                      StoredFile storedFile,
                                      Integer width,
                                      Integer height,
                                      Integer durationSec) {
        MediaVariant variant = new MediaVariant();
        variant.setMediaId(mediaId);
        variant.setVariantType(variantType);
        variant.setStorageKey(storedFile.getStorageKey());
        variant.setFileOrgName(storedFile.getOriginalName());
        variant.setFileSaveName(storedFile.getSaveName());
        variant.setExtension(storedFile.getExtension());
        variant.setMimeType(storedFile.getMimeType());
        variant.setFileSize(storedFile.getFileSize());
        variant.setWidth(width);
        variant.setHeight(height);
        variant.setDurationSec(durationSec);
        variant.setCreatedAt(LocalDateTime.now());
        return variant;
    }

    private void bindTags(Long mediaId, String rawTags) {
        Set<String> tagSet = parseTags(rawTags);
        for (String tagName : tagSet) {
            String normalized = normalizeTag(tagName);
            Tag tag = uploadMapper.findTagByNormalizedName(normalized);
            if (tag == null) {
                tag = new Tag();
                tag.setTagName(tagName);
                tag.setNormalizedName(normalized);
                uploadMapper.insertTag(tag);
            }
            uploadMapper.insertMediaTag(mediaId, tag.getTagId());
        }
    }

    private Set<String> parseTags(String rawTags) {
        Set<String> tags = new LinkedHashSet<>();
        if (rawTags == null || rawTags.isBlank()) {
            return tags;
        }

        String[] split = rawTags.split(",");
        for (String token : split) {
            String tag = token == null ? "" : token.trim();
            if (!tag.isBlank()) {
                tags.add(tag);
            }
        }
        return tags;
    }

    private String normalizeTag(String tagName) {
        return tagName.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    }

    private void requireAlbum(Long albumId) {
        if (albumId == null) {
            throw new UploadException("앨범은 필수 선택 항목입니다.");
        }
    }

    private LocalDateTime parseTakenAt(String takenAtRaw) {
        if (takenAtRaw == null || takenAtRaw.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(takenAtRaw, TAKEN_AT_FORMAT);
        } catch (DateTimeParseException e) {
            throw new UploadException("촬영 일시 형식이 올바르지 않습니다.");
        }
    }

    private void validateMimeType(MultipartFile file, String expectedPrefix) {
        String contentType = file.getContentType();
        if (contentType == null || !contentType.toLowerCase(Locale.ROOT).startsWith(expectedPrefix)) {
            throw new UploadException("허용되지 않는 파일 형식입니다.");
        }
    }

    private void rollbackFiles(List<String> savedKeys) {
        for (String key : savedKeys) {
            storageService.delete(key);
        }
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private BufferedImage resizePreserveRatio(BufferedImage source, int maxWidth) {
        if (source.getWidth() <= maxWidth) {
            return source;
        }
        int width = maxWidth;
        int height = (int) Math.round((double) source.getHeight() * width / source.getWidth());

        Image scaled = source.getScaledInstance(width, height, Image.SCALE_SMOOTH);
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = result.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2.drawImage(scaled, 0, 0, null);
        g2.dispose();
        return result;
    }

    private int smallImageWidth(BufferedImage source, int maxWidth) {
        return Math.min(source.getWidth(), maxWidth);
    }

    private int smallImageHeight(BufferedImage source, int maxWidth) {
        if (source.getWidth() <= maxWidth) {
            return source.getHeight();
        }
        return (int) Math.round((double) source.getHeight() * maxWidth / source.getWidth());
    }
}
