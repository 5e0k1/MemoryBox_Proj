package com.hogudeul.memorybox.service;

import com.hogudeul.memorybox.dto.CommentView;
import com.hogudeul.memorybox.dto.MediaDetailView;
import com.hogudeul.memorybox.mapper.DetailMapper;
import com.hogudeul.memorybox.mapper.UploadMapper;
import com.hogudeul.memorybox.model.CommentRow;
import com.hogudeul.memorybox.model.MediaDetailRow;
import com.hogudeul.memorybox.model.Tag;
import com.hogudeul.memorybox.storage.StorageUrlResolver;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.hogudeul.memorybox.config.StorageProperties;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DetailService {

    private static final Logger log = LoggerFactory.getLogger(DetailService.class);
    public static final int MAX_DOWNLOAD_COUNT = 30;
    private static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final DetailMapper detailMapper;
    private final Path storageRoot;
    private final TimeDisplayService timeDisplayService;
    private final NotificationService notificationService;
    private final UploadMapper uploadMapper;
    private final StorageUrlResolver storageUrlResolver;

    public DetailService(DetailMapper detailMapper,
                         TimeDisplayService timeDisplayService,
                         NotificationService notificationService,
                         UploadMapper uploadMapper,
                         StorageProperties storageProperties,
                         StorageUrlResolver storageUrlResolver) {
        this.detailMapper = detailMapper;
        this.timeDisplayService = timeDisplayService;
        this.notificationService = notificationService;
        this.uploadMapper = uploadMapper;
        this.storageRoot = Paths.get(storageProperties.getLocalRoot()).toAbsolutePath().normalize();
        this.storageUrlResolver = storageUrlResolver;
    }

    public MediaDetailView getMediaDetail(Long mediaId, Long userId) {
        MediaDetailRow row = detailMapper.findDetailByMediaId(mediaId, userId);
        if (row == null) {
            return null;
        }

        boolean isVideo = "VIDEO".equalsIgnoreCase(row.getMediaType());
        String displayStorageKey = row.getMediumStorageKey();
        if (isBlank(displayStorageKey)) {
            displayStorageKey = row.getSmallStorageKey();
        }
        if (isVideo && isBlank(displayStorageKey)) {
            displayStorageKey = row.getThumbStorageKey();
        }

        return new MediaDetailView(
                row.getMediaId(),
                row.getAlbumId(),
                defaultText(row.getTitle(), "(제목 없음)"),
                defaultText(row.getMediaType(), "UNKNOWN"),
                formatDateTime(row.getUploadedAt()),
                timeDisplayService.formatTakenDate(row.getTakenAt()),
                timeDisplayService.formatRelativeUploadedAt(row.getUploadedAt()),
                defaultText(row.getAlbumName(), "미분류"),
                defaultText(row.getDisplayName(), "알 수 없음"),
                isVideo ? "" : toPublicFileUrl(displayStorageKey),
                isVideo ? toPublicFileUrl(row.getOriginalStorageKey()) : "",
                isVideo ? toPublicFileUrl(row.getThumbStorageKey()) : "",
                "",
                storageUrlResolver.resolveDownloadUrl(mediaId, row.getOriginalStorageKey()),
                parseTags(row.getTagsCsv()),
                safeInt(row.getLikeCount()),
                safeInt(row.getCommentCount()),
                safeInt(row.getLikedByMe()) > 0,
                !isBlank(row.getOriginalStorageKey()),
                userId != null && userId.equals(row.getUserId())
        );
    }

    public List<CommentView> getComments(Long mediaId, Long viewerUserId) {
        List<CommentRow> rows = detailMapper.findCommentsByMediaId(mediaId);
        Map<Long, CommentView> commentMap = new LinkedHashMap<>();
        Map<Long, LocalDateTime> createdAtMap = new LinkedHashMap<>();
        List<CommentView> rootComments = new ArrayList<>();

        for (CommentRow row : rows) {
            CommentView commentView = toCommentView(row, viewerUserId);
            commentMap.put(row.getCommentId(), commentView);
            createdAtMap.put(row.getCommentId(), row.getCreatedAt());
        }

        for (CommentView commentView : commentMap.values()) {
            if (commentView.getParentId() == null) {
                rootComments.add(commentView);
                continue;
            }

            CommentView parent = commentMap.get(commentView.getParentId());
            if (parent != null) {
                parent.addReply(commentView);
            } else {
                // 방어 로직: 부모가 조회 결과에 없는 경우 최상위로 노출
                rootComments.add(commentView);
            }
        }

        Comparator<CommentView> newestFirst = Comparator.comparing(
                comment -> createdAtMap.get(comment.getCommentId()),
                Comparator.nullsLast(Comparator.reverseOrder())
        );
        Comparator<CommentView> oldestFirst = Comparator.comparing(
                comment -> createdAtMap.get(comment.getCommentId()),
                Comparator.nullsLast(Comparator.naturalOrder())
        );

        rootComments.sort(newestFirst);
        for (CommentView rootComment : rootComments) {
            rootComment.getReplies().sort(oldestFirst);
        }

        return rootComments;
    }

    public DownloadFileInfo getDownloadFileInfo(Long mediaId, Long userId) {
        MediaDetailRow row = detailMapper.findDetailByMediaId(mediaId, userId);
        if (row == null || isBlank(row.getOriginalStorageKey())) {
            return null;
        }
        return toDownloadFileInfo(row);
    }

    public List<DownloadFileInfo> getDownloadFileInfos(List<Long> mediaIds, Long userId) {
        validateDownloadRequest(mediaIds);

        List<MediaDetailRow> rows = detailMapper.findDetailsByMediaIds(mediaIds, userId);
        if (rows.size() != mediaIds.size()) {
            throw new DownloadException("유효하지 않은 파일 ID가 포함되어 있습니다.");
        }

        Map<Long, MediaDetailRow> rowMap = new HashMap<>();
        for (MediaDetailRow row : rows) {
            rowMap.put(row.getMediaId(), row);
        }

        List<DownloadFileInfo> result = new ArrayList<>();
        for (Long mediaId : mediaIds) {
            MediaDetailRow row = rowMap.get(mediaId);
            if (row == null) {
                throw new DownloadException("유효하지 않은 파일 ID가 포함되어 있습니다.");
            }
            if (isBlank(row.getOriginalStorageKey())) {
                throw new DownloadException("원본 파일이 없는 항목이 포함되어 있습니다.");
            }
            result.add(toDownloadFileInfo(row));
        }
        return result;
    }

    public void streamZip(List<DownloadFileInfo> files, OutputStream outputStream) throws IOException {
        Map<String, Integer> nameCounter = new HashMap<>();
        ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream);
        log.debug("streamZip start. fileCount={}", files.size());
        try {
            for (DownloadFileInfo file : files) {
                if (!file.existsReadable()) {
                    throw new DownloadException("원본 파일을 찾을 수 없습니다.");
                }

                String zipEntryName = resolveDuplicatedName(file.getFileName(), nameCounter);
                zipOutputStream.putNextEntry(new ZipEntry(zipEntryName));
                try (InputStream inputStream = new BufferedInputStream(file.openInputStream())) {
                    inputStream.transferTo(zipOutputStream);
                }
                zipOutputStream.closeEntry();
            }
            zipOutputStream.finish();
            zipOutputStream.flush();
            log.debug("streamZip finish. fileCount={}", files.size());
        } catch (DownloadException e) {
            log.warn("streamZip domain failure. msg={}", e.getMessage());
            throw e;
        } catch (IOException e) {
            log.warn("streamZip io failure. msg={}", e.getMessage());
            throw new IOException("ZIP 생성 중 오류가 발생했습니다.", e);
        } finally {
            log.debug("streamZip finally. fileCount={}", files.size());
        }
    }

    @Transactional
    public void logDownloadAttempt(Long mediaId,
                                   Long userId,
                                   String ipAddr,
                                   String userAgent,
                                   boolean success,
                                   String failReason) {
        Long dlId = detailMapper.selectNextDownloadLogId();
        detailMapper.insertDownloadLog(
                dlId,
                userId,
                mediaId,
                ipAddr,
                userAgent,
                success ? "Y" : "N",
                failReason
        );
    }

    private void validateDownloadRequest(List<Long> mediaIds) {
        if (mediaIds == null || mediaIds.isEmpty()) {
            throw new DownloadException("다운로드할 파일을 선택해 주세요.");
        }
        if (mediaIds.size() > MAX_DOWNLOAD_COUNT) {
            throw new DownloadException("한 번에 최대 30개까지만 다운로드할 수 있습니다.");
        }
        if (mediaIds.stream().anyMatch(id -> id == null || id <= 0L)) {
            throw new DownloadException("유효하지 않은 파일 ID가 포함되어 있습니다.");
        }
    }

    private String resolveDuplicatedName(String originalName, Map<String, Integer> nameCounter) {
        String normalized = defaultText(originalName, "download");
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

    private DownloadFileInfo toDownloadFileInfo(MediaDetailRow row) {
        Path filePath = storageRoot.resolve(row.getOriginalStorageKey()).normalize();
        return new DownloadFileInfo(filePath, defaultText(row.getOriginalFileName(), "download"), row.getOriginalMimeType());
    }

    @Transactional
    public boolean setLike(Long mediaId, Long userId, boolean like) {
        if (detailMapper.findDetailByMediaId(mediaId, userId) == null) {
            return false;
        }

        if (like) {
            try {
                detailMapper.insertLike(mediaId, userId);
            } catch (DuplicateKeyException ignore) {
                // 이미 좋아요된 상태면 멱등 처리.
            }
            return true;
        }

        detailMapper.deleteLike(mediaId, userId);
        return true;
    }

    @Transactional
    public boolean addComment(Long mediaId, Long userId, String content, Long parentId) {
        if (detailMapper.findDetailByMediaId(mediaId, userId) == null) {
            return false;
        }

        String normalized = content == null ? "" : content.trim();
        if (normalized.isBlank()) {
            return false;
        }

        Long normalizedParentId = null;
        if (parentId != null) {
            CommentRow parent = detailMapper.findCommentById(parentId);
            if (parent == null) {
                return false;
            }
            if (!mediaId.equals(parent.getMediaId())) {
                return false;
            }
            if (parent.getParentId() != null) {
                return false;
            }
            normalizedParentId = parentId;
        }

        Long commentId = detailMapper.selectNextCommentId();
        detailMapper.insertComment(commentId, mediaId, normalizedParentId, userId, normalized);

        if (normalizedParentId == null) {
            notificationService.notifyMediaOwnerForComment(userId, mediaId, commentId);
        } else {
            notificationService.notifyCommentOwnerForReply(userId, normalizedParentId, commentId);
        }
        return true;
    }

    @Transactional
    public boolean updateMediaMeta(Long mediaId, Long userId, String title, Long albumId) {
        if (mediaId == null || userId == null || albumId == null) {
            return false;
        }
        String normalizedTitle = title == null ? "" : title.trim();
        return detailMapper.updateMediaMeta(mediaId, userId, normalizedTitle, albumId) > 0;
    }

    @Transactional
    public boolean updateMediaTags(Long mediaId, Long userId, List<Long> selectedTagIds, String newTagsRaw) {
        if (mediaId == null || userId == null) {
            return false;
        }
        if (detailMapper.findDetailByMediaId(mediaId, userId) == null) {
            return false;
        }

        Set<Long> finalTagIds = resolveSelectedTagIds(selectedTagIds);
        Set<String> newTagNames = parseTagInput(newTagsRaw);

        for (String tagName : newTagNames) {
            String normalized = normalizeTag(tagName);
            Tag tag = uploadMapper.findTagByNormalizedName(normalized);
            if (tag == null) {
                tag = new Tag();
                tag.setTagId(uploadMapper.selectNextTagId());
                tag.setUserId(userId);
                tag.setTagName(tagName);
                tag.setNormalizedName(normalized);
                uploadMapper.insertTag(tag);
            }
            finalTagIds.add(tag.getTagId());
        }

        detailMapper.deleteMediaTags(mediaId);
        for (Long tagId : finalTagIds) {
            Long mdId = detailMapper.selectNextMediaTagId();
            detailMapper.insertMediaTag(mdId, mediaId, tagId);
        }
        return true;
    }

    private CommentView toCommentView(CommentRow row, Long viewerUserId) {
        boolean mine = viewerUserId != null && viewerUserId.equals(row.getUserId());
        return new CommentView(
                row.getCommentId(),
                row.getParentId(),
                defaultText(row.getDisplayName(), "알 수 없음"),
                defaultText(row.getContent(), ""),
                timeDisplayService.formatRelativeUploadedAt(row.getCreatedAt()),
                mine
        );
    }

    private String[] parseTags(String tagsCsv) {
        if (isBlank(tagsCsv)) {
            return new String[0];
        }
        return Arrays.stream(tagsCsv.split(","))
                .map(String::trim)
                .filter(tag -> !tag.isBlank())
                .map(this::formatScopedTag)
                .sorted((left, right) -> {
                    boolean leftPerson = left.startsWith("@");
                    boolean rightPerson = right.startsWith("@");
                    if (leftPerson == rightPerson) {
                        return left.compareToIgnoreCase(right);
                    }
                    return leftPerson ? -1 : 1;
                })
                .toArray(String[]::new);
    }

    private String formatScopedTag(String rawTag) {
        int delimiterIndex = rawTag.indexOf("|");
        if (delimiterIndex < 0) {
            return "#" + rawTag;
        }
        String scope = rawTag.substring(0, delimiterIndex);
        String name = rawTag.substring(delimiterIndex + 1);
        if ("P".equalsIgnoreCase(scope)) {
            return "@" + name;
        }
        return "#" + name;
    }

    private Set<Long> resolveSelectedTagIds(List<Long> selectedTagIds) {
        Set<Long> sanitized = new LinkedHashSet<>();
        if (selectedTagIds == null || selectedTagIds.isEmpty()) {
            return sanitized;
        }
        for (Long tagId : selectedTagIds) {
            if (tagId != null) {
                sanitized.add(tagId);
            }
        }
        if (sanitized.isEmpty()) {
            return sanitized;
        }
        List<Tag> validTags = uploadMapper.findActiveTagsByIds(new ArrayList<>(sanitized));
        Set<Long> validTagIds = new LinkedHashSet<>();
        for (Tag tag : validTags) {
            validTagIds.add(tag.getTagId());
        }
        if (!validTagIds.containsAll(sanitized)) {
            throw new DownloadException("선택할 수 없는 태그가 포함되어 있습니다.");
        }
        return sanitized;
    }

    private Set<String> parseTagInput(String rawTags) {
        Set<String> tags = new LinkedHashSet<>();
        if (rawTags == null || rawTags.isBlank()) {
            return tags;
        }
        for (String token : rawTags.split(",")) {
            String tag = token == null ? "" : token.trim();
            if (!tag.isBlank()) {
                tags.add(tag);
            }
        }
        return tags;
    }

    private String normalizeTag(String tagName) {
        return tagName.trim().toLowerCase(java.util.Locale.ROOT).replaceAll("\\s+", " ");
    }

    private String toPublicFileUrl(String storageKey) {
        return storageUrlResolver.resolvePublicUrl(storageKey);
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    private String formatDateTime(LocalDateTime time) {
        return time == null ? "" : time.format(DATETIME_FORMAT);
    }

    private String defaultText(String value, String fallback) {
        return isBlank(value) ? fallback : value;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public static class DownloadFileInfo {
        private final Path filePath;
        private final String fileName;
        private final String mimeType;

        public DownloadFileInfo(Path filePath, String fileName, String mimeType) {
            this.filePath = filePath;
            this.fileName = fileName;
            this.mimeType = mimeType;
        }

        public Path getFilePath() {
            return filePath;
        }

        public String getFileName() {
            return fileName;
        }

        public String getMimeType() {
            return mimeType;
        }

        public boolean existsReadable() {
            try {
                return java.nio.file.Files.exists(filePath) && java.nio.file.Files.isReadable(filePath);
            } catch (Exception ignore) {
                return false;
            }
        }

        public InputStream openInputStream() throws IOException {
            return java.nio.file.Files.newInputStream(filePath);
        }
    }

    public static class DownloadException extends RuntimeException {
        public DownloadException(String message) {
            super(message);
        }
    }
}
