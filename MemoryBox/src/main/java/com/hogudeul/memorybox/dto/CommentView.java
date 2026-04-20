package com.hogudeul.memorybox.dto;

public class CommentView {

    private final Long commentId;
    private final Long parentId;
    private final String authorName;
    private final String content;
    private final String createdAt;
    private final boolean mine;
    private final java.util.List<CommentView> replies = new java.util.ArrayList<>();

    public CommentView(Long commentId, Long parentId, String authorName, String content, String createdAt, boolean mine) {
        this.commentId = commentId;
        this.parentId = parentId;
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

    public Long getParentId() {
        return parentId;
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

    public java.util.List<CommentView> getReplies() {
        return replies;
    }

    public void addReply(CommentView reply) {
        this.replies.add(reply);
    }
}
