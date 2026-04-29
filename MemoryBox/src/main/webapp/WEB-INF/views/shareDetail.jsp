<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>MemoryBox - 공유 상세</title>
    <%@ include file="/WEB-INF/views/common/head-icons.jspf" %>
    <link rel="stylesheet" href="/css/common.css">
    <link rel="stylesheet" href="/css/detail.css">
    <link rel="stylesheet" href="/css/photoswipe/photoswipe.css">
</head>
<body class="page page-detail share-page ${allowDownload ? '' : 'no-download'}">
<main class="detail-layout" id="shareDetailLayout" data-token="${shareToken}" data-download-allowed="${allowDownload ? 'true' : 'false'}">
    <header class="detail-header">
        <div class="login-chip">게스트 공유</div>
    </header>

    <section class="detail-panel meta-panel">
        <h1 class="detail-title">${detail.title}</h1>
        <p class="meta-line">작성자 ${detail.authorName}</p>
        <p class="meta-line">업로드 ${detail.relativeUploadedAt}</p>
        <p class="meta-line">앨범 ${detail.albumName} · ${detail.commentCount} 댓글</p>
    </section>

    <section class="detail-panel" id="batchGridSection">
        <div class="batch-grid" id="batchGrid" data-total-count="${fn:length(detailItems)}">
            <c:forEach var="item" items="${detailItems}" varStatus="status">
                <button type="button" class="grid-item"
                        data-media-id="${item.mediaId}"
                        data-index="${status.index}"
                        data-media-type="${item.mediaType}"
                        data-small-url="${fn:escapeXml(item.smallUrl)}"
                        data-medium-url="${fn:escapeXml(item.mediumUrl)}"
                        data-preview-url="${fn:escapeXml(item.previewUrl)}"
                        data-download-url="/share/${shareToken}/media/${item.mediaId}/download">
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

    <c:if test="${allowDownload}">
        <section class="detail-panel action-panel">
            <a href="/share/${shareToken}/download-all" class="btn btn-secondary download-btn">전체 다운로드</a>
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

<c:if test="${allowDownload}">
    <div class="selection-bar" id="selectionBar" hidden>
        <span><strong id="selectedCount">0</strong>개 선택</span>
        <div>
            <button type="button" class="btn btn-secondary" id="cancelSelectBtn">취소</button>
            <button type="button" class="btn btn-secondary" id="downloadSelectBtn">선택 다운로드</button>
        </div>
    </div>
</c:if>

