<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>MemoryBox - 상세</title>
    <link rel="stylesheet" href="/css/common.css">
    <link rel="stylesheet" href="/css/detail.css">
</head>
<body class="page page-detail">
<main class="detail-layout">
    <header class="detail-header">
        <a href="/feed" class="back-link" aria-label="피드로 돌아가기">← 피드</a>
        <div class="login-chip">${loginUser.displayName}</div>
    </header>

    <c:if test="${notFound}">
        <section class="detail-panel state-panel">
            <h1>게시물을 찾을 수 없습니다</h1>
            <p>삭제되었거나 존재하지 않는 항목입니다.</p>
            <a href="/feed" class="btn btn-primary">피드 목록으로 이동</a>
        </section>
    </c:if>

    <c:if test="${not notFound}">
        <c:if test="${not empty info}">
            <div class="feedback-banner is-info">${info}</div>
        </c:if>
        <c:if test="${not empty error}">
            <div class="feedback-banner is-error">${error}</div>
        </c:if>

        <article class="detail-panel media-panel">
            <c:choose>
                <c:when test="${not empty detail.displayImageUrl}">
                    <img src="${detail.displayImageUrl}" alt="${detail.title}" class="detail-image" loading="eager">
                    <span class="variant-chip">${detail.displayVariantType}</span>
                </c:when>
                <c:otherwise>
                    <div class="image-empty">표시 가능한 이미지가 없습니다.</div>
                </c:otherwise>
            </c:choose>
        </article>

        <section class="detail-panel meta-panel">
            <h1 class="detail-title">${detail.title}</h1>
            <p class="meta-line">업로드 ${detail.uploadedAt}</p>
            <c:if test="${not empty detail.takenAt}">
                <p class="meta-line">촬영 ${detail.takenAt}</p>
            </c:if>
            <div class="meta-grid">
                <div><span>작성자</span><strong>${detail.authorName}</strong></div>
                <div><span>앨범</span><strong class="chip-like">${detail.albumName}</strong></div>
                <div><span>타입</span><strong>${detail.mediaType}</strong></div>
                <div><span>댓글</span><strong>${detail.commentCount}</strong></div>
            </div>
            <ul class="tag-chips">
                <c:forEach var="tag" items="${detail.tags}">
                    <li><button type="button" class="tag-chip" aria-label="태그 ${tag}">#${tag}</button></li>
                </c:forEach>
                <c:if test="${empty detail.tags}">
                    <li><span class="tag-chip is-empty">태그 없음</span></li>
                </c:if>
            </ul>
        </section>

        <section class="detail-panel action-panel">
            <form method="post" action="/feed/${detail.mediaId}/like" class="inline-form">
                <input type="hidden" name="action" value="${detail.likedByMe ? 'unlike' : 'like'}">
                <button type="submit" class="btn like-btn ${detail.likedByMe ? 'is-liked' : ''}">
                    ${detail.likedByMe ? '❤ 좋아요 취소' : '♡ 좋아요'}
                    <span>${detail.likeCount}</span>
                </button>
            </form>

            <c:choose>
                <c:when test="${detail.downloadable}">
                    <a href="${detail.downloadUrl}" class="btn btn-secondary download-btn">원본 다운로드</a>
                </c:when>
                <c:otherwise>
                    <button type="button" class="btn btn-secondary download-btn" disabled>원본 없음</button>
                </c:otherwise>
            </c:choose>
        </section>

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
                    <li class="comment-empty">첫 댓글을 남겨보세요.</li>
                </c:if>
            </ul>

            <form method="post" action="/feed/${detail.mediaId}/comments" class="comment-form">
                <label for="commentContent" class="sr-only">댓글 작성</label>
                <textarea id="commentContent" name="content" maxlength="3000" placeholder="댓글을 입력해 주세요" required></textarea>
                <button type="submit" class="btn btn-primary">댓글 등록</button>
            </form>
        </section>
    </c:if>
</main>
</body>
</html>
