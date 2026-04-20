package com.hogudeul.memorybox.dto;

public class CommentView {

    private final Long commentId;
    private final String authorName;
    private final String content;
    private final String createdAt;
    private final boolean mine;

    public CommentView(Long commentId, String authorName, String content, String createdAt, boolean mine) {
        this.commentId = commentId;
        this.authorName = authorName;
        this.content = content;
        this.createdAt = createdAt;
        this.mine = mine;
    }

    public Long getCommentId() {
        return commentId;
    }

    public String getAuthorName() {
        return authorName;
    }

    public String getContent() {
        return content;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public boolean isMine() {
        return mine;
    }
}
