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
        <section class="detail-panel meta-panel">
            <div class="title-row">
                <h1 class="detail-title">${detail.title}</h1>
                <a class="btn btn-secondary" href="/feed/${currentBatchId}/download-all">전체 다운로드</a>
            </div>
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
                    <li class="comment-item"><strong>${comment.authorName}</strong> ${comment.content}</li>
                </c:forEach>
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
            <a class="btn btn-secondary" id="viewerDownloadBtn" href="#">원본 다운로드</a>
        </div>
    </section>
</div>

<div class="selection-bar" id="selectionBar" hidden>
    <span><strong id="selectedCount">0</strong>개 선택</span>
    <div>
        <button type="button" class="btn btn-secondary" id="cancelSelectBtn">취소</button>
        <button type="button" class="btn btn-secondary" id="downloadSelectBtn">선택 다운로드</button>
    </div>
</div>

<script>
(() => {
    const grid = document.getElementById('batchGrid');
    if (!grid) return;
    const items = Array.from(grid.querySelectorAll('.grid-item'));
    const totalItems = items.length || 1;
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
    const selectionBar = document.getElementById('selectionBar');
    const selectedCount = document.getElementById('selectedCount');

    let currentIndex = 0;
    let selectionMode = false;
    const selected = new Set();

    const getItemData = (index) => mediaEntries[index];

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

    const createViewerFrame = (index, data) => {
        const frame = document.createElement('div');
        frame.className = 'viewer-media-frame';
        frame.appendChild(createViewerMedia(index, data));
        return frame;
    };

    const renderViewer = (index, direction) => {
        const data = getItemData(index);
        if (!data) return;
        const nextFrame = createViewerFrame(index, data);
        const currentFrame = viewerContent.querySelector('.viewer-media-frame');
        const isAnimated = (direction === 'next' || direction === 'prev') && currentFrame && !viewerContent.dataset.animating;

        if (!isAnimated) {
            viewerContent.innerHTML = '';
            viewerContent.appendChild(nextFrame);
        } else {
            viewerContent.dataset.animating = 'true';
            const offset = direction === 'next' ? 100 : -100;
            const durationMs = 320;
            const easing = 'cubic-bezier(0.22, 1, 0.36, 1)';

            nextFrame.style.transform = `translateX(${offset}%)`;
            nextFrame.style.opacity = '0.92';
            nextFrame.style.transition = `transform ${durationMs}ms ${easing}, opacity ${durationMs}ms ease`;
            currentFrame.style.transition = `transform ${durationMs}ms ${easing}, opacity ${durationMs}ms ease`;

            viewerContent.appendChild(nextFrame);

            requestAnimationFrame(() => {
                currentFrame.style.transform = `translateX(${-offset}%)`;
                currentFrame.style.opacity = '0.88';
                nextFrame.style.transform = 'translateX(0)';
                nextFrame.style.opacity = '1';
            });

            window.setTimeout(() => {
                viewerContent.innerHTML = '';
                nextFrame.style.transition = '';
                nextFrame.style.transform = '';
                nextFrame.style.opacity = '';
                viewerContent.appendChild(nextFrame);
                delete viewerContent.dataset.animating;
            }, durationMs + 20);
        }

        currentIndex = index;
        viewerCurrent.textContent = String(index + 1);
        viewerTotal.textContent = String(totalItems);
        viewerDownloadBtn.href = data.downloadUrl;
    };

    const openViewer = (index) => {
        viewerBackdrop.hidden = false;
        document.body.classList.add('modal-open');
        renderViewer(index, 'none');
    };

    const closeViewer = () => {
        viewerBackdrop.hidden = true;
        document.body.classList.remove('modal-open');
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
        btn.addEventListener('touchstart', () => {
            longPressTriggered = false;
            timer = setTimeout(() => {
                longPressTriggered = true;
                selectionMode = true;
                toggleSelection(btn);
            }, 500);
        }, {passive:true});
        btn.addEventListener('touchmove', () => { if (timer) clearTimeout(timer); }, {passive:true});
        btn.addEventListener('touchend', () => { if (timer) clearTimeout(timer); }, {passive:true});
        btn.addEventListener('contextmenu', (e) => { e.preventDefault(); selectionMode = true; toggleSelection(btn); });
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

    document.getElementById('viewerCloseBtn').addEventListener('click', closeViewer);
    document.getElementById('viewerPrevBtn').addEventListener('click', () => renderViewer((currentIndex - 1 + items.length) % items.length, 'prev'));
    document.getElementById('viewerNextBtn').addEventListener('click', () => renderViewer((currentIndex + 1) % items.length, 'next'));
    viewerBackdrop.addEventListener('click', (e) => { if (e.target === viewerBackdrop) closeViewer(); });

    let touchX = null;
    viewerContent.addEventListener('touchstart', (e) => { touchX = e.changedTouches[0].clientX; }, {passive:true});
    viewerContent.addEventListener('touchend', (e) => {
        if (touchX == null) return;
        const diff = e.changedTouches[0].clientX - touchX;
        touchX = null;
        if (Math.abs(diff) < 30) return;
        renderViewer(
            diff > 0 ? (currentIndex - 1 + items.length) % items.length : (currentIndex + 1) % items.length,
            diff > 0 ? 'prev' : 'next'
        );
    }, {passive:true});

    document.getElementById('cancelSelectBtn').addEventListener('click', () => {
        selectionMode = false; selected.clear(); syncSelectionUi();
    });

    document.getElementById('downloadSelectBtn').addEventListener('click', async () => {
        if (selected.size === 0) return;
        const mediaIds = Array.from(selected, (v) => Number(v));
        if (mediaIds.length === 1) {
            const target = items.find((i) => i.dataset.mediaId === String(mediaIds[0]));
            if (target) window.location.href = target.dataset.downloadUrl;
            return;
        }
        const res = await fetch('/feed/download-zip', {
            method: 'POST', headers: {'Content-Type': 'application/json'},
            body: JSON.stringify({mediaIds})
        });
        if (!res.ok) { alert('다운로드 실패'); return; }
        const blob = await res.blob();
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a'); a.href = url; a.download = 'memorybox_selected.zip'; a.click();
        URL.revokeObjectURL(url);
    });

})();
</script>
</body>
</html>
