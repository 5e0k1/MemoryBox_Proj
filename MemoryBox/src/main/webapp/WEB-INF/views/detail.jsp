<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>MemoryBox - 상세</title>
    <%@ include file="/WEB-INF/views/common/head-icons.jspf" %>
    <link rel="stylesheet" href="/css/common.css">
    <link rel="stylesheet" href="/css/detail.css">
    <link rel="stylesheet" href="/css/upload.css">
</head>
<body class="page page-detail">
<c:set var="shareTitleValue" value="${not empty detail ? detail.title : ''}" />
<c:set var="shareImageValue" value="${not empty detail ? detail.shareImageUrl : ''}" />
<main class="detail-layout"
      id="detailLayout"
      data-media-id="${currentMediaId}"
      data-share-title="${fn:escapeXml(shareTitleValue)}"
      data-share-image-url="${fn:escapeXml(shareImageValue)}"
      data-share-media-type="${not empty detail ? detail.mediaType : ''}"
      data-share-base-url="${fn:escapeXml(shareBaseUrl)}">
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
            <div class="title-row">
                <h1 class="detail-title">${detail.title}</h1>
                <button type="button"
                        class="share-open-btn"
                        id="openShareModalBtn"
                        data-media-id="${currentMediaId}"
                        aria-label="공유하기">🔗</button>
            </div>
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

<div class="share-modal" id="shareModal" hidden>
    <div class="share-modal-backdrop" id="shareModalBackdrop"></div>
    <section class="share-modal-panel" role="dialog" aria-modal="true" aria-labelledby="shareModalTitle">
        <header class="share-modal-header">
            <h2 id="shareModalTitle">공유하기</h2>
            <button type="button" class="share-close-btn" id="closeShareModalBtn" aria-label="닫기">✕</button>
        </header>

        <form class="share-form" id="shareForm">
            <input type="hidden" id="shareMediaId" value="${currentMediaId}">
            <label class="share-option-row">
                <input type="radio" name="shareType" value="member" checked>
                <span>회원끼리 공유</span>
            </label>
            <label class="share-option-row">
                <input type="radio" name="shareType" value="guest">
                <span>게스트 공유</span>
            </label>

            <section id="guestOptionWrap" class="guest-option-wrap" hidden>
                <h3>게스트 옵션</h3>
                <label class="share-option-row">
                    <input type="checkbox" id="allowComments">
                    <span>댓글 보기 허용</span>
                </label>
                <label class="share-option-row">
                    <input type="checkbox" id="allowDownload">
                    <span>다운로드 허용</span>
                </label>
                <p class="share-expire-notice">게스트 공유 링크는 생성 시점부터 1시간 동안만 접속 가능합니다.</p>
            </section>

            <div class="share-action-row">
                <button type="submit" class="btn btn-primary">링크 생성</button>
                <button type="button" class="btn btn-secondary" id="copyShareUrlBtn" disabled>링크 복사</button>
                <button type="button" class="kakao-share-btn" id="kakaoShareBtn" disabled aria-label="카카오톡 공유">
                    <img src="/images/kakaotalk_sharing_btn_medium.png" alt="카카오톡 공유하기">
                </button>
            </div>
            <input type="text" id="shareUrlOutput" class="share-url-output" readonly placeholder="생성된 공유 링크가 여기에 표시됩니다.">
            <p id="shareFeedback" class="share-feedback" aria-live="polite"></p>
        </form>
    </section>
