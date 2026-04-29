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
</head>
<body class="page page-detail">
<main class="detail-layout" id="shareDetailLayout" data-token="${shareToken}">
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

<div class="viewer-backdrop" id="viewerBackdrop" hidden>
    <section class="viewer-panel">
        <button type="button" class="viewer-close" id="viewerCloseBtn">✕</button>
        <button type="button" class="viewer-nav prev" id="viewerPrevBtn">‹</button>
        <button type="button" class="viewer-nav next" id="viewerNextBtn">›</button>
        <div class="viewer-content" id="viewerContent"></div>
        <div class="viewer-footer">
            <span id="viewerCounter"><span id="viewerCurrent">1</span> / <span id="viewerTotal">1</span></span>
            <c:if test="${allowDownload}">
                <a class="btn btn-secondary" id="viewerDownloadBtn" href="#">원본 다운로드</a>
            </c:if>
        </div>
    </section>
</div>

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
    const viewerBackdrop = document.getElementById('viewerBackdrop');
    const viewerContent = document.getElementById('viewerContent');
    const viewerCurrent = document.getElementById('viewerCurrent');
    const viewerTotal = document.getElementById('viewerTotal');
    const viewerDownloadBtn = document.getElementById('viewerDownloadBtn');
    const viewerCloseBtn = document.getElementById('viewerCloseBtn');
    const viewerPrevBtn = document.getElementById('viewerPrevBtn');
    const viewerNextBtn = document.getElementById('viewerNextBtn');
    const selectionBar = document.getElementById('selectionBar');
    const selectedCount = document.getElementById('selectedCount');
    const cancelSelectBtn = document.getElementById('cancelSelectBtn');
    const downloadSelectBtn = document.getElementById('downloadSelectBtn');
    const shareToken = document.getElementById('shareDetailLayout')?.dataset.token;
    let currentIndex = 0;
    const selected = new Set();
    let selectionMode = false;

    const createViewerMedia = (index, data) => {
        const button = items[index];
        const thumbElement = button?.querySelector('img, video');
        const thumbElementSrc = thumbElement?.getAttribute('src') || '';
        if (data.mediaType === 'VIDEO') {
            const video = document.createElement('video');
            video.controls = true;
            video.playsInline = true;
            video.autoplay = true;
            video.src = data.previewUrl || thumbElementSrc || data.thumbUrl || data.mediumUrl || data.smallUrl || '';
            return video;
        }
        const image = document.createElement('img');
        image.src = thumbElementSrc || data.thumbUrl || data.mediumUrl || data.smallUrl || '';
        image.alt = 'viewer';
        return image;
    };

    const renderViewer = (index) => {
        const data = mediaEntries[index];
        if (!data) return;
        const frame = document.createElement('div');
        frame.className = 'viewer-media-frame';
        frame.appendChild(createViewerMedia(index, data));
        viewerContent.innerHTML = '';
        viewerContent.appendChild(frame);
        currentIndex = index;
        viewerCurrent.textContent = String(index + 1);
        viewerTotal.textContent = String(items.length || 1);
        if (viewerDownloadBtn) viewerDownloadBtn.href = data.downloadUrl;
    };

    const openViewer = (index) => {
        viewerBackdrop.hidden = false;
        renderViewer(index);
    };

    items.forEach((btn, index) => {
        btn.addEventListener('click', () => {
            if (selectionMode) {
                const id = btn.dataset.mediaId;
                if (selected.has(id)) selected.delete(id); else selected.add(id);
                selectedCount.textContent = String(selected.size);
                btn.classList.toggle('is-selected', selected.has(id));
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

    if (viewerCloseBtn) viewerCloseBtn.addEventListener('click', () => { viewerBackdrop.hidden = true; });
    if (viewerBackdrop) viewerBackdrop.addEventListener('click', (e) => { if (e.target === viewerBackdrop) viewerBackdrop.hidden = true; });
    if (viewerPrevBtn) viewerPrevBtn.addEventListener('click', () => renderViewer((currentIndex - 1 + items.length) % items.length));
    if (viewerNextBtn) viewerNextBtn.addEventListener('click', () => renderViewer((currentIndex + 1) % items.length));

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
