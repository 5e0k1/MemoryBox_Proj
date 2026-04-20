package com.hogudeul.memorybox.service;

import com.hogudeul.memorybox.dto.FeedItemView;
import com.hogudeul.memorybox.mapper.FeedMapper;
import com.hogudeul.memorybox.model.FeedRow;
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

    public FeedService(FeedMapper feedMapper) {
        this.feedMapper = feedMapper;
    }

    public List<FeedItemView> getImageFeedItems() {
        List<FeedRow> rows = feedMapper.findImageFeedRows();
        List<FeedItemView> items = new ArrayList<>();

        for (FeedRow row : rows) {
            String[] tags = parseTags(row.getTagsCsv());
            LocalDateTime displayAt = row.getTakenAt() != null ? row.getTakenAt() : row.getUploadedAt();
            int shotYear = displayAt != null ? displayAt.getYear() : 0;

            items.add(new FeedItemView(
                    row.getMediaId(),
                    "photo",
                    toPublicFileUrl(row.getStorageKey()),
                    defaultText(row.getTitle(), "(제목 없음)"),
                    defaultText(row.getDisplayName(), "알 수 없음"),
                    shotYear,
                    formatDateTime(row.getUploadedAt()),
                    safeInt(row.getLikeCount()),
                    safeInt(row.getCommentCount()),
                    tags,
                    defaultText(row.getAlbumName(), "미분류"),
                    formatDateTime(row.getTakenAt()),
                    formatDateTime(displayAt)
            ));
        }

        return items;
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

    private String toPublicFileUrl(String storageKey) {
        if (storageKey == null || storageKey.isBlank()) {
            return "";
        }
        String normalized = storageKey.replace('\\', '/');
        return "/files/" + normalized;
    }

    private String[] parseTags(String tagsCsv) {
        if (tagsCsv == null || tagsCsv.isBlank()) {
            return new String[0];
        }
        return Arrays.stream(tagsCsv.split(","))
                .map(String::trim)
                .filter(tag -> !tag.isBlank())
                .toArray(String[]::new);
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
