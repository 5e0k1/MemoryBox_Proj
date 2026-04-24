package com.hogudeul.memorybox.service;

import com.hogudeul.memorybox.mapper.RequestMapper;
import com.hogudeul.memorybox.model.RequestCommentRow;
import com.hogudeul.memorybox.model.RequestPostRow;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RequestService {

    private static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final RequestMapper requestMapper;
    private final TimeDisplayService timeDisplayService;

    public RequestService(RequestMapper requestMapper, TimeDisplayService timeDisplayService) {
        this.requestMapper = requestMapper;
        this.timeDisplayService = timeDisplayService;
    }

    public List<Map<String, Object>> getRequestPosts() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (RequestPostRow row : requestMapper.findRequestPosts()) {
            result.add(toPostMap(row));
        }
        return result;
    }

    public Map<String, Object> getRequestPost(Long requestId) {
        RequestPostRow row = requestMapper.findRequestPostById(requestId);
        if (row == null) {
            return null;
        }

        Map<String, Object> payload = toPostMap(row);
        List<Map<String, Object>> comments = new ArrayList<>();
        for (RequestCommentRow commentRow : requestMapper.findCommentsByRequestId(requestId)) {
            Map<String, Object> comment = new LinkedHashMap<>();
            comment.put("requestCommentId", commentRow.getRequestCommentId());
            comment.put("authorName", commentRow.getAuthorName());
            comment.put("content", commentRow.getContent());
            comment.put("createdAt", commentRow.getCreatedAt() == null ? "" : commentRow.getCreatedAt().format(DATETIME_FORMAT));
            comment.put("relativeCreatedAt", timeDisplayService.formatRelativeUploadedAt(commentRow.getCreatedAt()));
            comments.add(comment);
        }
        payload.put("comments", comments);
        return payload;
    }

    @Transactional
    public boolean createRequestPost(Long userId, String title, String content) {
        String normalizedTitle = title == null ? "" : title.trim();
        String normalizedContent = content == null ? "" : content.trim();
        if (userId == null || normalizedTitle.isBlank() || normalizedContent.isBlank()) {
            return false;
        }
        return requestMapper.insertRequestPost(userId, normalizedTitle, normalizedContent) > 0;
    }

    @Transactional
    public boolean createRequestComment(Long requestId, Long userId, String content) {
        String normalizedContent = content == null ? "" : content.trim();
        if (requestId == null || userId == null || normalizedContent.isBlank()) {
            return false;
        }
        return requestMapper.insertRequestComment(requestId, userId, normalizedContent) > 0;
    }

    private Map<String, Object> toPostMap(RequestPostRow row) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("requestId", row.getRequestId());
        map.put("authorName", row.getAuthorName());
        map.put("title", row.getTitle());
        map.put("content", row.getContent());
        map.put("commentCount", row.getCommentCount() == null ? 0 : row.getCommentCount());
        map.put("createdAt", row.getCreatedAt() == null ? "" : row.getCreatedAt().format(DATETIME_FORMAT));
        map.put("relativeCreatedAt", timeDisplayService.formatRelativeUploadedAt(row.getCreatedAt()));
        return map;
    }
}
