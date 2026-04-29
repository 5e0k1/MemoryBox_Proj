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
    <link rel="stylesheet" href="/css/sweetalert2/sweetalert2.min.css">
</head>
<body class="page page-detail">
<main class="detail-layout" id="detailLayout" data-batch-id="${currentBatchId}">
    <header class="detail-header">
        <a href="/feed" class="back-link" aria-label="피드로 돌아가기">← 피드</a>
        <div class="login-chip">${loginUser.displayName}</div>
    </header>

    <c:if test="${notFound}">
        <section class="detail-panel state-panel">
            <h1>게시물을 찾을 수 없습니다</h1>
            <a href="/feed" class="btn btn-primary">피드로 이동</a>
        </section>
    </c:if>

    <c:if test="${not notFound}">
        <c:if test="${not empty info}">
            <section class="detail-panel feedback-banner is-info">${info}</section>
        </c:if>
        <c:if test="${not empty error}">
            <section class="detail-panel feedback-banner is-error">${error}</section>
        </c:if>

        <section class="detail-panel meta-panel">
            <div class="title-row">
                <h1 class="detail-title">${detail.title}</h1>
                <div class="meta-action-buttons">
                    <button type="button" class="share-open-btn" id="shareOpenBtn" aria-label="공유 열기">
                        <img src="/images/share-btn-img.png" alt="공유하기" width="20" height="20">
                    </button>
                    <button type="button" class="btn btn-secondary" id="downloadAllBtn"
                            data-batch-id="${currentBatchId}">전체 다운로드</button>
                </div>
            </div>
            <p class="meta-line">작성자 ${detail.authorName}</p>
            <p class="meta-line">업로드 ${detail.relativeUploadedAt}</p>
            <p class="meta-line">앨범 ${detail.albumName} · ${detail.commentCount} 댓글</p>
            <div class="engagement-row">
                <form action="/feed/${currentBatchId}/like" method="post" class="inline-form">
                    <input type="hidden" name="action" value="${detail.likedByMe ? 'unlike' : 'like'}">
                    <button type="submit" class="btn btn-secondary like-btn ${detail.likedByMe ? 'is-liked' : ''}">
                        ${detail.likedByMe ? '❤ 좋아요 취소' : '♡ 좋아요'} · ${detail.likeCount}
                    </button>
                </form>
            </div>
        </section>

        <section class="detail-panel" id="batchGridSection">
            <div class="batch-grid" id="batchGrid" data-total-count="${fn:length(detailItems)}">
                <c:forEach var="item" items="${detailItems}" varStatus="status">
                    <button type="button" class="grid-item"
                            data-media-id="${item.mediaId}"
                            data-index="${status.index}"
                            data-sort-order="${item.sortOrder}"
                            data-media-type="${item.mediaType}"
                            data-small-url="${fn:escapeXml(item.smallUrl)}"
                            data-medium-url="${fn:escapeXml(item.mediumUrl)}"
                            data-preview-url="${fn:escapeXml(item.previewUrl)}"
                            data-download-url="${fn:escapeXml(item.downloadUrl)}">
                        <c:choose>
                            <c:when test="${item.mediaType eq 'VIDEO'}">
                                <video src="${item.previewUrl}" muted playsinline preload="metadata"></video>
                            </c:when>
                            <c:otherwise>
                                <img src="${item.smallUrl}" alt="thumb-${status.index}" loading="lazy"/>
                            </c:otherwise>
                        </c:choose>
                        <span class="grid-check">✔</span>
                    </button>
                </c:forEach>
            </div>
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
                        <div class="reply-actions">
                            <button type="button" class="btn btn-secondary btn-sm reply-toggle-btn" data-comment-id="${comment.commentId}">답글</button>
                        </div>
                        <div class="reply-write-wrap" id="reply-wrap-${comment.commentId}" hidden>
                            <form method="post" action="/feed/${currentBatchId}/comments" class="reply-form">
                                <input type="hidden" name="parentId" value="${comment.commentId}">
                                <textarea name="content" maxlength="500" placeholder="답글을 입력하세요." required></textarea>
                                <button type="submit" class="btn btn-primary reply-submit">답글 등록</button>
                            </form>
                        </div>
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
                    </li>
                </c:forEach>
                <c:if test="${empty comments}">
                    <li class="comment-empty">아직 댓글이 없습니다. 첫 댓글을 남겨보세요.</li>
                </c:if>
            </ul>
            <form method="post" action="/feed/${currentBatchId}/comments" class="comment-form">
                <textarea name="content" maxlength="500" placeholder="댓글을 입력하세요." required></textarea>
                <button type="submit" class="btn btn-primary">댓글 등록</button>
            </form>
        </section>
    </c:if>