</div>
<script src="https://developers.kakao.com/sdk/js/kakao.min.js"></script>
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

    const FEED_URL = '/feed';
    window.addEventListener('pageshow', (event) => {
        const navigationEntries = performance.getEntriesByType ? performance.getEntriesByType('navigation') : [];
        const navigationType = navigationEntries.length > 0 ? navigationEntries[0].type : '';
        if (event.persisted || navigationType === 'back_forward') {
            window.location.replace(FEED_URL);
        }
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

    const singleDownloadBtn = document.querySelector('a.download-btn');
    let singleDownloadTraceActive = false;
    singleDownloadBtn?.addEventListener('click', (event) => {
        event.stopPropagation();
        console.debug('[download-debug] single-download-click');
        const downloadUrl = singleDownloadBtn.getAttribute('href');
        console.debug('[download-debug] single-download-url-ready', { downloadUrl });
        singleDownloadTraceActive = true;
        console.debug('[download-debug] single-download-trigger');
    });

    window.addEventListener('beforeunload', () => {
        if (!singleDownloadTraceActive) return;
        console.debug('[download-debug] single-download-after-beforeunload');
    });
    window.addEventListener('pagehide', () => {
        if (!singleDownloadTraceActive) return;
        console.debug('[download-debug] single-download-after-pagehide');
    });
    window.addEventListener('popstate', () => {
        if (!singleDownloadTraceActive) return;
        console.debug('[download-debug] single-download-after-popstate');
    });
    document.addEventListener('visibilitychange', () => {
        if (!singleDownloadTraceActive) return;
        console.debug('[download-debug] single-download-after-visibilitychange', { visibilityState: document.visibilityState });
    });
    document.addEventListener('submit', (event) => {
        if (!singleDownloadTraceActive) return;
        const form = event.target;
        if (!(form instanceof HTMLFormElement)) return;
        console.debug('[download-debug] single-download-after-form-submit', { action: form.action, method: form.method });
    }, true);
    document.addEventListener('click', (event) => {
        if (!singleDownloadTraceActive) return;
        const anchor = event.target.closest('a[href]');
        if (anchor && anchor !== singleDownloadBtn) {
            console.debug('[download-debug] single-download-after-anchor-click', { href: anchor.getAttribute('href') });
        }
    }, true);

    const shareModal = document.getElementById('shareModal');
    const openShareModalBtn = document.getElementById('openShareModalBtn');
    const closeShareModalBtn = document.getElementById('closeShareModalBtn');
    const shareModalBackdrop = document.getElementById('shareModalBackdrop');
    const shareForm = document.getElementById('shareForm');
    const guestOptionWrap = document.getElementById('guestOptionWrap');
    const allowCommentsInput = document.getElementById('allowComments');
    const allowDownloadInput = document.getElementById('allowDownload');
    const shareUrlOutput = document.getElementById('shareUrlOutput');
    const copyShareUrlBtn = document.getElementById('copyShareUrlBtn');
    const kakaoShareBtn = document.getElementById('kakaoShareBtn');
    const shareFeedback = document.getElementById('shareFeedback');
    const shareMediaId = document.getElementById('shareMediaId');
    const detailLayout = document.getElementById('detailLayout');
    const kakaoJavascriptKey = '${kakaoJavascriptKey}';
    const shareBaseUrl = (detailLayout?.dataset?.shareBaseUrl || '').trim();
    const shareTitle = (detailLayout?.dataset?.shareTitle || '').trim();
    const shareImageFromData = (detailLayout?.dataset?.shareImageUrl || '').trim();
    const fallbackImageUrl = '/images/default-image.png';
    let currentShareUrl = '';
    let currentShareType = 'member';
    const kakaoReady = (() => {
        if (!window.Kakao) {
            console.error('[share-kakao] Kakao SDK not loaded.');
            return false;
        }
        if (!kakaoJavascriptKey) {
            return false;
        }
        try {
            if (!window.Kakao.isInitialized()) {
                window.Kakao.init(kakaoJavascriptKey);
            }
            return true;
        } catch (error) {
            console.error('[share-kakao] Kakao.init failed', error);
            return false;
        }
    })();

    const toAbsoluteUrl = (value) => {
        if (!value) return '';
        if (/^https?:\/\//i.test(value)) {
            return value;
        }
        if (value.startsWith('//')) {
            return window.location.protocol + value;
        }
        const normalizedBase = (shareBaseUrl || window.location.origin).replace(/\/$/, '');
        const normalizedPath = value.startsWith('/') ? value : '/' + value;
        return normalizedBase + normalizedPath;
    };

    const isCloudFrontUrl = (value) => /^https?:\/\/[^/]*cloudfront\.net\//i.test(value || '');
    const isLocalhostUrl = (value) => /^https?:\/\/(localhost|127\.0\.0\.1)(:\d+)?\//i.test(value || '');

    const toSmallVariantUrl = (value) => {
        if (!value) return '';
        const absoluteUrl = toAbsoluteUrl(value);
        if (absoluteUrl.includes('/small/')) {
            return absoluteUrl;
        }
        if (isCloudFrontUrl(absoluteUrl) && absoluteUrl.includes('/medium/')) {
            return absoluteUrl.replace('/medium/', '/small/');
        }
        return '';
    };

    const resolveShareImageUrl = () => {
        const smallUrl = toSmallVariantUrl(shareImageFromData);
        const absoluteFallbackImageUrl = toAbsoluteUrl(fallbackImageUrl);
        const shareImageUrl = (!smallUrl || isLocalhostUrl(smallUrl))
                ? absoluteFallbackImageUrl
                : smallUrl;
        console.log('[share-kakao] image candidates', {
            shareImageFromData: shareImageFromData,
            smallUrl: smallUrl,
            fallbackImageUrl: absoluteFallbackImageUrl,
            shareImageUrl: shareImageUrl
        });
        return shareImageUrl;
    };

    const updateGuestOptionVisibility = () => {
        const selected = shareForm?.querySelector('input[name="shareType"]:checked');
        const isGuest = selected?.value === 'guest';
        currentShareType = isGuest ? 'guest' : 'member';
        if (guestOptionWrap) {
            guestOptionWrap.hidden = !isGuest;
        }
        if (!isGuest) {
            allowCommentsInput.checked = false;
            allowDownloadInput.checked = false;
        }
    };

    const openShareModal = () => {
        if (!shareModal) return;
        shareModal.hidden = false;
        updateGuestOptionVisibility();
        if (!kakaoJavascriptKey) {
            kakaoShareBtn.disabled = true;
            shareFeedback.textContent = '카카오 공유 설정이 필요합니다.';
        }
    };

    const closeShareModal = () => {
        if (!shareModal) return;
        shareModal.hidden = true;
    };

    openShareModalBtn?.addEventListener('click', openShareModal);
    closeShareModalBtn?.addEventListener('click', closeShareModal);
    shareModalBackdrop?.addEventListener('click', closeShareModal);
    shareForm?.querySelectorAll('input[name="shareType"]').forEach((radio) => {
        radio.addEventListener('change', updateGuestOptionVisibility);
    });

    shareForm?.addEventListener('submit', async (event) => {
        event.preventDefault();
        shareFeedback.textContent = '';
        copyShareUrlBtn.disabled = true;
        kakaoShareBtn.disabled = true;
        shareUrlOutput.value = '';
        currentShareUrl = '';

        const selected = shareForm.querySelector('input[name="shareType"]:checked');
        const isGuest = selected?.value === 'guest';
        const serverRenderedMediaId = '${currentMediaId}';
        const mediaId = (
            openShareModalBtn?.dataset?.mediaId
            || detailLayout?.dataset?.mediaId
            || shareMediaId?.value
            || serverRenderedMediaId
            || ''
        ).trim();
        if (!mediaId) {
            shareFeedback.textContent = '공유할 게시글 정보를 찾을 수 없습니다.';
            return;
        }

        try {
            const response = await fetch('/share/batch/' + mediaId, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({
                    guest: isGuest,
                    allowComments: isGuest ? allowCommentsInput.checked : false,
                    allowDownload: isGuest ? allowDownloadInput.checked : false,
                    expiresMinutes: isGuest ? 60 : null
                })
            });

            const data = await response.json();
            if (!response.ok) {
                shareFeedback.textContent = data.message || '공유 링크 생성에 실패했습니다.';
                return;
            }

            const memberUrl = toAbsoluteUrl(data.memberUrl || '');
            const guestUrl = toAbsoluteUrl(data.guestUrl || '');
            const selectedShareUrl = isGuest ? guestUrl : memberUrl;
            shareUrlOutput.value = selectedShareUrl || '';
            currentShareUrl = selectedShareUrl || '';
            currentShareType = isGuest ? 'guest' : 'member';
            copyShareUrlBtn.disabled = !selectedShareUrl;
            kakaoShareBtn.disabled = !selectedShareUrl || !kakaoReady;
            console.log('[share-kakao] share link response', {
                memberUrl: memberUrl,
                guestUrl: guestUrl,
                selectedShareUrl: selectedShareUrl
            });
            shareFeedback.textContent = isGuest
                    ? '게스트 공유 링크가 생성되었습니다.'
                    : '회원 공유 링크가 준비되었습니다.';
        } catch (error) {
            shareFeedback.textContent = '공유 링크 생성 중 오류가 발생했습니다.';
        }
    });

    copyShareUrlBtn?.addEventListener('click', async () => {
        const value = shareUrlOutput.value;
        if (!value) return;
        try {
            await navigator.clipboard.writeText(value);
            shareFeedback.textContent = '링크가 복사되었습니다.';
        } catch (error) {
            shareFeedback.textContent = '복사에 실패했습니다. 링크를 직접 복사해 주세요.';
        }
    });

    kakaoShareBtn?.addEventListener('click', () => {
        if (!currentShareUrl) {
            shareFeedback.textContent = '먼저 공유 링크를 생성해 주세요.';
            return;
        }
        if (!window.Kakao) {
            console.error('[share-kakao] Kakao SDK not loaded.');
            shareFeedback.textContent = '카카오 SDK를 불러오지 못했습니다.';
            return;
        }
        if (!kakaoJavascriptKey || !kakaoReady) {
            console.error('[share-kakao] javascript key missing or Kakao not initialized.');
            shareFeedback.textContent = '카카오 공유 설정이 필요합니다.';
            return;
        }

        const selectedShareUrl = toAbsoluteUrl(currentShareUrl);
        if (!selectedShareUrl) {
            shareFeedback.textContent = '먼저 공유 링크를 생성해 주세요.';
            return;
        }

        const resolvedTitle = shareTitle || 'MemoryBox 공유 사진';
        const description = currentShareType === 'guest'
                ? '공유된 사진/영상을 확인해보세요.'
                : 'MemoryBox에서 사진/영상을 확인해보세요.';
        const shareImageUrl = resolveShareImageUrl();
        console.log('[share-kakao] send payload', {
            selectedShareUrl: selectedShareUrl,
            shareImageUrl: shareImageUrl
        });

        try {
            window.Kakao.Share.sendDefault({
                objectType: 'feed',
                content: {
                    title: resolvedTitle,
                    description: description,
                    imageUrl: shareImageUrl,
                    link: {
                        mobileWebUrl: selectedShareUrl,
                        webUrl: selectedShareUrl
                    }
                },
                buttons: [
                    {
                        title: '보러가기',
                        link: {
                            mobileWebUrl: selectedShareUrl,
                            webUrl: selectedShareUrl
                        }
                    }
                ]
            });
        } catch (error) {
            console.error('[share-kakao] sendDefault failed', error);
            shareFeedback.textContent = '카카오 공유 중 오류가 발생했습니다.';
        }
    });

</script>
<script src="/js/upload.js"></script>
</body>
</html>
