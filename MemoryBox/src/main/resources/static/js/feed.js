document.addEventListener('DOMContentLoaded', () => {
    const MAX_SELECTION_COUNT = 30;
    const DOWNLOAD_API_URL = '/feed/download-zip';
    const FEED_API_URL = '/api/feed/items';
    const FEED_STATE_KEY = 'memorybox.feed.state.v1';

    const body = document.body;
    const mode = body.dataset.mode || 'feed';
    const grid = document.getElementById('feedGrid');
    const tabButtons = document.querySelectorAll('.tab-btn');
    const colButtons = document.querySelectorAll('.col-btn');
    const mobileSelectionBar = document.getElementById('mobileSelectionBar');
    const selectedCountText = document.getElementById('selectedCount');
    const cancelSelectionBtn = document.getElementById('cancelSelectionBtn');
    const downloadSelectedBtn = document.getElementById('downloadSelectedBtn');
    const tagChecks = document.querySelectorAll('.tag-check');
    const selectedTagText = document.getElementById('selectedTagText');
    const authorFilter = document.getElementById('authorFilter');
    const albumFilter = document.getElementById('albumFilter');
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
    let hasMore = mode === 'feed';
    let page = 1;
    const pageSize = 24;

    const feedState = {
        type: 'all',
        columns: '1',
        author: '전체',
        album: '전체',
        sort: 'uploaded_desc',
        tags: [],
        scrollTop: 0
    };

    const openPasswordModal = () => {
        if (!passwordModalBackdrop) return;
        passwordModalBackdrop.hidden = false;
        document.body.classList.add('modal-open');
    };

    const closePasswordModal = () => {
        if (!passwordModalBackdrop) return;
        passwordModalBackdrop.hidden = true;
        document.body.classList.remove('modal-open');
    };

    openPasswordModalBtn?.addEventListener('click', openPasswordModal);
    closePasswordModalBtn?.addEventListener('click', closePasswordModal);
    cancelPasswordModalBtn?.addEventListener('click', closePasswordModal);
    passwordModalBackdrop?.addEventListener('click', (event) => {
        if (event.target === passwordModalBackdrop) closePasswordModal();
    });

    const getCards = () => grid.querySelectorAll('.feed-card');
    const getMediaBadges = () => grid.querySelectorAll('.media-badge');

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
    };

    const clearSelectionMode = () => {
        selectionMode = false;
        selectedIds.clear();
        getCards().forEach((card) => card.classList.remove('is-selected'));
        updateSelectionUI();
    };

    const enterSelectionMode = () => {
        selectionMode = true;
        updateSelectionUI();
    };

    const toggleCardSelection = (card) => {
        const id = card.dataset.itemId;
        if (selectedIds.has(id)) {
            selectedIds.delete(id);
            card.classList.remove('is-selected');
        } else {
            if (selectedIds.size >= MAX_SELECTION_COUNT) {
                window.alert(`최대 ${MAX_SELECTION_COUNT}개까지 선택할 수 있습니다.`);
                return;
            }
            selectedIds.add(id);
            card.classList.add('is-selected');
        }

        if (selectedIds.size === 0) {
            clearSelectionMode();
            return;
        }
        updateSelectionUI();
    };

    const saveFeedState = () => {
        if (mode !== 'feed') return;
        feedState.scrollTop = window.scrollY;
        sessionStorage.setItem(FEED_STATE_KEY, JSON.stringify(feedState));
    };

    const buildCardHtml = (item) => {
        const mediaLabel = item.mediaType === 'video' ? 'Video' : 'Photo';
        const tagHtml = (item.tags || []).map((tag) => `<li>#${tag}</li>`).join('');
        const title = item.title || '(제목 없음)';
        const takenAt = item.takenAt || '-';
        return `
            <article class="feed-card" data-media-type="${item.mediaType}" data-item-id="${item.id}">
                <a class="thumb-link" href="/feed/${item.id}" aria-label="${title} 상세보기">
                    <img src="${item.thumbnailUrl}" alt="${title} 썸네일" loading="lazy">
                    <span class="media-badge ${item.mediaType}" data-full-text="${mediaLabel}" data-short-text="${item.mediaType === 'video' ? 'V' : 'P'}">${mediaLabel}</span>
                    <span class="select-check" aria-hidden="true">✔</span>
                    <div class="overlay-meta overlay-top">
                        <p class="overlay-desc">${title}</p>
                        <p class="overlay-album">앨범 ${item.albumName || ''}</p>
                    </div>
                    <div class="overlay-meta overlay-bottom">
                        <p>💬 ${item.commentCount} · ❤ ${item.likeCount}</p>
                        <p>${item.author || ''}</p>
                    </div>
                </a>
                <div class="feed-meta">
                    <h2>${title}</h2>
                    <p>${item.author || ''} · 촬영 ${takenAt} · 업로드 ${item.uploadedAt || ''}</p>
                    <ul class="tag-list">${tagHtml}</ul>
                    <div class="engagement">
                        ${item.likeCount > 0 ? `<span>❤ ${item.likeCount}</span>` : ''}
                        ${item.commentCount > 0 ? `<span>💬 ${item.commentCount}</span>` : ''}
                    </div>
                </div>
            </article>`;
    };

    const bindCardEvents = (card) => {
        let longPressTimer;
        let longPressTriggered = false;
        let ignoreContextMenuUntil = 0;
        const thumbLink = card.querySelector('.thumb-link');

        card.addEventListener('contextmenu', (event) => {
            event.preventDefault();
            if (Date.now() < ignoreContextMenuUntil) return;
            if (!selectionMode) enterSelectionMode();
            toggleCardSelection(card);
        });

        card.addEventListener('touchstart', () => {
            longPressTriggered = false;
            longPressTimer = setTimeout(() => {
                longPressTriggered = true;
                ignoreContextMenuUntil = Date.now() + 800;
                if (!selectionMode) enterSelectionMode();
                if (!card.classList.contains('is-selected')) toggleCardSelection(card);
            }, 500);
        }, { passive: true });

        card.addEventListener('touchmove', () => clearTimeout(longPressTimer), { passive: true });
        card.addEventListener('touchcancel', () => {
            clearTimeout(longPressTimer);
            longPressTriggered = false;
        });

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

            if (mode === 'feed') saveFeedState();
            if (!selectionMode) return;

            event.preventDefault();
            toggleCardSelection(card);
        });
    };

    const bindAllCards = () => getCards().forEach((card) => bindCardEvents(card));

    const selectedPrimaryTag = () => {
        const selectedTags = Array.from(tagChecks).filter((check) => check.checked).map((check) => check.value);
        feedState.tags = selectedTags;
        return selectedTags.length > 0 && selectedTags[0] !== '전체' ? selectedTags[0] : null;
    };

    const updateTagSummary = () => {
        if (!selectedTagText) return;
        const selectedTags = Array.from(tagChecks).filter((check) => check.checked).map((check) => check.value);
        if (selectedTags.length === 0) {
            selectedTagText.textContent = '전체 태그';
        } else if (selectedTags.length <= 2) {
            selectedTagText.textContent = selectedTags.map((tag) => `#${tag}`).join(', ');
        } else {
            selectedTagText.textContent = `${selectedTags.slice(0, 2).map((tag) => `#${tag}`).join(', ')} 외 ${selectedTags.length - 2}`;
        }
    };

    const applyColumn = (columns) => {
        feedState.columns = columns;
        grid.classList.remove('columns-1', 'columns-3', 'columns-5');
        grid.classList.add(`columns-${columns}`);
        colButtons.forEach((b) => b.classList.toggle('is-active', b.dataset.columns === columns));
        updateBadgeLabels(columns);
    };

    const buildParams = () => {
        const params = new URLSearchParams();
        params.set('page', String(page));
        params.set('size', String(pageSize));
        if (feedState.type !== 'all') params.set('type', feedState.type);
        if (feedState.author && feedState.author !== '전체') params.set('author', feedState.author);
        if (feedState.album && feedState.album !== '전체') params.set('album', feedState.album);
        const primaryTag = selectedPrimaryTag();
        if (primaryTag) params.set('tag', primaryTag);
        params.set('sort', feedState.sort);
        if (mode === 'likes') params.set('likesOnly', 'true');
        if (mode === 'mypage') params.set('mineOnly', 'true');
        return params;
    };

    const loadNextPage = async () => {
        if (loading || !hasMore || !infiniteLoader || !feedSentinel) return;
        loading = true;
        infiniteLoader.hidden = false;

        try {
            const response = await fetch(`${FEED_API_URL}?${buildParams().toString()}`);
            if (!response.ok) throw new Error('피드 로드 실패');
            const payload = await response.json();
            const items = payload.items || [];
            hasMore = Boolean(payload.hasMore && items.length > 0);

            if (items.length > 0) {
                const html = items.map(buildCardHtml).join('');
                grid.insertAdjacentHTML('beforeend', html);
                Array.from(grid.querySelectorAll('.feed-card')).slice(-items.length).forEach(bindCardEvents);
                updateBadgeLabels(feedState.columns);
                page += 1;
            }
        } catch (e) {
            hasMore = false;
        } finally {
            loading = false;
            infiniteLoader.hidden = true;
        }
    };

    const resetAndReload = async () => {
        page = 1;
        hasMore = true;
        clearSelectionMode();
        grid.innerHTML = '';
        await loadNextPage();
        saveFeedState();
    };

    const restoreState = () => {
        if (mode !== 'feed') return;
        const savedRaw = sessionStorage.getItem(FEED_STATE_KEY);
        if (!savedRaw) return;
        try {
            const saved = JSON.parse(savedRaw);
            Object.assign(feedState, saved);
        } catch (e) {
            return;
        }

        tabButtons.forEach((button) => button.classList.toggle('is-active', button.dataset.filterType === feedState.type));
        if (authorFilter) authorFilter.value = feedState.author;
        if (albumFilter) albumFilter.value = feedState.album;
        if (sortOption) sortOption.value = feedState.sort;
        tagChecks.forEach((check) => {
            check.checked = feedState.tags?.includes(check.value);
        });
        applyColumn(feedState.columns || '1');
        updateTagSummary();
        window.requestAnimationFrame(() => window.scrollTo({ top: feedState.scrollTop || 0, behavior: 'auto' }));
    };

    const initInfiniteScroll = () => {
        if (!feedSentinel || mode !== 'feed') return;
        const observer = new IntersectionObserver((entries) => {
            entries.forEach((entry) => {
                if (entry.isIntersecting) loadNextPage();
            });
        }, { rootMargin: '200px 0px' });
        observer.observe(feedSentinel);
    };

    tabButtons.forEach((button) => {
        button.addEventListener('click', () => {
            tabButtons.forEach((b) => b.classList.remove('is-active'));
            button.classList.add('is-active');
            feedState.type = button.dataset.filterType;
            resetAndReload();
        });
    });

    colButtons.forEach((button) => button.addEventListener('click', () => applyColumn(button.dataset.columns)));
    authorFilter?.addEventListener('change', () => {
        feedState.author = authorFilter.value;
        resetAndReload();
    });
    albumFilter?.addEventListener('change', () => {
        feedState.album = albumFilter.value;
        resetAndReload();
    });
    sortOption?.addEventListener('change', () => {
        feedState.sort = sortOption.value;
        resetAndReload();
    });
    tagChecks.forEach((check) => check.addEventListener('change', () => {
        updateTagSummary();
        resetAndReload();
    }));

    cancelSelectionBtn?.addEventListener('click', clearSelectionMode);

    const setDownloadButtonLoading = (isLoading) => {
        if (!downloadSelectedBtn) return;
        downloadSelectedBtn.disabled = isLoading;
        downloadSelectedBtn.textContent = isLoading ? '다운로드 준비중...' : '다운로드';
    };

    const startDownload = (blob, fileName) => {
        const url = URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = fileName;
        document.body.appendChild(link);
        link.click();
        link.remove();
        URL.revokeObjectURL(url);
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
            if (!response.ok) throw new Error('download failed');
            const contentDisposition = response.headers.get('Content-Disposition') || '';
            const match = contentDisposition.match(/filename\*=UTF-8''([^;]+)/i);
            const fileName = match ? decodeURIComponent(match[1].trim()) : 'memorybox_download.zip';
            startDownload(await response.blob(), fileName);
        } catch (error) {
            window.alert('다중 다운로드 처리 중 오류가 발생했습니다.');
        } finally {
            setDownloadButtonLoading(false);
        }
    });

    bindAllCards();
    updateTagSummary();
    applyColumn('1');
    updateSelectionUI();

    if (mode === 'feed') {
        restoreState();
        page = 2;
        initInfiniteScroll();
        window.addEventListener('beforeunload', saveFeedState);
    }
});