</main>

<div class="share-modal" id="shareModal" hidden>
    <div class="share-modal-backdrop" id="shareBackdrop"></div>
    <section class="share-modal-panel" role="dialog" aria-modal="true" aria-labelledby="shareModalTitle">
        <div class="share-modal-header">
            <h2 id="shareModalTitle">게시물 공유</h2>
            <button type="button" class="share-close-btn" id="shareCloseBtn" aria-label="공유 모달 닫기">×</button>
        </div>
        <form class="share-form" id="shareForm">
            <label class="share-option-row">
                <input type="radio" name="shareScope" value="member" checked>
                <span>회원 전용 링크</span>
            </label>
            <label class="share-option-row">
                <input type="radio" name="shareScope" value="guest">
                <span>게스트 링크</span>
            </label>
            <div class="guest-option-wrap" id="guestOptionWrap" hidden>
                <h3>게스트 권한</h3>
                <label class="share-option-row">
                    <input type="checkbox" name="allowComments" id="allowCommentsChk">
                    <span>댓글 보기 허용</span>
                </label>
                <label class="share-option-row">
                    <input type="checkbox" name="allowDownload" id="allowDownloadChk">
                    <span>다운로드 허용</span>
                </label>
            </div>
            <div class="share-action-row">
                <button type="submit" class="btn btn-primary" id="shareCreateBtn">링크 생성</button>
                <button type="button" class="btn btn-secondary" id="shareCopyBtn" disabled>복사</button>
                <button type="button" class="kakao-share-btn" id="shareKakaoBtn" disabled aria-label="카카오톡으로 공유">
                    <img src="/images/kakaotalk_sharing_btn_medium.png" alt="카카오톡 공유">
                </button>
            </div>
            <input type="text" class="share-url-output" id="shareUrlOutput" readonly placeholder="생성된 링크가 여기에 표시됩니다.">
            <p class="share-feedback" id="shareFeedback" aria-live="polite"></p>
        </form>
    </section>
</div>

<div class="selection-bar" id="selectionBar" hidden>
    <span><strong id="selectedCount">0</strong>개 선택</span>
    <div>
        <button type="button" class="btn btn-secondary" id="cancelSelectBtn">취소</button>
        <button type="button" class="btn btn-secondary" id="downloadSelectBtn">선택 다운로드</button>
    </div>
</div>

<script src="/js/sweetalert2/sweetalert2.all.min.js"></script>
<link rel="stylesheet" href="/css/photoswipe/photoswipe.css">
<script src="/js/photoswipe/photoswipe.umd.min.js"></script>
<script src="/js/photoswipe/photoswipe-lightbox.umd.min.js"></script>
<script src="https://t1.kakaocdn.net/kakao_js_sdk/2.8.1/kakao.min.js"
        integrity="sha384-OL+ylM/iuPLtW5U3XcvLSGhE8JzReKDank5InqlHGWPhb4140/yrBw0bg0y7+C9J"
        crossorigin="anonymous"></script>
