document.addEventListener('DOMContentLoaded', () => {
    const MAX_SELECTION_COUNT = 30;
    const DOWNLOAD_API_URL = '/feed/download-zip';
    const FEED_API_URL = '/api/feed/items';
    const FEED_STATE_KEY = 'memorybox.feed.state.v1';

    const mode = document.body.dataset.mode || 'feed';
    const isFeedMode = mode === 'feed';
    const isSearchMode = mode === 'search';
    const canInfinite = isFeedMode || isSearchMode;

    const grid = document.getElementById('feedGrid');
    const tabButtons = document.querySelectorAll('.tab-btn');
    const colButtons = document.querySelectorAll('.col-btn');
    const mobileSelectionBar = document.getElementById('mobileSelectionBar');
    const selectedCountText = document.getElementById('selectedCount');
    const cancelSelectionBtn = document.getElementById('cancelSelectionBtn');
    const downloadSelectedBtn = document.getElementById('downloadSelectedBtn');
    const authorFilter = document.getElementById('authorFilter');
    const albumFilter = document.getElementById('albumFilter');
    const tagFilter = document.getElementById('tagFilter');
    const sortOption = document.getElementById('sortOption');
    const infiniteLoader = document.getElementById('infiniteLoader');
    const feedSentinel = document.getElementById('feedSentinel');

    const passwordModalBackdrop = document.getElementById('passwordModalBackdrop');
    const openPasswordModalBtn = document.getElementById('openPasswordModalBtn');
    const closePasswordModalBtn = document.getElementById('closePasswordModalBtn');
    const cancelPasswordModalBtn = document.getElementById('cancelPasswordModalBtn');

    let selectionMode = false;
    const selectedIds = new Set();
    let loading = false;
    let hasMore = canInfinite;
    let page = 2;
    const pageSize = 24;

    const state = {
        type: 'all',
        columns: isSearchMode ? '3' : '1',
        author: '전체',
        album: '전체',
        tag: '',
        sort: 'uploaded_desc',
        scrollTop: 0
    };

    const getCards = () => grid.querySelectorAll('.feed-card');
    const getMediaBadges = () => grid.querySelectorAll('.media-badge');

    const updateSelectionUI = () => {
        selectedCountText.textContent = String(selectedIds.size);
        mobileSelectionBar.hidden = !selectionMode;
    };

    const updateBadgeLabels = (columns) => {
        const shortMode = columns === '3' || columns === '5';
        getMediaBadges().forEach((badge) => {
            badge.textContent = shortMode ? badge.dataset.shortText : badge.dataset.fullText;
        });
    };

    const applyColumn = (columns) => {
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

    const bindCardEvents = (card) => {
        let longPressTimer;
        let longPressTriggered = false;
        const thumbLink = card.querySelector('.thumb-link');

        card.addEventListener('contextmenu', (event) => {
            event.preventDefault();
            toggleCardSelection(card);
        });

        card.addEventListener('touchstart', () => {
            longPressTriggered = false;
            longPressTimer = setTimeout(() => {
                longPressTriggered = true;
                toggleCardSelection(card);
            }, 500);
        }, { passive: true });

        card.addEventListener('touchmove', () => clearTimeout(longPressTimer), { passive: true });
        card.addEventListener('touchend', () => {
            clearTimeout(longPressTimer);
            if (longPressTriggered) thumbLink.dataset.ignoreClickOnce = 'true';
        });

        thumbLink.addEventListener('click', (event) => {
            if (thumbLink.dataset.ignoreClickOnce === 'true') {
                thumbLink.dataset.ignoreClickOnce = 'false';
                event.preventDefault();
                return;
            }
            if (isFeedMode) saveFeedState();
            if (!selectionMode) return;
            event.preventDefault();
            toggleCardSelection(card);
        });
    };

    const buildParams = () => {
        const params = new URLSearchParams();
        params.set('page', String(page));
        params.set('size', String(pageSize));
        if (state.type !== 'all') params.set('type', state.type);
        if (authorFilter && authorFilter.value !== '전체') params.set('author', authorFilter.value);
        if (albumFilter && albumFilter.value !== '전체') params.set('album', albumFilter.value);
        if (tagFilter && tagFilter.value) params.set('tag', tagFilter.value);
        if (sortOption) params.set('sort', sortOption.value);
        if (mode === 'likes') params.set('likesOnly', 'true');
        if (mode === 'mypage') params.set('mineOnly', 'true');
        return params;
    };

    const buildCardHtml = (item) => {
        const mediaLabel = item.mediaType === 'video' ? 'Video' : 'Photo';
        const tagHtml = (item.tags || []).map((tag) => `<li>#${tag}</li>`).join('');
        const title = item.title || '(제목 없음)';
        return `<article class="feed-card" data-media-type="${item.mediaType}" data-item-id="${item.id}">
            <a class="thumb-link" href="/feed/${item.id}" aria-label="${title} 상세보기">
                <img src="${item.thumbnailUrl}" alt="${title} 썸네일" loading="lazy">
                <span class="media-badge ${item.mediaType}" data-full-text="${mediaLabel}" data-short-text="${item.mediaType === 'video' ? 'V' : 'P'}">${mediaLabel}</span>
                <span class="select-check" aria-hidden="true">✔</span>
                <div class="overlay-meta overlay-top"><p class="overlay-desc">${title}</p><p class="overlay-album">앨범 ${item.albumName || ''}</p></div>
                <div class="overlay-meta overlay-bottom"><p>💬 ${item.commentCount} · ❤ ${item.likeCount}</p><p>${item.author || ''}</p></div>
            </a>
            <div class="feed-meta"><h2>${title}</h2><p>${item.author || ''} · 촬영 ${item.takenAt || '-'} · 업로드 ${item.uploadedAt || ''}</p><ul class="tag-list">${tagHtml}</ul>
                <div class="engagement">${item.likeCount > 0 ? `<span>❤ ${item.likeCount}</span>` : ''}${item.commentCount > 0 ? `<span>💬 ${item.commentCount}</span>` : ''}</div>
            </div></article>`;
    };

    const loadNextPage = async () => {
        if (!canInfinite || loading || !hasMore) return;
        loading = true;
        infiniteLoader.hidden = false;
        try {
            const response = await fetch(`${FEED_API_URL}?${buildParams().toString()}`);
            if (!response.ok) throw new Error();
            const payload = await response.json();
            const items = payload.items || [];
            hasMore = Boolean(payload.hasMore && items.length > 0);
            if (items.length > 0) {
                grid.insertAdjacentHTML('beforeend', items.map(buildCardHtml).join(''));
                Array.from(grid.querySelectorAll('.feed-card')).slice(-items.length).forEach(bindCardEvents);
                updateBadgeLabels(state.columns);
                page += 1;
            }
        } finally {
            loading = false;
            infiniteLoader.hidden = true;
        }
    };

    const reloadFromFirstPage = async () => {
        if (!canInfinite) return;
        page = 1;
        hasMore = true;
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
            tabButtons.forEach((b) => b.classList.remove('is-active'));
            button.classList.add('is-active');
            state.type = button.dataset.filterType;
            reloadFromFirstPage();
        });
    });

    colButtons.forEach((button) => button.addEventListener('click', () => applyColumn(button.dataset.columns)));
    albumFilter?.addEventListener('change', reloadFromFirstPage);
    authorFilter?.addEventListener('change', reloadFromFirstPage);
    tagFilter?.addEventListener('change', reloadFromFirstPage);
    sortOption?.addEventListener('change', reloadFromFirstPage);

    cancelSelectionBtn?.addEventListener('click', clearSelectionMode);

    const setDownloadButtonLoading = (isLoading) => {
        if (!downloadSelectedBtn) return;
        downloadSelectedBtn.disabled = isLoading;
        downloadSelectedBtn.textContent = isLoading ? '다운로드 준비중...' : '다운로드';
    };

    downloadSelectedBtn?.addEventListener('click', async () => {
        if (selectedIds.size === 0) return window.alert('다운로드할 파일을 먼저 선택해 주세요.');
        setDownloadButtonLoading(true);
        try {
            const response = await fetch(DOWNLOAD_API_URL, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ mediaIds: Array.from(selectedIds, (id) => Number(id)) })
            });
            if (!response.ok) throw new Error();
            const blob = await response.blob();
            const url = URL.createObjectURL(blob);
            const link = document.createElement('a');
            link.href = url;
            link.download = 'memorybox_download.zip';
            document.body.appendChild(link);
            link.click();
            link.remove();
            URL.revokeObjectURL(url);
        } catch (e) {
            window.alert('다중 다운로드 처리 중 오류가 발생했습니다.');
        } finally {
            setDownloadButtonLoading(false);
        }
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

    getCards().forEach(bindCardEvents);
    updateSelectionUI();
    applyColumn(state.columns);
    if (isFeedMode) restoreFeedState();
    initInfiniteObserver();
    if (isFeedMode) window.addEventListener('beforeunload', saveFeedState);
});
