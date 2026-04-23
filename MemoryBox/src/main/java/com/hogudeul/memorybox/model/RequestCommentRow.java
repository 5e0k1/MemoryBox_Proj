package com.hogudeul.memorybox.model;

import java.time.LocalDateTime;

public class RequestCommentRow {

    private Long requestCommentId;
    private Long requestId;
    private Long userId;
    private String content;
    private LocalDateTime createdAt;
    private String authorName;

    public Long getRequestCommentId() {
        return requestCommentId;
    }

    public void setRequestCommentId(Long requestCommentId) {
        this.requestCommentId = requestCommentId;
    }

    public Long getRequestId() {
        return requestId;
    }

    public void setRequestId(Long requestId) {
        this.requestId = requestId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getAuthorName() {
        return authorName;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }
}
