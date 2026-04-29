<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>MemoryBox - 공유 영상</title>
    <link rel="stylesheet" href="/css/common.css">
    <link rel="stylesheet" href="/css/detail.css">
</head>
<body class="page page-detail share-page ${allowDownload ? '' : 'no-download'}">
<main class="detail-layout">
    <header class="detail-header">
        <a href="/share/${shareToken}" class="back-link" aria-label="공유 상세로 돌아가기">← 공유 상세</a>
        <div class="login-chip">게스트 공유</div>
    </header>

    <section class="detail-panel meta-panel">
        <h1 class="detail-title">${detail.title}</h1>
        <p class="meta-line">작성자 ${detail.authorName}</p>
        <p class="meta-line">업로드 ${detail.relativeUploadedAt}</p>
        <p class="meta-line">앨범 ${detail.albumName} · ${detail.commentCount} 댓글</p>
    </section>

    <section class="detail-panel media-panel">
        <video class="detail-image" controls playsinline preload="metadata"
               poster="${video.posterUrl}" src="${video.playbackUrl}"></video>
    </section>

    <c:if test="${allowDownload}">
        <section class="detail-panel meta-panel">
            <a href="/share/${shareToken}/media/${video.mediaId}/download" class="btn btn-secondary">다운로드</a>
        </section>
    </c:if>

    <c:if test="${allowComments}">
        <section class="detail-panel comment-panel">
            <h2>댓글</h2>
            <ul class="comment-list">
                <c:forEach var="comment" items="${comments}">
                    <li class="comment-item">
                        <div class="comment-head"><strong>${comment.authorName}</strong><span>${comment.createdAt}</span></div>
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
