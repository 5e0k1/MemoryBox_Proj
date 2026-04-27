<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>MemoryBox - 공유 상세</title>
    <%@ include file="/WEB-INF/views/common/head-icons.jspf" %>
    <link rel="stylesheet" href="/css/common.css">
    <link rel="stylesheet" href="/css/detail.css">
</head>
<body class="page page-detail">
<main class="detail-layout">
    <section class="detail-panel media-panel">
        <c:choose>
            <c:when test="${detail.mediaType eq 'VIDEO' and not empty detail.originalVideoUrl}">
                <video controls playsinline preload="metadata" poster="${detail.videoThumbnailUrl}" class="detail-image">
                    <source src="${detail.originalVideoUrl}" type="video/mp4">
                </video>
            </c:when>
            <c:when test="${not empty detail.displayImageUrl}">
                <img src="${detail.displayImageUrl}" alt="${detail.title}" class="detail-image" loading="eager">
            </c:when>
            <c:otherwise>
                <div class="image-empty">표시 가능한 미리보기가 없습니다.</div>
            </c:otherwise>
        </c:choose>
    </section>

    <section class="detail-panel meta-panel">
        <h1 class="detail-title">${detail.title}</h1>
        <div class="meta-grid">
            <div><span>업로더</span><strong>${detail.authorName}</strong></div>
            <div><span>앨범</span><strong class="chip-like">${detail.albumName}</strong></div>
            <div><span>타입</span><strong>${detail.mediaType}</strong></div>
            <div><span>댓글 수</span><strong>${detail.commentCount}</strong></div>
        </div>
    </section>

    <c:if test="${allowDownload}">
        <section class="detail-panel action-panel">
            <a href="${detail.downloadUrl}" class="btn btn-secondary download-btn" download>다운로드</a>
        </section>
    </c:if>

    <c:if test="${allowComments}">
        <section class="detail-panel comment-panel">
            <h2>댓글</h2>
            <ul class="comment-list">
                <c:forEach var="comment" items="${comments}">
                    <li class="comment-item">
                        <div class="comment-head">
                            <strong>${comment.authorName}</strong>
                            <span>${comment.createdAt}</span>
                        </div>
                        <p>${comment.content}</p>
                    </li>
                </c:forEach>
                <c:if test="${empty comments}">
                    <li class="comment-empty">공개된 댓글이 없습니다.</li>
                </c:if>
            </ul>
        </section>
    </c:if>
</main>
</body>
</html>
