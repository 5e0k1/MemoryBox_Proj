package com.hogudeul.memorybox.service;

import com.hogudeul.memorybox.dto.FeedItemView;
import com.hogudeul.memorybox.dto.FeedMediaItemView;
import com.hogudeul.memorybox.dto.SearchMediaItemView;
import com.hogudeul.memorybox.mapper.FeedMapper;
import com.hogudeul.memorybox.model.FeedMediaRow;
import com.hogudeul.memorybox.model.FeedRow;
import com.hogudeul.memorybox.model.SearchMediaItemRow;
import com.hogudeul.memorybox.storage.StorageUrlResolver;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class FeedService {

    private static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final FeedMapper feedMapper;
    private final TimeDisplayService timeDisplayService;
    private final StorageUrlResolver storageUrlResolver;

    public FeedService(FeedMapper feedMapper,
                       TimeDisplayService timeDisplayService,
                       StorageUrlResolver storageUrlResolver) {
        this.feedMapper = feedMapper;
        this.timeDisplayService = timeDisplayService;
        this.storageUrlResolver = storageUrlResolver;
    }

    public List<FeedItemView> getImageFeedItems() {
        return toView(feedMapper.findImageFeedRows());
    }

    public List<FeedItemView> getFeedItems(String mediaType, String author, String album, String tag,
                                           String sort, Long userId, boolean likedOnly, boolean mineOnly,
                                           int page, int size) {
        int safeSize = Math.max(1, Math.min(size, 60));
        int safePage = Math.max(1, page);
        int offset = (safePage - 1) * safeSize;

        return toView(feedMapper.findFeedRows(
                normalizeFilter(mediaType),
                normalizeFilter(author),
                normalizeFilter(album),
                normalizeFilter(tag),
                normalizeSort(sort),
                userId,
                likedOnly,
                mineOnly,
                safeSize,
                offset));
    }


    public int getFeedItemCount(String mediaType, String author, String album, String tag,
                                Long userId, boolean likedOnly, boolean mineOnly) {
        return feedMapper.countFeedRows(
                normalizeFilter(mediaType),
                normalizeFilter(author),
                normalizeFilter(album),
                normalizeFilter(tag),
                userId,
                likedOnly,
                mineOnly
        );
    }

    public List<SearchMediaItemView> getSearchMediaItems(String mediaType, String author, String album, String tag,
                                                         String sort, Long userId, boolean likedOnly, boolean mineOnly,
                                                         int page, int size) {
        int safeSize = Math.max(1, Math.min(size, 60));
        int safePage = Math.max(1, page);
        int offset = (safePage - 1) * safeSize;

        List<SearchMediaItemRow> rows = feedMapper.findSearchMediaItems(
                normalizeFilter(mediaType),
                normalizeFilter(author),
                normalizeFilter(album),
                normalizeFilter(tag),
                normalizeSort(sort),
                userId,
                likedOnly,
                mineOnly,
                safeSize,
                offset);

        List<Long> batchIds = rows.stream().map(SearchMediaItemRow::getBatchId).filter(id -> id != null).distinct().toList();
        java.util.Map<Long, java.util.List<FeedMediaItemView>> mediaByBatch = buildMediaItemsMap(batchIds);
        List<SearchMediaItemView> items = new ArrayList<>();
        for (SearchMediaItemRow row : rows) {
            items.add(new SearchMediaItemView(
                    row.getMediaId(),
                    row.getBatchId(),
                    toUiMediaType(row.getMediaType()),
                    toPublicFileUrl(row.getSmallStorageKey()),
                    toPublicFileUrl(row.getMediumStorageKey()),
                    toPublicFileUrl(row.getPreviewStorageKey()),
                    "/feed/media/" + row.getMediaId() + "/download",
                    defaultText(row.getTitle(), "(제목 없음)"),
                    defaultText(row.getDisplayName(), "알 수 없음"),
                    timeDisplayService.formatTakenDate(row.getTakenAt()),
                    timeDisplayService.formatRelativeUploadedAt(row.getUploadedAt()),
                    safeInt(row.getBatchMediaCount()),
                    mediaByBatch.getOrDefault(row.getBatchId(), java.util.List.of())
            ));
        }
        return items;
    }

    public int countSearchMediaItems(String mediaType, String author, String album, String tag,
                                     Long userId, boolean likedOnly, boolean mineOnly) {
        return feedMapper.countSearchMediaItems(
                normalizeFilter(mediaType),
                normalizeFilter(author),
                normalizeFilter(album),
                normalizeFilter(tag),
                userId,
                likedOnly,
                mineOnly
        );
    }

    public List<String> getAuthorFilterOptions(List<FeedItemView> feedItems) {
        Set<String> authors = new LinkedHashSet<>();
        authors.add("전체");
        for (FeedItemView item : feedItems) {
            authors.add(item.getAuthor());
        }
        return new ArrayList<>(authors);
    }

    public List<String> getAlbumFilterOptions(List<FeedItemView> feedItems) {
        Set<String> albums = new LinkedHashSet<>();
        albums.add("전체");
        for (FeedItemView item : feedItems) {
            albums.add(item.getAlbumName());
        }
        return new ArrayList<>(albums);
    }

    public List<String> getTagFilterOptions(List<FeedItemView> feedItems) {
        Set<String> tags = new LinkedHashSet<>();
        for (FeedItemView item : feedItems) {
            tags.addAll(Arrays.asList(item.getTags()));
        }

        List<String> options = new ArrayList<>();
        options.add("전체");
        options.addAll(tags);
        return options;
    }

    public List<String> getTagFilterOptionsWithoutAll(List<FeedItemView> feedItems) {
        Set<String> tags = new LinkedHashSet<>();
        for (FeedItemView item : feedItems) {
            tags.addAll(Arrays.asList(item.getTags()));
        }
        List<String> sortedTags = new ArrayList<>(tags);
        sortedTags.sort((left, right) -> {
            boolean leftPerson = left.startsWith("@");
            boolean rightPerson = right.startsWith("@");
            if (leftPerson == rightPerson) {
                return left.compareToIgnoreCase(right);
            }
            return leftPerson ? -1 : 1;
        });
        return sortedTags;
    }

    private List<FeedItemView> toView(List<FeedRow> rows) {
        List<FeedItemView> items = new ArrayList<>();
        List<Long> batchIds = rows.stream().map(FeedRow::getMediaId).filter(id -> id != null).toList();
        java.util.Map<Long, java.util.List<FeedMediaItemView>> mediaByBatch = buildMediaItemsMap(batchIds);

        for (FeedRow row : rows) {
            String[] tags = parseTags(row.getTagsCsv());
            LocalDateTime displayAt = row.getTakenAt() != null ? row.getTakenAt() : row.getUploadedAt();
            int shotYear = displayAt != null ? displayAt.getYear() : 0;
            List<FeedMediaItemView> mediaItems = mediaByBatch.getOrDefault(row.getMediaId(), java.util.List.of());

            items.add(new FeedItemView(
                    row.getMediaId(),
                    toUiMediaType(row.getMediaType()),
                    toPublicFileUrl(row.getThumbnailStorageKey()),
                    toPublicFileUrl(row.getPreviewStorageKey()),
                    defaultText(row.getTitle(), "(제목 없음)"),
                    defaultText(row.getDisplayName(), "알 수 없음"),
                    shotYear,
                    formatDateTime(row.getUploadedAt()),
                    safeInt(row.getLikeCount()),
                    safeInt(row.getCommentCount()),
                    safeInt(row.getLikedByMe()) > 0,
                    tags,
                    defaultText(row.getAlbumName(), "미분류"),
                    timeDisplayService.formatTakenDate(row.getTakenAt()),
                    formatDateTime(displayAt),
                    timeDisplayService.formatRelativeUploadedAt(row.getUploadedAt()),
                    timeDisplayService.isNew(row.getUploadedAt()),
                    mediaItems
            ));
        }

        return items;
    }


    private java.util.Map<Long, java.util.List<FeedMediaItemView>> buildMediaItemsMap(List<Long> batchIds) {
        if (batchIds == null || batchIds.isEmpty()) {
            return java.util.Map.of();
        }
        List<FeedMediaRow> mediaRows = feedMapper.findMediaItemsByBatchIds(batchIds);
        java.util.Map<Long, java.util.List<FeedMediaItemView>> grouped = new java.util.LinkedHashMap<>();
        for (FeedMediaRow row : mediaRows) {
            grouped.computeIfAbsent(row.getBatchId(), key -> new ArrayList<>()).add(
                    new FeedMediaItemView(
                            row.getMediaId(),
                            toUiMediaType(row.getMediaType()),
                            toPublicFileUrl(row.getSmallStorageKey()),
                            toPublicFileUrl(row.getMediumStorageKey()),
                            toPublicFileUrl(row.getPreviewStorageKey()),
                            defaultText(row.getOriginalFileName(), ""),
                            safeInt(row.getSortOrder())
                    )
            );
        }
        return grouped;
    }

    private String normalizeFilter(String value) {
        if (value == null || value.isBlank() || "전체".equals(value) || "all".equalsIgnoreCase(value)) {
            return null;
        }
        if ("photo".equalsIgnoreCase(value)) {
            return "IMAGE";
        }
        if ("video".equalsIgnoreCase(value)) {
            return "VIDEO";
        }
        if (value.startsWith("#") || value.startsWith("@")) {
            return value.substring(1);
        }
        return value;
    }

    private String normalizeSort(String value) {
        if (value == null || value.isBlank()) {
            return "uploaded_desc";
        }
        return switch (value) {
            case "uploaded_asc", "taken_desc", "taken_asc", "likes_desc" -> value;
            default -> "uploaded_desc";
        };
    }

    private String toUiMediaType(String mediaType) {
        if (mediaType == null) {
            return "photo";
        }
        return "VIDEO".equalsIgnoreCase(mediaType) ? "video" : "photo";
    }

    private String toPublicFileUrl(String storageKey) {
        return storageUrlResolver.resolvePublicUrl(storageKey);
    }

    private String[] parseTags(String tagsCsv) {
        if (tagsCsv == null || tagsCsv.isBlank()) {
            return new String[0];
        }
        return Arrays.stream(tagsCsv.split(","))
                .map(String::trim)
                .filter(tag -> !tag.isBlank())
                .map(this::formatScopedTag)
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

    private String formatDateTime(LocalDateTime time) {
        if (time == null) {
            return "";
        }
        return time.format(DATETIME_FORMAT);
    }

    private String defaultText(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value;
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }
}
