document.addEventListener('DOMContentLoaded', () => {
    const MAX_SELECTION_COUNT = 30;
    const DOWNLOAD_API_URL = '/feed/download-zip';
    const FEED_API_URL = '/api/feed/items';
    const FEED_STATE_KEY = 'memorybox.feed.state.v1';

    const mode = document.body.dataset.mode || 'feed';
    const isFeedMode = mode === 'feed';
    const isSearchMode = mode === 'search';
    const canInfinite = isFeedMode || isSearchMode || mode === 'likes' || mode === 'mypage';

    const grid = document.getElementById('feedGrid');
    const tabButtons = document.querySelectorAll('.tab-btn');
    const colButtons = document.querySelectorAll('.col-btn');
    const mobileSelectionBar = document.getElementById('mobileSelectionBar');
    const selectedCountText = document.getElementById('selectedCount');
    const cancelSelectionBtn = document.getElementById('cancelSelectionBtn');
    const downloadSelectedBtn = document.getElementById('downloadSelectedBtn');
    const authorFilter = document.getElementById('authorFilter');
    const albumFilter = document.getElementById('albumFilter');
    const albumPickerSection = document.getElementById('albumPickerSection');
    const albumPickerGrid = document.getElementById('albumPickerGrid');
    const selectedAlbumHeader = document.getElementById('selectedAlbumHeader');
    const selectedAlbumTitle = document.getElementById('selectedAlbumTitle');
    const floatingHead = document.getElementById('floatingHead');
    const controlPanel = isSearchMode ? document.querySelector('.control-panel') : null;
    const sortOption = document.getElementById('sortOption');
    const tagChecks = document.querySelectorAll('.tag-check');
    const selectedTagText = document.getElementById('selectedTagText');
    const infiniteLoader = document.getElementById('infiniteLoader');
    const feedSentinel = document.getElementById('feedSentinel');
    const feedEndMessage = document.getElementById('feedEndMessage');
    const loadedCountText = document.getElementById('loadedCountText');
    const totalCountText = document.getElementById('totalCountText');
    const notificationToggleBtn = document.getElementById('notificationToggleBtn');
    const notificationDropdown = document.getElementById('notificationDropdown');
    const notificationList = document.getElementById('notificationList');
    const notificationUnreadBadge = document.getElementById('notificationUnreadBadge');

    const passwordModalBackdrop = document.getElementById('passwordModalBackdrop');
    const openPasswordModalBtn = document.getElementById('openPasswordModalBtn');
    const closePasswordModalBtn = document.getElementById('closePasswordModalBtn');
    const cancelPasswordModalBtn = document.getElementById('cancelPasswordModalBtn');

    const commentSheetBackdrop = document.getElementById('commentSheetBackdrop');
    const commentSheetCloseBtn = document.getElementById('commentSheetCloseBtn');
    const commentSheetBody = document.getElementById('commentSheetBody');
    const commentSheetForm = document.getElementById('commentSheetForm');
    const commentParentIdInput = document.getElementById('commentParentId');
    const commentSheetInput = document.getElementById('commentSheetInput');
    const commentReplyCancelBtn = document.getElementById('commentReplyCancelBtn');

    let selectionMode = false;
    const selectedIds = new Set();
    const likePendingIds = new Set();
    let loading = false;
    let hasMore = canInfinite;
    let page = 2;
    const pageSize = 24;
    let activeCommentMediaId = null;
    let activePreviewVideo = null;
    let previewObserver = null;

    const state = {
        type: 'all',
        columns: isSearchMode ? '3' : '1',
        sort: 'uploaded_desc',
        scrollTop: 0,
        selectedAlbum: isSearchMode ? null : '전체'
    };

    const isAlbumPickerMode = () => isSearchMode && state.selectedAlbum === null;

    const stopVideo = (video, reset = false) => {
        if (!video) return;
        video.pause();
        if (reset) {
            video.currentTime = 0;
        }
    };

    const playVideo = (video) => {
        if (!video) return;
        if (activePreviewVideo && activePreviewVideo !== video) {
            stopVideo(activePreviewVideo, true);
        }
        activePreviewVideo = video;
        video.play().catch(() => {});
    };

    const initPreviewAutoplay = () => {
        const videos = Array.from(grid.querySelectorAll('.feed-preview-video[data-has-preview="true"]'));
        if (videos.length === 0) return;

        if (!previewObserver) {
            previewObserver = new IntersectionObserver((entries) => {
                entries.forEach((entry) => {
                    const video = entry.target;
                    if (entry.isIntersecting && entry.intersectionRatio >= 0.6) {
                        playVideo(video);
                        return;
                    }
                    if (video === activePreviewVideo) {
                        activePreviewVideo = null;
                    }
                    stopVideo(video, true);
                });
            }, { threshold: [0.6] });
        }

        videos.forEach((video) => {
            if (video.dataset.previewObserved === 'true') return;
            video.dataset.previewObserved = 'true';
            previewObserver.observe(video);
        });
    };

    const updateTagSummary = () => {
        if (!selectedTagText) return;
        const selectedTags = Array.from(tagChecks).filter((check) => check.checked).map((check) => check.value);
        if (selectedTags.length === 0) {
            selectedTagText.textContent = '태그 전체';
        } else if (selectedTags.length <= 2) {
            selectedTagText.textContent = selectedTags.join(', ');
        } else {
            selectedTagText.textContent = `${selectedTags.slice(0, 2).join(', ')} 외 ${selectedTags.length - 2}`;
        }
    };

    const selectedPrimaryTag = () => Array.from(tagChecks).find((check) => check.checked)?.value || '';

    const getCards = () => grid.querySelectorAll('.feed-card:not(.is-nav-card)');
    const getMediaBadges = () => grid.querySelectorAll('.media-badge');
    const getNewBadges = () => grid.querySelectorAll('.new-badge');

    const updateSelectionUI = () => {
        if (!selectedCountText || !mobileSelectionBar) return;
        selectedCountText.textContent = String(selectedIds.size);
        mobileSelectionBar.hidden = !selectionMode;
    };

    const updateBadgeLabels = (columns) => {
        const shortMode = columns === '3' || columns === '5';
        getMediaBadges().forEach((badge) => {
            badge.textContent = shortMode ? badge.dataset.shortText : badge.dataset.fullText;
        });
        getNewBadges().forEach((badge) => {
            badge.textContent = shortMode ? badge.dataset.shortText : badge.dataset.fullText;
        });
    };

    const applyColumn = (columns) => {
        if (!grid) return;
        state.columns = columns;
        grid.classList.remove('columns-1', 'columns-3', 'columns-5');
        grid.classList.add(`columns-${columns}`);
        colButtons.forEach((b) => b.classList.toggle('is-active', b.dataset.columns === columns));
        updateBadgeLabels(columns);
    };

    const clearSelectionMode = () => {
        selectionMode = false;
        selectedIds.clear();
        getCards().forEach((card) => card.classList.remove('is-selected'));
        updateSelectionUI();
    };

    const toggleCardSelection = (card) => {
        const id = card.dataset.itemId;
        if (selectedIds.has(id)) {
            selectedIds.delete(id);
            card.classList.remove('is-selected');
        } else {
            if (selectedIds.size >= MAX_SELECTION_COUNT) return window.alert(`최대 ${MAX_SELECTION_COUNT}개까지 선택할 수 있습니다.`);
            selectedIds.add(id);
            card.classList.add('is-selected');
        }
        if (selectedIds.size === 0) return clearSelectionMode();
        selectionMode = true;
        updateSelectionUI();
    };

    const saveFeedState = () => {
        if (!isFeedMode) return;
        state.scrollTop = window.scrollY;
        sessionStorage.setItem(FEED_STATE_KEY, JSON.stringify(state));
    };

    const resetSearchFilters = () => {
        state.type = 'all';
        tabButtons.forEach((button) => button.classList.toggle('is-active', button.dataset.filterType === 'all'));
        if (authorFilter) authorFilter.value = '전체';
        tagChecks.forEach((check) => { check.checked = false; });
        updateTagSummary();
        if (sortOption) {
            sortOption.value = 'uploaded_desc';
            state.sort = sortOption.value;
        }
    };

    const buildBackCardHtml = () => `
        <article class="feed-card is-nav-card" data-role="back-to-albums">
            <button type="button" class="back-album-btn" aria-label="앨범 선택으로 돌아가기">
                <span class="thumb-link back-thumb" aria-hidden="true">
                    <span class="back-album-icon">↩</span>
                </span>
                <div class="feed-meta back-meta">
                    <h2>이전</h2>
                    <p>앨범 선택으로</p>
                </div>
            </button>
        </article>
    `;

    const injectBackCard = () => {
        if (!isSearchMode || isAlbumPickerMode() || !grid) return;
        if (grid.querySelector('.is-nav-card')) return;
        grid.insertAdjacentHTML('afterbegin', buildBackCardHtml());
    };

    const enterAlbumPicker = () => {
        if (!isSearchMode) return;
        state.selectedAlbum = null;
        clearSelectionMode();
        resetSearchFilters();
        if (grid) grid.innerHTML = '';
        updateCountUI(0, 0);
        hasMore = false;
        page = 1;
        if (albumPickerSection) albumPickerSection.hidden = false;
        if (selectedAlbumHeader) selectedAlbumHeader.hidden = true;
        if (floatingHead) floatingHead.hidden = true;
        if (controlPanel) controlPanel.hidden = true;
        if (feedEndMessage) feedEndMessage.hidden = true;
    };

    const enterAlbumView = async (albumName) => {
        if (!isSearchMode) return;
        state.selectedAlbum = albumName || '전체';
        if (selectedAlbumTitle) selectedAlbumTitle.textContent = state.selectedAlbum;
        if (albumPickerSection) albumPickerSection.hidden = true;
        if (selectedAlbumHeader) selectedAlbumHeader.hidden = false;
        if (floatingHead) floatingHead.hidden = false;
        if (controlPanel) controlPanel.hidden = false;
        resetSearchFilters();
        await reloadFromFirstPage();
    };

    const isActionElement = (target) => Boolean(target.closest('[data-action], button, input, textarea, select, .modal-close-btn'));

    const handleCardClick = (event, card) => {
        const detailUrl = card.dataset.detailUrl;
        if (!detailUrl) return;
        if (isActionElement(event.target)) return;

        if (selectionMode) {
            event.preventDefault();
            toggleCardSelection(card);
            return;
        }

        if (isFeedMode) saveFeedState();
        window.location.href = detailUrl;
    };

    const bindCardEvents = (card) => {
        let longPressTimer;
        let longPressTriggered = false;
        let suppressContextMenuUntil = 0;

        card.addEventListener('contextmenu', (event) => {
            if (isActionElement(event.target)) return;
            if (Date.now() < suppressContextMenuUntil) {
                event.preventDefault();
                return;
            }
            event.preventDefault();
            toggleCardSelection(card);
        });

        card.addEventListener('touchstart', (event) => {
            if (isActionElement(event.target)) return;
            longPressTriggered = false;
            longPressTimer = setTimeout(() => {
                longPressTriggered = true;
                suppressContextMenuUntil = Date.now() + 800;
                toggleCardSelection(card);
            }, 500);
        }, { passive: true });

        card.addEventListener('touchmove', () => clearTimeout(longPressTimer), { passive: true });
        card.addEventListener('touchend', () => clearTimeout(longPressTimer));

        card.addEventListener('click', (event) => {
            if (longPressTriggered) {
                event.preventDefault();
                longPressTriggered = false;
                return;
            }
            handleCardClick(event, card);
        });

        card.querySelectorAll('[data-action="like-toggle"]').forEach((button) => {
            button.addEventListener('click', (event) => {
                event.preventDefault();
                event.stopPropagation();
                toggleLike(card);
            });
        });

        const commentButton = card.querySelector('[data-action="open-comments"]');
        commentButton?.addEventListener('click', (event) => {
            event.preventDefault();
            event.stopPropagation();
            if (state.columns !== '1') {
                window.location.href = card.dataset.detailUrl;
                return;
            }
            openCommentSheet(card.dataset.itemId);
        });
    };

    const buildParams = () => {
        const params = new URLSearchParams();
        params.set('page', String(page));
        params.set('size', String(pageSize));
        if (isSearchMode) {
            if (state.selectedAlbum === null) return params;
            if (state.selectedAlbum !== '전체') params.set('album', state.selectedAlbum);
        }
        if (state.type !== 'all') params.set('type', state.type);
        if (authorFilter && authorFilter.value !== '전체') params.set('author', authorFilter.value);
        if (albumFilter && albumFilter.value !== '전체') params.set('album', albumFilter.value);
        const selectedTag = selectedPrimaryTag();
        if (selectedTag) params.set('tag', selectedTag);
        if (sortOption) params.set('sort', sortOption.value);
        if (mode === 'likes') params.set('likesOnly', 'true');
        if (mode === 'mypage') params.set('mineOnly', 'true');
        return params;
    };

    const buildTagLi = (tag) => `<li class="${tag.startsWith('@') ? 'person-tag' : ''}">${escapeHtml(tag)}</li>`;

    const buildCardHtml = (item) => {
        const mediaLabel = item.mediaType === 'video' ? 'Video' : 'Photo';
        const tagHtml = (item.tags || []).map(buildTagLi).join('');
        const title = item.title || '(제목 없음)';
        const likedClass = item.likedByMe ? 'is-liked' : '';
        const likedIcon = item.likedByMe ? '❤' : '♡';

        const mediaHtml = item.mediaType === 'video'
            ? `<video class="feed-preview-video" src="${item.previewUrl || ''}" poster="${item.thumbnailUrl || ''}" muted playsinline loop preload="none" data-has-preview="${item.previewUrl ? 'true' : 'false'}"></video>`
            : `<img src="${item.thumbnailUrl}" alt="${escapeHtml(title)} 썸네일" loading="lazy">`;

        return `<article class="feed-card" data-media-type="${item.mediaType}" data-item-id="${item.id}" data-detail-url="/feed/${item.id}">
            <a class="thumb-link" href="/feed/${item.id}" aria-label="${escapeHtml(title)} 상세보기">
                ${mediaHtml}
                <span class="media-badge ${item.mediaType}" data-full-text="${mediaLabel}" data-short-text="${item.mediaType === 'video' ? 'V' : 'P'}">${mediaLabel}</span>
                ${item.recent ? `<span class="new-badge" data-full-text="New" data-short-text="N">New</span>` : ""}
                <span class="select-check" aria-hidden="true">✔</span>
                <div class="overlay-meta overlay-bottom"><p>${escapeHtml(item.author || '')}</p></div>
            </a>
            <button type="button" class="like-toggle-btn ${likedClass}" data-action="like-toggle" aria-label="좋아요 토글" aria-pressed="${item.likedByMe}"><span class="heart">${likedIcon}</span></button>
            <div class="feed-meta">
                <h2>${escapeHtml(title)}</h2>
                <p>${escapeHtml(item.author || '')} · 촬영 ${escapeHtml(item.takenAt || '-')} · 업로드 ${escapeHtml(item.relativeUploadedAt || item.uploadedAt || '')}</p>
                <ul class="tag-list">${tagHtml}</ul>
                <div class="engagement" data-action="meta-actions">
                    <button type="button" class="meta-btn like-meta-btn ${likedClass}" data-action="like-toggle" aria-label="좋아요 토글">❤ <span class="like-count">${item.likeCount || 0}</span></button>
                    <button type="button" class="meta-btn comment-meta-btn" data-action="open-comments" aria-label="댓글 열기">💬 <span class="comment-count">${item.commentCount || 0}</span></button>
                </div>
            </div>
        </article>`;
    };

    const escapeHtml = (raw) => (raw || '')
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;');

    const updateCountUI = (loadedCount, totalCount) => {
        if (loadedCountText) loadedCountText.textContent = String(loadedCount);
        if (totalCountText) totalCountText.textContent = String(totalCount);
    };

    const extractErrorMessage = async (response, fallback) => {
        try {
            const payload = await response.json();
            return payload?.message || fallback;
        } catch (_) {
            return fallback;
        }
    };

    const updateCardStats = (mediaId, stats) => {
        const card = grid.querySelector(`.feed-card[data-item-id="${mediaId}"]`);
        if (!card) return;

        if (typeof stats.likeCount === 'number') {
            card.querySelectorAll('.like-count').forEach((el) => { el.textContent = String(stats.likeCount); });
        }
        if (typeof stats.commentCount === 'number') {
            card.querySelectorAll('.comment-count').forEach((el) => { el.textContent = String(stats.commentCount); });
        }
        if (typeof stats.likedByMe === 'boolean') {
            card.querySelectorAll('[data-action="like-toggle"]').forEach((button) => {
                button.classList.toggle('is-liked', stats.likedByMe);
                if (button.classList.contains('like-toggle-btn')) {
                    const heart = button.querySelector('.heart');
                    if (heart) heart.textContent = stats.likedByMe ? '❤' : '♡';
                    button.setAttribute('aria-pressed', stats.likedByMe ? 'true' : 'false');
                }
            });
        }
    };

    const toggleLike = async (card) => {
        const mediaId = card.dataset.itemId;
        if (!mediaId || likePendingIds.has(mediaId)) return;

        const topLikeButton = card.querySelector('.like-toggle-btn');
        const currentlyLiked = topLikeButton?.classList.contains('is-liked');
        const action = currentlyLiked ? 'unlike' : 'like';

        likePendingIds.add(mediaId);
        card.classList.add('is-like-pending');

        try {
            const response = await fetch(`/api/feed/${mediaId}/like`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8' },
                body: `action=${encodeURIComponent(action)}`
            });
            if (!response.ok) {
                const message = await extractErrorMessage(response, '좋아요 처리 중 오류가 발생했습니다.');
                window.alert(message);
                return;
            }
            const payload = await response.json();
            if (!payload.success) {
                window.alert(payload.message || '좋아요 처리 중 오류가 발생했습니다.');
                return;
            }
            updateCardStats(mediaId, {
                likeCount: Number(payload.likeCount || 0),
                commentCount: Number(payload.commentCount || 0),
                likedByMe: Boolean(payload.likedByMe)
            });
        } catch (_) {
            window.alert('좋아요 처리 중 오류가 발생했습니다.');
        } finally {
            likePendingIds.delete(mediaId);
            card.classList.remove('is-like-pending');
        }
    };

    const renderComments = (comments) => {
        if (!commentSheetBody) return;
        if (!comments || comments.length === 0) {
            commentSheetBody.innerHTML = '<p class="comment-empty">첫 댓글을 남겨보세요.</p>';
            return;
        }

        const renderItem = (comment, isReply) => {
            const replies = (comment.replies || []).map((reply) => renderItem(reply, true)).join('');
            return `<li class="${isReply ? 'sheet-reply-item' : 'sheet-comment-item'}">
                <div class="sheet-comment-head"><strong>${escapeHtml(comment.authorName || '알 수 없음')}</strong><span>${escapeHtml(comment.createdAt || '')}</span></div>
                <p>${escapeHtml(comment.content || '')}</p>
                ${isReply ? '' : `<button type="button" class="btn btn-text reply-toggle-btn" data-action="reply" data-comment-id="${comment.commentId}" data-author="${escapeHtml(comment.authorName || '')}">답글 달기</button>`}
                ${replies ? `<ul class="sheet-reply-list">${replies}</ul>` : ''}
            </li>`;
        };

        commentSheetBody.innerHTML = `<ul class="sheet-comment-list">${comments.map((comment) => renderItem(comment, false)).join('')}</ul>`;
    };

    const openCommentSheet = async (mediaId) => {
        if (!commentSheetBackdrop || !mediaId) return;

        activeCommentMediaId = mediaId;
        commentParentIdInput.value = '';
        commentSheetInput.placeholder = '댓글을 입력해 주세요';
        commentReplyCancelBtn.hidden = true;
        commentSheetBody.innerHTML = '<p class="comment-empty">댓글을 불러오는 중...</p>';
        commentSheetBackdrop.hidden = false;
        document.body.classList.add('modal-open');

        try {
            const response = await fetch(`/api/feed/${mediaId}/comments`);
            if (!response.ok) {
                const message = await extractErrorMessage(response, '댓글을 불러오지 못했습니다.');
                commentSheetBody.innerHTML = `<p class="comment-empty">${escapeHtml(message)}</p>`;
                return;
            }
            const payload = await response.json();
            renderComments(payload.comments || []);
            updateCardStats(mediaId, {
                likeCount: Number(payload.likeCount || 0),
                commentCount: Number(payload.commentCount || 0),
                likedByMe: Boolean(payload.likedByMe)
            });
        } catch (_) {
            commentSheetBody.innerHTML = '<p class="comment-empty">댓글을 불러오지 못했습니다.</p>';
        }
    };

    const closeCommentSheet = () => {
        if (!commentSheetBackdrop) return;
        commentSheetBackdrop.hidden = true;
        document.body.classList.remove('modal-open');
        activeCommentMediaId = null;
        commentParentIdInput.value = '';
        commentReplyCancelBtn.hidden = true;
        commentSheetInput.value = '';
        commentSheetInput.placeholder = '댓글을 입력해 주세요';
    };

    const loadNextPage = async () => {
        if (!grid) return;
        if (isAlbumPickerMode()) return;
        if (!canInfinite || loading || !hasMore) return;
        loading = true;
        if (feedEndMessage) feedEndMessage.hidden = true;
        infiniteLoader.hidden = false;
        try {
            const response = await fetch(`${FEED_API_URL}?${buildParams().toString()}`);
            if (!response.ok) throw new Error();
            const payload = await response.json();
            const items = payload.items || [];
            const totalCount = Number(payload.totalCount || 0);
            const loadedCount = Number(payload.loadedCount || 0);
            updateCountUI(loadedCount, totalCount);
            hasMore = Boolean(payload.hasMore);
            if (items.length > 0) {
                if (feedEndMessage) feedEndMessage.hidden = true;
                grid.insertAdjacentHTML('beforeend', items.map(buildCardHtml).join(''));
                Array.from(grid.querySelectorAll('.feed-card')).slice(-items.length).forEach(bindCardEvents);
                updateBadgeLabels(state.columns);
                initPreviewAutoplay();
                page += 1;
            }
            injectBackCard();
        } finally {
            loading = false;
            infiniteLoader.hidden = true;
            if (!hasMore && feedEndMessage) {
                feedEndMessage.hidden = false;
            }
        }
    };

    const reloadFromFirstPage = async () => {
        if (!canInfinite || isAlbumPickerMode() || !grid) return;
        page = 1;
        hasMore = true;
        if (feedEndMessage) feedEndMessage.hidden = true;
        grid.innerHTML = '';
        clearSelectionMode();
        await loadNextPage();
        page = 2;
        if (isFeedMode) saveFeedState();
    };

    const restoreFeedState = () => {
        if (!isFeedMode) return;
        const raw = sessionStorage.getItem(FEED_STATE_KEY);
        if (!raw) return;
        try {
            Object.assign(state, JSON.parse(raw));
            tabButtons.forEach((button) => button.classList.toggle('is-active', button.dataset.filterType === state.type));
            applyColumn(state.columns || '1');
            if (sortOption) sortOption.value = state.sort || 'uploaded_desc';
            window.requestAnimationFrame(() => window.scrollTo({ top: state.scrollTop || 0, behavior: 'auto' }));
        } catch (ignore) {
            // noop
        }
    };

    const initInfiniteObserver = () => {
        if (!canInfinite || !feedSentinel) return;
        const observer = new IntersectionObserver((entries) => {
            entries.forEach((entry) => {
                if (entry.isIntersecting) loadNextPage();
            });
        }, { rootMargin: '220px 0px' });
        observer.observe(feedSentinel);
    };

    tabButtons.forEach((button) => {
        button.addEventListener('click', () => {
            if (isAlbumPickerMode()) return;
            tabButtons.forEach((b) => b.classList.remove('is-active'));
            button.classList.add('is-active');
            state.type = button.dataset.filterType;
            reloadFromFirstPage();
        });
    });

    colButtons.forEach((button) => button.addEventListener('click', () => applyColumn(button.dataset.columns)));
    albumFilter?.addEventListener('change', reloadFromFirstPage);
    authorFilter?.addEventListener('change', reloadFromFirstPage);
    sortOption?.addEventListener('change', () => {
        state.sort = sortOption.value;
        reloadFromFirstPage();
    });
    tagChecks.forEach((check) => check.addEventListener('change', () => {
        updateTagSummary();
        reloadFromFirstPage();
    }));

    cancelSelectionBtn?.addEventListener('click', clearSelectionMode);

    const debugDownloadState = (stage, extra = {}) => {
        console.debug('[download-debug]', stage, {
            selectionMode,
            selectedCount: selectedIds.size,
            overlayVisible: Boolean(document.getElementById('downloadLockOverlay') && !document.getElementById('downloadLockOverlay').hidden),
            bodyDownloadLock: document.body.classList.contains('download-lock-active'),
            buttonDisabled: Boolean(downloadSelectedBtn?.disabled),
            ...extra
        });
    };

    const setDownloadButtonLoading = (isLoading) => {
        if (!downloadSelectedBtn) return;
        downloadSelectedBtn.disabled = isLoading;
        downloadSelectedBtn.textContent = isLoading ? '다운로드 준비중...' : '다운로드';
    };

    const ensureDownloadOverlay = () => {
        let overlay = document.getElementById('downloadLockOverlay');
        if (overlay) return overlay;

        overlay = document.createElement('div');
        overlay.id = 'downloadLockOverlay';
        overlay.className = 'download-lock-overlay';
        overlay.hidden = true;
        overlay.innerHTML = `
            <section class="download-lock-panel" role="alert" aria-live="polite">
                <h2 class="download-lock-title">다운로드 준비중</h2>
                <p class="download-lock-message" id="downloadLockMessage">파일을 준비하고 있습니다...</p>
                <progress class="download-lock-progress" id="downloadLockProgress" max="100" value="0"></progress>
            </section>
        `;
        document.body.appendChild(overlay);
        return overlay;
    };

    const setDownloadLock = (active, message = '파일을 준비하고 있습니다...', progressPercent = null) => {
        const overlay = ensureDownloadOverlay();
        const messageEl = overlay.querySelector('#downloadLockMessage');
        const progressEl = overlay.querySelector('#downloadLockProgress');

        document.body.classList.toggle('download-lock-active', active);
        overlay.hidden = !active;
        if (messageEl) messageEl.textContent = message;

        if (progressEl) {
            if (typeof progressPercent === 'number') {
                progressEl.removeAttribute('indeterminate');
                progressEl.value = Math.max(0, Math.min(100, progressPercent));
            } else {
                progressEl.removeAttribute('value');
            }
        }
    };

    const resetDownloadUiState = (reason) => {
        debugDownloadState('multi-download-ui-reset-before', { reason });
        setDownloadLock(false);
        setDownloadButtonLoading(false);
        clearSelectionMode();

        const overlay = document.getElementById('downloadLockOverlay');
        if (overlay) {
            overlay.hidden = true;
        }
        const temporaryLinks = document.querySelectorAll('a[download="memorybox_download.zip"]');
        temporaryLinks.forEach((link) => link.remove());
        document.body.classList.remove('download-lock-active');
        document.body.style.removeProperty('pointer-events');
        debugDownloadState('multi-download-ui-reset-after', { reason, removedTempLinkCount: temporaryLinks.length });
    };

    const downloadBlobWithProgress = async (response) => {
        if (!response.body) {
            return await response.blob();
        }
        const contentLength = Number(response.headers.get('Content-Length') || 0);
        const reader = response.body.getReader();
        const chunks = [];
        let received = 0;
        while (true) {
            const { done, value } = await reader.read();
            if (done) break;
            if (!value) continue;
            chunks.push(value);
            received += value.length;
            if (contentLength > 0) {
                const percent = (received / contentLength) * 100;
                setDownloadLock(true, `다운로드 중... ${Math.floor(percent)}%`, percent);
            } else {
                setDownloadLock(true, `다운로드 중... ${Math.floor(received / 1024)}KB`);
            }
        }
        return new Blob(chunks);
    };

    downloadSelectedBtn?.addEventListener('click', async (event) => {
        event.preventDefault();
        event.stopPropagation();
        if (selectedIds.size === 0) return window.alert('다운로드할 파일을 먼저 선택해 주세요.');
        debugDownloadState('multi-download-start');
        setDownloadButtonLoading(true);
        setDownloadLock(true, 'ZIP 파일을 준비하고 있습니다...');
        try {
            const response = await fetch(DOWNLOAD_API_URL, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ mediaIds: Array.from(selectedIds, (id) => Number(id)) })
            });
            if (!response.ok) throw new Error();
            const blob = await downloadBlobWithProgress(response);
            const url = URL.createObjectURL(blob);
            const link = document.createElement('a');
            link.href = url;
            link.download = 'memorybox_download.zip';
            document.body.appendChild(link);
            debugDownloadState('multi-download-before-trigger', { downloadUrl: link.href });
            link.click();
            link.remove();
            URL.revokeObjectURL(url);
            debugDownloadState('multi-download-success');
        } catch (e) {
            debugDownloadState('multi-download-fail', { error: e?.message || String(e) });
            window.alert('다중 다운로드 처리 중 오류가 발생했습니다.');
        } finally {
            resetDownloadUiState('finally');
            debugDownloadState('multi-download-finally');
        }
    });

    albumPickerGrid?.addEventListener('click', (event) => {
        const card = event.target.closest('.album-picker-card');
        if (!card) return;
        enterAlbumView(card.dataset.albumValue || '전체');
    });

    grid?.addEventListener('click', (event) => {
        const backCardButton = event.target.closest('.back-album-btn');
        if (!backCardButton) return;
        event.preventDefault();
        event.stopPropagation();
        enterAlbumPicker();
    });

    openPasswordModalBtn?.addEventListener('click', () => {
        passwordModalBackdrop.hidden = false;
        document.body.classList.add('modal-open');
    });
    const closeModal = () => {
        if (!passwordModalBackdrop) return;
        passwordModalBackdrop.hidden = true;
        document.body.classList.remove('modal-open');
    };
    closePasswordModalBtn?.addEventListener('click', closeModal);
    cancelPasswordModalBtn?.addEventListener('click', closeModal);
    passwordModalBackdrop?.addEventListener('click', (event) => {
        if (event.target === passwordModalBackdrop) closeModal();
    });

    commentSheetCloseBtn?.addEventListener('click', closeCommentSheet);
    commentSheetBackdrop?.addEventListener('click', (event) => {
        if (event.target === commentSheetBackdrop) closeCommentSheet();
    });

    commentSheetBody?.addEventListener('click', (event) => {
        const replyBtn = event.target.closest('[data-action="reply"]');
        if (!replyBtn) return;
        commentParentIdInput.value = replyBtn.dataset.commentId || '';
        commentReplyCancelBtn.hidden = false;
        commentSheetInput.placeholder = `${replyBtn.dataset.author || ''}님에게 답글 작성`;
        commentSheetInput.focus();
    });

    commentReplyCancelBtn?.addEventListener('click', () => {
        commentParentIdInput.value = '';
        commentReplyCancelBtn.hidden = true;
        commentSheetInput.placeholder = '댓글을 입력해 주세요';
    });

    commentSheetForm?.addEventListener('submit', async (event) => {
        event.preventDefault();
        if (!activeCommentMediaId) return;

        const content = commentSheetInput.value.trim();
        if (!content) {
            window.alert('댓글 내용을 입력해 주세요.');
            return;
        }

        const params = new URLSearchParams();
        params.set('content', content);
        if (commentParentIdInput.value) {
            params.set('parentId', commentParentIdInput.value);
        }

        try {
            const response = await fetch(`/api/feed/${activeCommentMediaId}/comments`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8' },
                body: params.toString()
            });
            if (!response.ok) {
                const message = await extractErrorMessage(response, '댓글 처리 중 오류가 발생했습니다.');
                window.alert(message);
                return;
            }

            const payload = await response.json();
            if (!payload.success) {
                window.alert(payload.message || '댓글 처리 중 오류가 발생했습니다.');
                return;
            }

            renderComments(payload.comments || []);
            updateCardStats(activeCommentMediaId, {
                likeCount: Number(payload.likeCount || 0),
                commentCount: Number(payload.commentCount || 0),
                likedByMe: Boolean(payload.likedByMe)
            });

            commentSheetInput.value = '';
            commentParentIdInput.value = '';
            commentReplyCancelBtn.hidden = true;
            commentSheetInput.placeholder = '댓글을 입력해 주세요';
        } catch (_) {
            window.alert('댓글 처리 중 오류가 발생했습니다.');
        }
    });



    const refreshNotifications = async () => {
        if (!notificationList) return;
        try {
            const response = await fetch('/api/notifications');
            if (!response.ok) return;
            const payload = await response.json();
            const items = payload.items || [];
            if (notificationUnreadBadge) {
                const unread = Number(payload.unreadCount || 0);
                notificationUnreadBadge.textContent = String(unread);
                notificationUnreadBadge.hidden = unread <= 0;
            }
            if (items.length === 0) {
                notificationList.innerHTML = '<li class="empty">새 알림이 없습니다.</li>';
                return;
            }
            notificationList.innerHTML = items.map((item) => `
                <li class="${item.isRead ? '' : 'is-unread'}">
                    <a href="/notifications/${item.notificationId}/open" class="notification-link">
                        <p>${escapeHtml(item.message || '')}</p>
                        <small>${escapeHtml(item.relativeCreatedAt || '')}</small>
                    </a>
                    <div class="notification-actions">
                        <button type="button" class="notification-action-btn" data-action="notification-read" data-id="${item.notificationId}">읽음</button>
                        <button type="button" class="notification-action-btn is-delete" data-action="notification-delete" data-id="${item.notificationId}">삭제</button>
                    </div>
                </li>
            `).join('');
        } catch (_) {
            // noop
        }
    };

    notificationToggleBtn?.addEventListener('click', () => {
        if (!notificationDropdown) return;
        const next = !notificationDropdown.hidden;
        notificationDropdown.hidden = next;
        if (!next) {
            refreshNotifications();
        }
    });

    document.addEventListener('click', (event) => {
        if (!notificationDropdown || notificationDropdown.hidden) return;
        if (event.target.closest('#notificationDropdown, #notificationToggleBtn')) return;
        notificationDropdown.hidden = true;
    });

    notificationList?.addEventListener('click', async (event) => {
        const actionButton = event.target.closest('[data-action="notification-read"], [data-action="notification-delete"]');
        if (!actionButton) return;
        event.preventDefault();
        event.stopPropagation();

        const notificationId = actionButton.dataset.id;
        if (!notificationId) return;
        const isDelete = actionButton.dataset.action === 'notification-delete';
        const endpoint = isDelete
            ? `/api/notifications/${notificationId}/delete`
            : `/api/notifications/${notificationId}/read`;

        try {
            const response = await fetch(endpoint, { method: 'POST' });
            if (!response.ok) {
                return;
            }
            await refreshNotifications();
        } catch (_) {
            // noop
        }
    });


    const initCalendarWidget = () => {
        const calendarCard = document.getElementById('sharedCalendarCard');
        if (!calendarCard || calendarCard.dataset.calendarState !== 'READY') return;

        const eventPanel = document.getElementById('calendarEventPanel');
        const eventHeader = document.getElementById('calendarEventHeader');
        const eventList = document.getElementById('calendarEventList');
        const closeBtn = document.getElementById('calendarCloseBtn');
        const dayButtons = Array.from(document.querySelectorAll('.calendar-day[data-date]'));

        if (!eventPanel || !eventHeader || !eventList) return;

        let monthData;
        try {
            monthData = JSON.parse(eventPanel.dataset.calendarMonth || '{}');
        } catch (_) {
            return;
        }

        const dayMap = new Map((monthData.days || []).map((day) => [day.date, day]));
        const upcomingEvents = monthData.upcomingEvents || [];

        const formatKoreanDate = (isoDate) => {
            const [y, m, d] = isoDate.split('-').map(Number);
            return `${m}월 ${d}일`;
        };

        const calcDday = (isoDate) => {
            const today = new Date();
            today.setHours(0, 0, 0, 0);
            const target = new Date(`${isoDate}T00:00:00`);
            const diff = Math.floor((target.getTime() - today.getTime()) / (1000 * 60 * 60 * 24));
            if (diff === 0) return 'D-Day';
            if (diff > 0) return `D-${diff}`;
            return `D+${Math.abs(diff)}`;
        };

        const renderUpcoming = () => {
            eventHeader.textContent = '다가오는 일정';
            closeBtn.hidden = true;
            if (!upcomingEvents.length) {
                eventList.innerHTML = '<li class="empty">다가오는 일정이 없습니다.</li>';
                return;
            }
            eventList.innerHTML = upcomingEvents.map((event) => `
                <li>
                    <span class="event-time">${calcDday(event.date)} · ${event.timeText}</span>
                    <span class="event-title">${escapeHtml(event.title)}</span>
                </li>
            `).join('');
        };

        const renderSelectedDate = (isoDate) => {
            const dayData = dayMap.get(isoDate);
            const events = dayData?.events || [];
            eventHeader.textContent = `${formatKoreanDate(isoDate)} 일정`;
            closeBtn.hidden = false;
            if (!events.length) {
                eventList.innerHTML = '<li class="empty">일정이 없습니다.</li>';
                return;
            }
            eventList.innerHTML = events.map((event) => `
                <li>
                    <span class="event-time">${event.timeText}</span>
                    <span class="event-title">${escapeHtml(event.title)}</span>
                </li>
            `).join('');
        };

        dayButtons.forEach((button) => {
            button.addEventListener('click', () => {
                dayButtons.forEach((node) => node.classList.remove('is-selected'));
                button.classList.add('is-selected');
                renderSelectedDate(button.dataset.date);
            });
        });

        closeBtn?.addEventListener('click', () => {
            dayButtons.forEach((node) => node.classList.remove('is-selected'));
            renderUpcoming();
        });

        renderUpcoming();
    };

    getCards().forEach(bindCardEvents);
    updateSelectionUI();
    updateCountUI(getCards().length, Number(totalCountText?.textContent || getCards().length));
    updateTagSummary();
    applyColumn(state.columns);
    if (sortOption) sortOption.value = state.sort;
    if (isFeedMode) restoreFeedState();
    initInfiniteObserver();
    initPreviewAutoplay();
    if (isSearchMode) enterAlbumPicker();
    initCalendarWidget();
    if (isFeedMode) window.addEventListener('beforeunload', saveFeedState);
});