<script>
window.PhotoSwipe = window.PhotoSwipe || undefined;
</script>
<script src="/js/photoswipe/photoswipe.umd.min.js"></script>
<script src="/js/photoswipe/photoswipe-lightbox.umd.min.js"></script>
<script>
(() => {
    const grid = document.getElementById('batchGrid');
    if (!grid) return;
    const items = Array.from(grid.querySelectorAll('.grid-item'));
    const mediaEntries = items.map((button) => ({
        mediaId: button.dataset.mediaId,
        mediaType: button.dataset.mediaType,
        smallUrl: button.dataset.smallUrl,
        mediumUrl: button.dataset.mediumUrl,
        previewUrl: button.dataset.previewUrl,
        downloadUrl: button.dataset.downloadUrl,
        thumbUrl: button.querySelector('img, video')?.getAttribute('src') || ''
    }));
    const selectionBar = document.getElementById('selectionBar');
    const selectedCount = document.getElementById('selectedCount');
    const cancelSelectBtn = document.getElementById('cancelSelectBtn');
    const downloadSelectBtn = document.getElementById('downloadSelectBtn');
    const shareRoot = document.getElementById('shareDetailLayout');
    const shareToken = shareRoot?.dataset.token;
    const allowDownload = shareRoot?.dataset.downloadAllowed === 'true';
    const selected = new Set();
    let selectionMode = false;

    const toAbsoluteUrl = (url) => {
        if (!url) return '';
        if (/^https?:\/\//i.test(url)) return url;
        return window.location.origin + (url.startsWith('/') ? url : '/' + url);
    };
    const imageSizeCache = new Map();
    const readImageSize = (src) => new Promise((resolve) => {
        if (!src) return resolve({width: 1600, height: 1200});
        if (imageSizeCache.has(src)) return resolve(imageSizeCache.get(src));
        const img = new Image();
        img.onload = () => {
            const size = {width: img.naturalWidth || 1600, height: img.naturalHeight || 1200};
            imageSizeCache.set(src, size);
            resolve(size);
        };
        img.onerror = () => resolve({width: 1600, height: 1200});
        img.src = src;
    });
    const openViewer = async (index) => {
        const clicked = mediaEntries[index];
        if (!clicked || clicked.mediaType === 'VIDEO' || !window.PhotoSwipe) return;
        const imageEntries = mediaEntries.filter((entry) => entry.mediaType !== 'VIDEO');
        const imageItems = await Promise.all(imageEntries.map(async (entry) => {
            const src = toAbsoluteUrl(entry.mediumUrl || entry.previewUrl || entry.smallUrl);
            const size = await readImageSize(src);
            return { src, width: size.width, height: size.height };
        }));
        const imageIndex = mediaEntries.filter((entry, i) => i <= index && entry.mediaType !== 'VIDEO').length - 1;
        const pswp = new window.PhotoSwipe({ dataSource: imageItems, index: Math.max(0, imageIndex), pswpModule: window.PhotoSwipe, wheelToZoom: true });
        if (!allowDownload) {
            pswp.on('bindEvents', () => {
                pswp.events.add(pswp.scrollWrap, 'contextmenu', (event) => event.preventDefault());
            });
        }
        pswp.init();
    };

    if (!allowDownload) {
        const blockedSelector = [
            'img', 'video', 'a', 'picture', 'figure',
            '.photo-card', '.share-grid-item', '.grid-item',
            '.batch-grid', '.pswp', '.pswp__container', '.pswp__item'
        ].join(', ');
        const blockSaveAttempt = (event) => {
            const target = event.target;
            if (!(target instanceof Element)) return;
            if (target.closest(blockedSelector)) {
                event.preventDefault();
                event.stopPropagation();
            }
        };
        document.addEventListener('contextmenu', blockSaveAttempt, true);
        document.addEventListener('dragstart', blockSaveAttempt, true);
        document.addEventListener('selectstart', blockSaveAttempt, true);
    }

    items.forEach((btn, index) => {
        btn.addEventListener('click', () => {
            if (selectionMode) {
                const id = btn.dataset.mediaId;
                if (selected.has(id)) selected.delete(id); else selected.add(id);
                selectedCount.textContent = String(selected.size);
                btn.classList.toggle('is-selected', selected.has(id));
                return;
            }
            if (btn.dataset.mediaType === 'VIDEO') {
                window.location.href = '/share/' + shareToken + '/video/' + btn.dataset.mediaId;
                return;
            }
            openViewer(index);
        });
        btn.addEventListener('contextmenu', (e) => {
            if (!selectionBar) return;
            e.preventDefault();
            selectionMode = true;
            selectionBar.hidden = false;
            const id = btn.dataset.mediaId;
            if (selected.has(id)) selected.delete(id); else selected.add(id);
            selectedCount.textContent = String(selected.size);
            btn.classList.toggle('is-selected', selected.has(id));
        });
    });

    if (cancelSelectBtn) cancelSelectBtn.addEventListener('click', () => {
        selectionMode = false;
        selected.clear();
        selectionBar.hidden = true;
        items.forEach((btn) => btn.classList.remove('is-selected'));
    });

    if (downloadSelectBtn && shareToken) downloadSelectBtn.addEventListener('click', async () => {
        const mediaIds = Array.from(selected, (v) => Number(v));
        if (mediaIds.length === 0) return;
        if (mediaIds.length === 1) {
            const target = items.find((i) => i.dataset.mediaId === String(mediaIds[0]));
            if (target) window.location.href = target.dataset.downloadUrl;
            return;
        }
        const res = await fetch('/share/' + shareToken + '/download-zip', {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify({mediaIds})
        });
        if (!res.ok) {
            alert('다운로드 실패');
            return;
        }
        const blob = await res.blob();
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = 'memorybox_share_selected.zip';
        a.click();
        URL.revokeObjectURL(url);
    });
})();
</script>
</body>
</html>