<script>
(() => {
    const grid = document.getElementById('batchGrid');
    if (!grid) return;
    const items = Array.from(grid.querySelectorAll('.grid-item'));
    const totalItems = items.length || 1;
    const mediaEntries = items.map((button) => ({
        mediaId: button.dataset.mediaId,
        sortOrder: Number(button.dataset.sortOrder || Number.MAX_SAFE_INTEGER),
        mediaType: button.dataset.mediaType,
        smallUrl: button.dataset.smallUrl,
        mediumUrl: button.dataset.mediumUrl,
        previewUrl: button.dataset.previewUrl,
        downloadUrl: button.dataset.downloadUrl,
        thumbUrl: button.querySelector('img, video')?.getAttribute('src') || ''
    }));
    const toAbsoluteUrl = (url) => {
        if (!url) return '';
        if (/^https?:\/\//i.test(url)) return url;
        return window.location.origin + (url.startsWith('/') ? url : '/' + url);
    };
    const selectionBar = document.getElementById('selectionBar');
    const selectedCount = document.getElementById('selectedCount');
    const cancelSelectBtn = document.getElementById('cancelSelectBtn');
    const downloadSelectBtn = document.getElementById('downloadSelectBtn');
    const downloadAllBtn = document.getElementById('downloadAllBtn');
    const shareOpenBtn = document.getElementById('shareOpenBtn');
    const shareModal = document.getElementById('shareModal');
    const shareBackdrop = document.getElementById('shareBackdrop');
    const shareCloseBtn = document.getElementById('shareCloseBtn');
    const shareForm = document.getElementById('shareForm');
    const guestOptionWrap = document.getElementById('guestOptionWrap');
    const shareCreateBtn = document.getElementById('shareCreateBtn');
    const shareCopyBtn = document.getElementById('shareCopyBtn');
    const shareKakaoBtn = document.getElementById('shareKakaoBtn');
    const shareUrlOutput = document.getElementById('shareUrlOutput');
    const shareFeedback = document.getElementById('shareFeedback');
    const kakaoJavascriptKey = '<c:out value="${kakaoJavascriptKey}" />';

    if (!selectionBar || !selectedCount || !cancelSelectBtn || !downloadSelectBtn) return;

    let selectionMode = false;
    const selected = new Set();
    let pswpInstance = null;

    const getItemData = (index) => mediaEntries[index];

    const openViewer = (index) => {
        const imageItems = mediaEntries
            .filter((entry) => entry.mediaType !== 'VIDEO')
            .map((entry) => ({ src: toAbsoluteUrl(entry.mediumUrl || entry.previewUrl || entry.smallUrl), width: 1600, height: 1200 }));
        const clicked = getItemData(index);
        if (!clicked || clicked.mediaType === 'VIDEO' || imageItems.length === 0 || !window.PhotoSwipe) return;
        const imageIndex = mediaEntries.filter((entry, i) => i <= index && entry.mediaType !== 'VIDEO').length - 1;
        pswpInstance = new window.PhotoSwipe({
            dataSource: imageItems,
            index: Math.max(0, imageIndex),
            pswpModule: window.PhotoSwipe,
            wheelToZoom: true,
            bgOpacity: 0.9
        });
        pswpInstance.init();
    };

    const syncSelectionUi = () => {
        selectedCount.textContent = String(selected.size);
        selectionBar.hidden = !selectionMode;
        items.forEach((btn) => btn.classList.toggle('is-selected', selected.has(btn.dataset.mediaId)));
    };

    const toggleSelection = (btn) => {
        const id = btn.dataset.mediaId;
        if (selected.has(id)) selected.delete(id); else selected.add(id);
        if (selected.size === 0) selectionMode = false;
        syncSelectionUi();
    };

    items.forEach((btn, index) => {
        let timer = null;
        let longPressTriggered = false;
        let suppressContextMenuUntil = 0;
        btn.addEventListener('touchstart', () => {
            longPressTriggered = false;
            timer = setTimeout(() => {
                longPressTriggered = true;
                suppressContextMenuUntil = Date.now() + 900;
                selectionMode = true;
                toggleSelection(btn);
            }, 500);
        }, {passive:true});
        btn.addEventListener('touchmove', () => { if (timer) clearTimeout(timer); }, {passive:true});
        btn.addEventListener('touchend', () => { if (timer) clearTimeout(timer); }, {passive:true});
        btn.addEventListener('contextmenu', (e) => {
            e.preventDefault();
            if (Date.now() < suppressContextMenuUntil) return;
            selectionMode = true;
            toggleSelection(btn);
        });
        btn.addEventListener('click', (e) => {
            if (longPressTriggered) {
                e.preventDefault();
                longPressTriggered = false;
                return;
            }
            if (selectionMode) { toggleSelection(btn); return; }
            openViewer(index);
        });
    });


    cancelSelectBtn.addEventListener('click', () => {
        selectionMode = false; selected.clear(); syncSelectionUi();
    });

    const wait = (ms) => new Promise((resolve) => window.setTimeout(resolve, ms));
    const triggerBrowserDownload = (url) => {
        if (!url) return;
        const a = document.createElement('a');
        a.href = url;
        a.download = '';
        a.rel = 'noopener';
        a.style.display = 'none';
        document.body.appendChild(a);
        a.click();
        window.setTimeout(() => a.remove(), 1000);
    };

    const withPreparingAlert = async (job) => {
        const startedAt = Date.now();
        if (window.Swal && typeof window.Swal.fire === 'function') {
            window.Swal.fire({
                title: '압축 파일 준비 중...',
                text: '파일을 묶는 중입니다. 잠시만 기다려주세요.',
                allowOutsideClick: false,
                allowEscapeKey: false,
                didOpen: () => window.Swal.showLoading()
            });
        }
        try {
            return await job();
        } finally {
            const elapsed = Date.now() - startedAt;
            if (elapsed < 300) {
                await wait(300 - elapsed);
            }
            if (window.Swal && typeof window.Swal.close === 'function') {
                window.Swal.close();
            }
        }
    };

    downloadSelectBtn.addEventListener('click', async () => {
        if (selected.size === 0) return;
        downloadSelectBtn.disabled = true;
        const mediaIds = Array.from(selected, (v) => Number(v));
        if (mediaIds.length === 1) {
            const target = items.find((i) => i.dataset.mediaId === String(mediaIds[0]));
            if (target) triggerBrowserDownload(target.dataset.downloadUrl);
            downloadSelectBtn.disabled = false;
            return;
        }
        try {
            const payload = await withPreparingAlert(async () => {
                const res = await fetch('/download/zip/prepare', {
                    method: 'POST', headers: {'Content-Type': 'application/json'},
                    body: JSON.stringify({mediaIds})
                });
                if (!res.ok) throw new Error('zip prepare failed');
                return res.json();
            });
            selectionMode = false;
            selected.clear();
            syncSelectionUi();
            triggerBrowserDownload(payload.downloadUrl);
        } catch (e) {
            alert('압축 파일 생성 실패');
        } finally {
            downloadSelectBtn.disabled = false;
        }
    });

    downloadAllBtn?.addEventListener('click', async () => {
        const batchId = Number(downloadAllBtn.dataset.batchId);
        if (!batchId) return;
        downloadAllBtn.disabled = true;
        try {
            const payload = await withPreparingAlert(async () => {
                const res = await fetch('/download/zip/prepare', {
                    method: 'POST',
                    headers: {'Content-Type': 'application/json'},
                    body: JSON.stringify({batchId})
                });
                if (!res.ok) {
                    const error = await res.json().catch(() => ({}));
                    throw new Error(error.message || 'ZIP 생성에 실패했습니다.');
                }
                return res.json();
            });
            triggerBrowserDownload(payload.downloadUrl);
        } catch (e) {
            alert(e.message || 'ZIP 생성에 실패했습니다.');
        } finally {
            downloadAllBtn.disabled = false;
        }
    });

    document.querySelectorAll('.reply-toggle-btn').forEach((btn) => {
        btn.addEventListener('click', () => {
            const commentId = btn.dataset.commentId;
            const wrap = document.getElementById('reply-wrap-' + commentId);
            if (!wrap) return;
            wrap.hidden = !wrap.hidden;
            if (!wrap.hidden) {
                const textarea = wrap.querySelector('textarea');
                if (textarea) textarea.focus();
            }
        });
    });

    const detailLayout = document.getElementById('detailLayout');
    const batchId = detailLayout?.dataset.batchId;
    const likeForm = document.querySelector('.engagement-row .inline-form');
    const commentForms = document.querySelectorAll('.comment-form, .reply-form');

    const refreshWithoutHistoryStack = () => {
        const currentUrl = window.location.pathname + window.location.search;
        window.location.replace(currentUrl);
    };

    likeForm?.addEventListener('submit', async (event) => {
        event.preventDefault();
        if (!batchId) return;
        const action = likeForm.querySelector('input[name="action"]')?.value || 'like';
        const params = new URLSearchParams();
        params.set('action', action);
        const response = await fetch(`/api/feed/${batchId}/like`, {
            method: 'POST',
            headers: {'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8'},
            body: params.toString()
        });
        if (!response.ok) {
            alert('좋아요 처리 중 오류가 발생했습니다.');
            return;
        }
        refreshWithoutHistoryStack();
    });

    commentForms.forEach((form) => {
        form.addEventListener('submit', async (event) => {
            event.preventDefault();
            if (!batchId) return;
            const textarea = form.querySelector('textarea[name="content"]');
            const content = (textarea?.value || '').trim();
            if (!content) {
                alert('댓글 내용을 입력해 주세요.');
                return;
            }
            const params = new URLSearchParams();
            params.set('content', content);
            const parentId = form.querySelector('input[name="parentId"]')?.value;
            if (parentId) params.set('parentId', parentId);

            const response = await fetch(`/api/feed/${batchId}/comments`, {
                method: 'POST',
                headers: {'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8'},
                body: params.toString()
            });
            if (!response.ok) {
                alert('댓글 처리 중 오류가 발생했습니다.');
                return;
            }
            refreshWithoutHistoryStack();
        });
    });

    if (shareOpenBtn && shareModal && shareForm && shareCloseBtn && shareBackdrop &&
        guestOptionWrap && shareCreateBtn && shareCopyBtn && shareKakaoBtn && shareUrlOutput && shareFeedback) {
        if (window.Kakao && kakaoJavascriptKey && !window.Kakao.isInitialized()) {

            window.Kakao.init(kakaoJavascriptKey);
        }
        const closeShareModal = () => {
            shareModal.hidden = true;
        };
        const openShareModal = () => {
            shareModal.hidden = false;
        };
        const updateGuestOptionVisibility = () => {
            const selectedScope = shareForm.querySelector('input[name="shareScope"]:checked')?.value;
            guestOptionWrap.hidden = selectedScope !== 'guest';
        };

        shareOpenBtn.addEventListener('click', openShareModal);
        shareCloseBtn.addEventListener('click', closeShareModal);
        shareBackdrop.addEventListener('click', closeShareModal);
        shareForm.querySelectorAll('input[name="shareScope"]').forEach((radio) => {
            radio.addEventListener('change', updateGuestOptionVisibility);
        });
        updateGuestOptionVisibility();

        shareForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            const selectedScope = shareForm.querySelector('input[name="shareScope"]:checked')?.value || 'member';
            const payload = {
                guest: selectedScope === 'guest',
                allowComments: !!document.getElementById('allowCommentsChk')?.checked,
                allowDownload: !!document.getElementById('allowDownloadChk')?.checked
            };
            shareCreateBtn.disabled = true;
            shareFeedback.textContent = '링크 생성 중...';
            try {
                const res = await fetch('/share/batch/' + grid.closest('main').dataset.batchId, {
                    method: 'POST',
                    headers: {'Content-Type': 'application/json'},
                    body: JSON.stringify(payload)
                });
                const body = await res.json();
                if (!res.ok) {
                    shareFeedback.textContent = body.message || '공유 링크 생성에 실패했습니다.';
                    return;
                }
                const link = payload.guest ? body.guestUrl : body.memberUrl;
                shareUrlOutput.value = link || '';
                shareCopyBtn.disabled = !link;
                shareKakaoBtn.disabled = !link;
                shareFeedback.textContent = link ? '공유 링크가 생성되었습니다.' : '링크 생성에 실패했습니다.';
            } catch (error) {
                shareFeedback.textContent = '공유 링크 생성 중 오류가 발생했습니다.';
            } finally {
                shareCreateBtn.disabled = false;
            }
        });

        shareCopyBtn.addEventListener('click', async () => {
            if (!shareUrlOutput.value) return;
            try {
                await navigator.clipboard.writeText(shareUrlOutput.value);
                shareFeedback.textContent = '링크를 복사했습니다.';
            } catch (error) {
                shareUrlOutput.select();
                document.execCommand('copy');
                shareFeedback.textContent = '링크를 복사했습니다.';
            }
        });

        shareKakaoBtn.addEventListener('click', () => {
            const link = shareUrlOutput.value?.trim();
            if (!link) return;
            if (!window.Kakao || !window.Kakao.Share) {
                shareFeedback.textContent = '카카오톡 공유를 사용할 수 없습니다. 링크를 복사해 사용해 주세요.';
                return;
            }
            const primaryMedia = mediaEntries.find((entry) => entry.sortOrder === 1)
                || mediaEntries.slice().sort((a, b) => a.sortOrder - b.sortOrder)[0]
                || {};
            const previewImage = primaryMedia.thumbUrl || primaryMedia.smallUrl || primaryMedia.mediumUrl || '/images/default-image.png';
            window.Kakao.Share.sendDefault({
                objectType: 'feed',
                content: {
                    title: '${fn:escapeXml(detail.title)}',
                    description: 'MemoryBox 게시물 공유 링크입니다.',
                    imageUrl: toAbsoluteUrl(previewImage),
                    link: {
                        mobileWebUrl: link,
                        webUrl: link
                    }
                },
                buttons: [{
                    title: '게시물 보기',
                    link: {
                        mobileWebUrl: link,
                        webUrl: link
                    }
                }]
            });
        });
    }

})();
</script>
</body>
</html>
