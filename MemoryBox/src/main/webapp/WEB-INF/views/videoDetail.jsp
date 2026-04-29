<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="ko"><head><meta charset="UTF-8"><title>Video Detail</title><link rel="stylesheet" href="/css/common.css"><link rel="stylesheet" href="/css/detail.css"></head>
<body class="page page-detail"><main class="detail-layout with-bottom-nav">
    <header class="detail-header"><a href="/feed" class="back-link">← 피드</a><div class="login-chip">${loginUser.displayName}</div></header>
    <c:if test="${notFound}"><section class="detail-panel state-panel"><p>영상을 찾을 수 없습니다.</p></section></c:if>
    <c:if test="${not notFound}">
        <section class="detail-panel meta-panel">
            <div class="title-row">
                <h1 class="detail-title">${video.title}</h1>
                <div class="meta-action-buttons">
                    <a href="/feed/${video.batchId}" class="share-open-btn" aria-label="공유하기">
                        <img src="/images/share-btn-img.png" alt="공유하기" width="20" height="20">
                    </a>
                    <a href="/feed/${video.batchId}" class="btn btn-secondary">전체 보기</a>
                </div>
            </div>
            <p class="meta-line">작성자 ${detail.authorName}</p>
            <p class="meta-line">업로드 ${detail.relativeUploadedAt}</p>
            <p class="meta-line">앨범 ${detail.albumName} · ${detail.commentCount} 댓글</p>
            <div class="engagement-row">
                <form action="/feed/${video.batchId}/like" method="post" class="inline-form">
                    <input type="hidden" name="action" value="${detail.likedByMe ? 'unlike' : 'like'}">
                    <button type="submit" class="btn btn-secondary like-btn ${detail.likedByMe ? 'is-liked' : ''}">
                        ${detail.likedByMe ? '❤ 좋아요 취소' : '♡ 좋아요'} · ${detail.likeCount}
                    </button>
                </form>
            </div>
        </section>
        <section class="detail-panel media-panel">
            <video class="detail-image" controls playsinline preload="metadata" poster="${video.posterUrl}" src="${video.playbackUrl}"></video>
        </section>
        <section class="detail-panel meta-panel">
            <a class="btn btn-secondary" href="${video.downloadUrl}">원본 다운로드</a>
        </section>
        <section class="detail-panel comment-panel">
            <h2>댓글</h2>
            <ul class="comment-list">
                <c:forEach var="comment" items="${comments}">
                    <li class="comment-item"><div class="comment-head"><strong>${comment.authorName}</strong><span>${comment.createdAt}</span></div><p>${comment.content}</p></li>
                </c:forEach>
                <c:if test="${empty comments}"><li class="comment-empty">아직 댓글이 없습니다. 첫 댓글을 남겨보세요.</li></c:if>
            </ul>
            <form method="post" action="/feed/${video.batchId}/comments" class="comment-form">
                <textarea name="content" maxlength="500" placeholder="댓글을 입력하세요." required></textarea>
                <button type="submit" class="btn btn-primary">댓글 등록</button>
            </form>
        </section>
    </c:if>
</main>
<nav class="bottom-nav" aria-label="하단 메뉴">
    <a href="/feed" class="nav-item">피드</a>
    <a href="/search" class="nav-item">검색</a>
    <a href="/upload" class="nav-item">업로드</a>
    <a href="/likes" class="nav-item">좋아요</a>
    <a href="/mypage" class="nav-item">마이</a>
</nav>
</body></html>
