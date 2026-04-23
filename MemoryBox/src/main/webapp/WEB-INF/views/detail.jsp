<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>MemoryBox - 상세</title>
    <link rel="stylesheet" href="/css/common.css">
    <link rel="stylesheet" href="/css/detail.css">
    <link rel="stylesheet" href="/css/upload.css">
</head>
<body class="page page-detail">
<main class="detail-layout">
    <header class="detail-header">
        <a href="javascript:history.back()" class="back-link" aria-label="피드로 돌아가기">← 피드</a>
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
                <c:when test="${detail.mediaType eq 'VIDEO' and not empty detail.originalVideoUrl}">
                    <video controls playsinline preload="metadata" poster="${detail.videoThumbnailUrl}" class="detail-image">
                        <source src="${detail.originalVideoUrl}" type="video/mp4">
                    </video>
                </c:when>
                <c:when test="${not empty detail.displayImageUrl}">
                    <img src="${detail.displayImageUrl}" alt="${detail.title}" class="detail-image" loading="eager">
                </c:when>
                <c:otherwise>
                    <div class="image-empty">표시 가능한 이미지가 없습니다.</div>
                </c:otherwise>
            </c:choose>
        </article>

        <section class="detail-panel meta-panel">
            <h1 class="detail-title">${detail.title}</h1>
            <p class="meta-line">업로드 ${detail.relativeUploadedAt}</p>
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
                    <li><button type="button" class="tag-chip ${fn:startsWith(tag, '@') ? 'is-person' : ''}" aria-label="태그 ${tag}">${tag}</button></li>
                </c:forEach>
                <c:if test="${empty detail.tags}">
                    <li><span class="tag-chip is-empty">태그 없음</span></li>
                </c:if>
            </ul>

            <section class="meta-edit-panel">
                <c:choose>
                    <c:when test="${detail.editableByMe}">
                        <button type="button" class="btn btn-secondary" id="openOwnerEditBtn">수정</button>
                        <div id="ownerEditPanel" class="edit-panel-wrap" hidden>
                            <form method="post" action="/feed/${detail.mediaId}/edit-meta" class="meta-edit-form">
                                <h2>제목 / 앨범 수정</h2>
                                <label for="editTitle-${detail.mediaId}">제목</label>
                                <input id="editTitle-${detail.mediaId}" name="title" value="${detail.title}" maxlength="200">
                                <label for="editAlbum-${detail.mediaId}">앨범</label>
                                <select id="editAlbum-${detail.mediaId}" name="albumId" required>
                                    <c:forEach var="album" items="${albums}">
                                        <option value="${album.albumId}" ${album.albumId == detail.albumId ? 'selected' : ''}>${album.albumName}</option>
                                    </c:forEach>
                                </select>
                                <button type="submit" class="btn btn-secondary">제목/앨범 저장</button>
                            </form>

                            <form method="post" action="/feed/${detail.mediaId}/edit-tags" class="meta-edit-form">
                                <h2>태그 수정</h2>
                                <section class="tag-widget" data-widget="tag-picker" data-create-url="/upload/tag">
                                    <div class="tag-widget-header">
                                        <h2>태그 목록</h2>
                                        <span class="tag-count">선택 0개</span>
                                    </div>
                                    <div class="tag-title-row">
                                        <small>태그를 선택하거나 새 태그를 추가하세요.</small>
                                    </div>
                                    <div class="tag-option-list">
                                        <c:forEach var="tagOption" items="${tags}">
                                            <label class="tag-option ${tagOption.tagScope == 'P' ? 'is-person' : ''}" data-normalized="${tagOption.normalizedName}" data-scope="${tagOption.tagScope}">
                                                <input type="checkbox"
                                                       name="selectedTagIds"
                                                       value="${tagOption.tagId}"
                                                       class="tag-check"
                                                       ${fn:contains(fn:join(detail.tags, ','), tagOption.tagName) ? 'checked' : ''}>
                                                <span class="tag-label">${tagOption.tagScope == 'P' ? '@' : '#'}${tagOption.tagName}</span>
                                            </label>
                                        </c:forEach>
                                    </div>
                                    <div class="tag-add-row">
                                        <input type="text" class="tag-add-input" placeholder="새 태그 입력 후 추가 버튼">
                                        <button type="button" class="tag-add-btn">추가</button>
                                    </div>
                                    <input type="hidden" name="newTags" class="new-tags-hidden" value="">
                                </section>
                                <button type="submit" class="btn btn-secondary">태그 저장</button>
                            </form>
                        </div>
                    </c:when>
                    <c:otherwise>
                        <button type="button" class="btn btn-secondary" id="openTagEditBtn">태그 수정</button>
                        <div id="tagEditPanel" class="edit-panel-wrap" hidden>
                            <form method="post" action="/feed/${detail.mediaId}/edit-tags" class="meta-edit-form">
                                <section class="tag-widget" data-widget="tag-picker" data-create-url="/upload/tag">
                                    <div class="tag-widget-header">
                                        <h2>태그 목록</h2>
                                        <span class="tag-count">선택 0개</span>
                                    </div>
                                    <div class="tag-title-row">
                                        <small>태그를 선택하거나 새 태그를 추가하세요.</small>
                                    </div>
                                    <div class="tag-option-list">
                                        <c:forEach var="tagOption" items="${tags}">
                                            <label class="tag-option ${tagOption.tagScope == 'P' ? 'is-person' : ''}" data-normalized="${tagOption.normalizedName}" data-scope="${tagOption.tagScope}">
                                                <input type="checkbox"
                                                       name="selectedTagIds"
                                                       value="${tagOption.tagId}"
                                                       class="tag-check"
                                                       ${fn:contains(fn:join(detail.tags, ','), tagOption.tagName) ? 'checked' : ''}>
                                                <span class="tag-label">${tagOption.tagScope == 'P' ? '@' : '#'}${tagOption.tagName}</span>
                                            </label>
                                        </c:forEach>
                                    </div>
                                    <div class="tag-add-row">
                                        <input type="text" class="tag-add-input" placeholder="새 태그 입력 후 추가 버튼">
                                        <button type="button" class="tag-add-btn">추가</button>
                                    </div>
                                    <input type="hidden" name="newTags" class="new-tags-hidden" value="">
                                </section>
                                <button type="submit" class="btn btn-secondary">태그 저장</button>
                            </form>
                        </div>
                    </c:otherwise>
                </c:choose>
            </section>
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
                    <li class="comment-item" id="comment-${comment.commentId}">
                        <div class="comment-head">
                            <strong>${comment.authorName}</strong>
                            <span>${comment.createdAt}</span>
                        </div>
                        <p>${comment.content}</p>

                        <c:if test="${not empty comment.replies}">
                            <ul class="reply-list">
                                <c:forEach var="reply" items="${comment.replies}">
                                    <li class="reply-item" id="comment-${reply.commentId}">
                                        <div class="comment-head">
                                            <strong>${reply.authorName}</strong>
                                            <span>${reply.createdAt}</span>
                                        </div>
                                        <p>${reply.content}</p>
                                    </li>
                                </c:forEach>
                            </ul>
                        </c:if>

                        <div class="reply-actions">
                            <button type="button"
                                    class="btn btn-text reply-toggle-btn"
                                    data-target="replyForm-${comment.commentId}">
                                답글 달기
                            </button>
                        </div>

                        <div id="replyForm-${comment.commentId}" class="reply-write-wrap" hidden>
                            <form method="post" action="/feed/${detail.mediaId}/comments" class="reply-form">
                                <input type="hidden" name="parentId" value="${comment.commentId}">
                                <label for="reply-${comment.commentId}" class="sr-only">대댓글 작성</label>
                                <textarea id="reply-${comment.commentId}" name="content" maxlength="3000" placeholder="답글 달기" required></textarea>
                                <button type="submit" class="btn btn-secondary reply-submit">답글 등록</button>
                            </form>
                        </div>
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
<script>
    document.querySelectorAll('.reply-toggle-btn').forEach((button) => {
        button.addEventListener('click', () => {
            const targetId = button.dataset.target;
            const target = document.getElementById(targetId);
            if (!target) {
                return;
            }

            const nextHidden = !target.hidden;
            target.hidden = nextHidden;
            button.textContent = nextHidden ? '답글 달기' : '답글 작성 닫기';

            if (!nextHidden) {
                const textarea = target.querySelector('textarea');
                if (textarea) {
                    textarea.focus();
                }
            }
        });
    });

    const openOwnerEditBtn = document.getElementById('openOwnerEditBtn');
    const ownerEditPanel = document.getElementById('ownerEditPanel');
    openOwnerEditBtn?.addEventListener('click', () => {
        ownerEditPanel.hidden = !ownerEditPanel.hidden;
    });

    const openTagEditBtn = document.getElementById('openTagEditBtn');
    const tagEditPanel = document.getElementById('tagEditPanel');
    openTagEditBtn?.addEventListener('click', () => {
        tagEditPanel.hidden = !tagEditPanel.hidden;
    });

</script>
<script src="/js/upload.js"></script>
</body>
</html>
