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
    const sortOption = document.getElementById('sortOption');
    const tagChecks = document.querySelectorAll('.tag-check');
    const selectedTagText = document.getElementById('selectedTagText');
    const infiniteLoader = document.getElementById('infiniteLoader');
    const feedSentinel = document.getElementById('feedSentinel');
    const feedEndMessage = document.getElementById('feedEndMessage');
    const loadedCountText = document.getElementById('loadedCountText');
    const totalCountText = document.getElementById('totalCountText');

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

    const state = {
        type: 'all',
        columns: isSearchMode ? '3' : '1',
        sort: 'uploaded_desc',
        scrollTop: 0
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

        card.addEventListener('contextmenu', (event) => {
            if (isActionElement(event.target)) return;
            event.preventDefault();
            toggleCardSelection(card);
        });

        card.addEventListener('touchstart', (event) => {
            if (isActionElement(event.target)) return;
            longPressTriggered = false;
            longPressTimer = setTimeout(() => {
                longPressTriggered = true;
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

        return `<article class="feed-card" data-media-type="${item.mediaType}" data-item-id="${item.id}" data-detail-url="/feed/${item.id}">
            <a class="thumb-link" href="/feed/${item.id}" aria-label="${escapeHtml(title)} 상세보기">
                <img src="${item.thumbnailUrl}" alt="${escapeHtml(title)} 썸네일" loading="lazy">
                <span class="media-badge ${item.mediaType}" data-full-text="${mediaLabel}" data-short-text="${item.mediaType === 'video' ? 'V' : 'P'}">${mediaLabel}</span>
                <span class="select-check" aria-hidden="true">✔</span>
                <div class="overlay-meta overlay-bottom"><p>${escapeHtml(item.author || '')}</p></div>
            </a>
            <button type="button" class="like-toggle-btn ${likedClass}" data-action="like-toggle" aria-label="좋아요 토글" aria-pressed="${item.likedByMe}"><span class="heart">${likedIcon}</span></button>
            <div class="feed-meta">
                <h2>${escapeHtml(title)}</h2>
                <p>${escapeHtml(item.author || '')} · 촬영 ${escapeHtml(item.takenAt || '-')} · 업로드 ${escapeHtml(item.uploadedAt || '')}</p>
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
                page += 1;
            }
        } finally {
            loading = false;
            infiniteLoader.hidden = true;
            if (!hasMore && feedEndMessage) {
                feedEndMessage.hidden = false;
            }
        }
    };

    const reloadFromFirstPage = async () => {
        if (!canInfinite) return;
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

    getCards().forEach(bindCardEvents);
    updateSelectionUI();
    updateCountUI(getCards().length, Number(totalCountText?.textContent || getCards().length));
    updateTagSummary();
    applyColumn(state.columns);
    if (sortOption) sortOption.value = state.sort;
    if (isFeedMode) restoreFeedState();
    initInfiniteObserver();
    if (isFeedMode) window.addEventListener('beforeunload', saveFeedState);
});
