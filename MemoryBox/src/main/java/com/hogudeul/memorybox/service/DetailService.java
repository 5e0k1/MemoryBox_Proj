package com.hogudeul.memorybox.service;

import com.hogudeul.memorybox.dto.CommentView;
import com.hogudeul.memorybox.dto.MediaDetailView;
import com.hogudeul.memorybox.mapper.DetailMapper;
import com.hogudeul.memorybox.model.CommentRow;
import com.hogudeul.memorybox.model.MediaDetailRow;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Comparator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DetailService {

    private static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final DetailMapper detailMapper;
    private final Path storageRoot;

    public DetailService(DetailMapper detailMapper,
                         @Value("${app.storage.local-root:D:/memorybox/upload/}") String storageRoot) {
        this.detailMapper = detailMapper;
        this.storageRoot = Paths.get(storageRoot).toAbsolutePath().normalize();
    }

    public MediaDetailView getMediaDetail(Long mediaId, Long userId) {
        MediaDetailRow row = detailMapper.findDetailByMediaId(mediaId, userId);
        if (row == null) {
            return null;
        }

        String displayStorageKey = row.getMediumStorageKey();
        if (isBlank(displayStorageKey)) {
            displayStorageKey = row.getSmallStorageKey();
        }

        return new MediaDetailView(
                row.getMediaId(),
                defaultText(row.getTitle(), "(제목 없음)"),
                defaultText(row.getMediaType(), "UNKNOWN"),
                formatDateTime(row.getUploadedAt()),
                formatDateTime(row.getTakenAt()),
                defaultText(row.getAlbumName(), "미분류"),
                defaultText(row.getDisplayName(), "알 수 없음"),
                toPublicFileUrl(displayStorageKey),
                "",
                "/feed/" + mediaId + "/download",
                parseTags(row.getTagsCsv()),
                safeInt(row.getLikeCount()),
                safeInt(row.getCommentCount()),
                safeInt(row.getLikedByMe()) > 0,
                !isBlank(row.getOriginalStorageKey())
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
        return true;
    }

    private CommentView toCommentView(CommentRow row, Long viewerUserId) {
        boolean mine = viewerUserId != null && viewerUserId.equals(row.getUserId());
        return new CommentView(
                row.getCommentId(),
                row.getParentId(),
                defaultText(row.getDisplayName(), "알 수 없음"),
                defaultText(row.getContent(), ""),
                formatDateTime(row.getCreatedAt()),
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
                .toArray(String[]::new);
    }

    private String toPublicFileUrl(String storageKey) {
        if (isBlank(storageKey)) {
            return "";
        }
        return "/files/" + storageKey.replace('\\', '/');
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
    }
}
