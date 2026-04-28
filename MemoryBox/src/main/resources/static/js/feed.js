document.addEventListener('DOMContentLoaded', () => {
    const FEED_API_URL = '/api/feed/items';
    const FEED_STATE_KEY = 'memorybox.feed.state.v1';

    const mode = document.body.dataset.mode || 'feed';
    const isFeedMode = mode === 'feed';
    const isSearchMode = mode === 'search';
    const canInfinite = isFeedMode || isSearchMode || mode === 'likes' || mode === 'mypage';

    const grid = document.getElementById('feedGrid');
    const tabButtons = document.querySelectorAll('.tab-btn');
    const colButtons = document.querySelectorAll('.col-btn');
    const authorFilter = document.getElementById('authorFilter');
    const albumFilter = document.getElementById('albumFilter');
    const albumPickerSection = document.getElementById('albumPickerSection');
    const albumPickerGrid = document.getElementById('albumPickerGrid');
    const selectedAlbumHeader = document.getElementById('selectedAlbumHeader');
    const selectedAlbumTitle = document.getElementById('selectedAlbumTitle');
    const searchModeTabs = document.getElementById('searchModeTabs');
    const searchModeButtons = document.querySelectorAll('.search-mode-btn');
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
    const searchViewerBackdrop = document.getElementById('searchViewerBackdrop');
    const searchViewerContent = document.getElementById('searchViewerContent');
    const searchViewerCurrent = document.getElementById('searchViewerCurrent');
    const searchViewerTotal = document.getElementById('searchViewerTotal');
    const searchViewerDownloadBtn = document.getElementById('searchViewerDownloadBtn');
    const searchViewerCloseBtn = document.getElementById('searchViewerCloseBtn');
    const searchViewerPrevBtn = document.getElementById('searchViewerPrevBtn');
    const searchViewerNextBtn = document.getElementById('searchViewerNextBtn');
    const searchSelectionBar = document.getElementById('searchSelectionBar');
    const searchSelectedCount = document.getElementById('searchSelectedCount');
    const searchCancelSelectBtn = document.getElementById('searchCancelSelectBtn');
    const searchDownloadSelectBtn = document.getElementById('searchDownloadSelectBtn');

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
        selectedAlbum: isSearchMode ? null : '전체',
        searchMode: 'feed'
    };
    let photoViewerItems = [];
    let photoViewerIndex = 0;
    let longPressTimer = null;
    let longPressTriggered = false;
    const selectedPhotoIds = new Set();
    let selectingPhotoMode = false;
    let searchViewerHistoryActive = false;

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
        const compact = columns === '3' || columns === '5';
        getCards().forEach((card) => {
            card.classList.toggle('compact-card', compact && state.searchMode === 'feed');
            const countNode = card.querySelector('.compact-batch-count');
            if (countNode) countNode.hidden = !(compact && state.searchMode === 'feed');
        });
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
                    <span class="back-thumb-label">이전</span>
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
        resetSearchFilters();
        selectingPhotoMode = false;
        selectedPhotoIds.clear();
        if (grid) grid.innerHTML = '';
        updateCountUI(0, 0);
        hasMore = false;
        page = 1;
        if (albumPickerSection) albumPickerSection.hidden = false;
        if (selectedAlbumHeader) selectedAlbumHeader.hidden = true;
        if (searchModeTabs) searchModeTabs.hidden = true;
        if (floatingHead) floatingHead.hidden = true;
        if (controlPanel) controlPanel.hidden = true;
        if (feedEndMessage) feedEndMessage.hidden = true;
        if (searchSelectionBar) searchSelectionBar.hidden = true;
    };

    const enterAlbumView = async (albumName) => {
        if (!isSearchMode) return;
        state.selectedAlbum = albumName || '전체';
        if (selectedAlbumTitle) selectedAlbumTitle.textContent = state.selectedAlbum;
        if (albumPickerSection) albumPickerSection.hidden = true;
        if (selectedAlbumHeader) selectedAlbumHeader.hidden = false;
        if (searchModeTabs) searchModeTabs.hidden = false;
        if (floatingHead) floatingHead.hidden = false;
        if (controlPanel) controlPanel.hidden = false;
        selectingPhotoMode = false;
        selectedPhotoIds.clear();
        if (searchSelectedCount) searchSelectedCount.textContent = '0';
        if (searchSelectionBar) searchSelectionBar.hidden = true;
        resetSearchFilters();
        await reloadFromFirstPage();
    };

    const isActionElement = (target) => Boolean(target.closest('[data-action], button, input, textarea, select, .modal-close-btn'));

    const handleCardClick = (event, card) => {
        if (isSearchMode && state.searchMode === 'photo') {
            if (isActionElement(event.target)) return;
            if (longPressTriggered) {
                event.preventDefault();
                longPressTriggered = false;
                return;
            }
            if (selectingPhotoMode) {
                event.preventDefault();
                togglePhotoSelect(card);
                return;
            }
            const mediaId = card.dataset.mediaId;
            const items = JSON.parse(card.dataset.batchItems || '[]');
            const currentIndex = Math.max(0, items.findIndex((item) => String(item.mediaId) === String(mediaId)));
            openSearchViewer(items, currentIndex);
            return;
        }
        const detailUrl = card.dataset.detailUrl;
        if (!detailUrl) return;
        if (isActionElement(event.target)) return;

        if (isFeedMode) saveFeedState();
        window.location.href = detailUrl;
    };

    const initCardSlider = (card) => {
        const slider = card.querySelector('[data-slider]');
        if (!slider) return;
        const track = slider.querySelector('[data-slider-track]');
        const slides = Array.from(slider.querySelectorAll('.feed-slide'));
        const counter = slider.querySelector('[data-slide-counter]');
        if (!track || slides.length <= 1) return;

        let currentIndex = 0;
        const moveTo = (nextIndex) => {
            const safeIndex = (nextIndex + slides.length) % slides.length;
            currentIndex = safeIndex;
            track.style.transform = `translateX(-${safeIndex * 100}%)`;
            if (counter) counter.textContent = `${safeIndex + 1} / ${slides.length}`;
        };

        slider.querySelector('[data-action="slide-prev"]')?.addEventListener('click', (event) => {
            event.preventDefault();
            event.stopPropagation();
            moveTo(currentIndex - 1);
        });
        slider.querySelector('[data-action="slide-next"]')?.addEventListener('click', (event) => {
            event.preventDefault();
            event.stopPropagation();
            moveTo(currentIndex + 1);
        });

        let touchStartX = null;
        let touchStartY = null;
        slider.addEventListener('touchstart', (event) => {
            touchStartX = event.changedTouches?.[0]?.clientX ?? null;
            touchStartY = event.changedTouches?.[0]?.clientY ?? null;
        }, { passive: true });
        slider.addEventListener('touchend', (event) => {
            if (touchStartX == null || touchStartY == null) return;
            const endX = event.changedTouches?.[0]?.clientX ?? touchStartX;
            const endY = event.changedTouches?.[0]?.clientY ?? touchStartY;
            const delta = endX - touchStartX;
            const deltaY = endY - touchStartY;
            touchStartX = null;
            touchStartY = null;
            if (Math.abs(delta) < 30) return;
            if (Math.abs(delta) <= Math.abs(deltaY)) return;
            moveTo(delta > 0 ? currentIndex - 1 : currentIndex + 1);
        }, { passive: true });

        moveTo(0);
    };

    const bindCardEvents = (card) => {
        if (!(isSearchMode && state.searchMode === 'photo')) {
            initCardSlider(card);
        }
        card.addEventListener('click', (event) => {
            handleCardClick(event, card);
        });

        if (isSearchMode && state.searchMode === 'photo') {
            card.addEventListener('contextmenu', (event) => {
                event.preventDefault();
            });
            card.addEventListener('touchstart', () => {
                longPressTriggered = false;
                longPressTimer = window.setTimeout(() => {
                    longPressTriggered = true;
                    selectingPhotoMode = true;
                    togglePhotoSelect(card);
                }, 500);
            }, { passive: true });
            card.addEventListener('touchmove', () => {
                if (longPressTimer) window.clearTimeout(longPressTimer);
            }, { passive: true });
            card.addEventListener('touchend', () => {
                if (longPressTimer) window.clearTimeout(longPressTimer);
            }, { passive: true });
            return;
        }

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
            openCommentSheet(card.dataset.batchId);
        });
    };

    const buildParams = () => {
        const params = new URLSearchParams();
        params.set('page', String(page));
        params.set('size', String(pageSize));
        if (isSearchMode) {
            params.set('viewMode', state.searchMode);
        }
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
        const mediaItems = Array.isArray(item.mediaItems) && item.mediaItems.length > 0
            ? item.mediaItems
            : [{ mediaType: item.mediaType, smallUrl: item.thumbnailUrl, previewUrl: item.previewUrl }];

        const slideHtml = mediaItems.map((media) => {
            if (media.mediaType === 'video') {
                return `<div class="feed-slide"><video class="feed-preview-video" src="${media.previewUrl || ''}" poster="${media.smallUrl || item.thumbnailUrl || ''}" muted playsinline loop preload="none" data-has-preview="${media.previewUrl ? 'true' : 'false'}"></video></div>`;
            }
            return `<div class="feed-slide"><img src="${media.smallUrl || item.thumbnailUrl || ''}" alt="${escapeHtml(title)} 썸네일" loading="lazy"></div>`;
        }).join('');

        const sliderControl = mediaItems.length > 1
            ? `<button type="button" class="slider-nav prev" data-action="slide-prev" aria-label="이전 미디어">‹</button>
               <button type="button" class="slider-nav next" data-action="slide-next" aria-label="다음 미디어">›</button>
               <span class="slide-counter" data-slide-counter>1 / ${mediaItems.length}</span>`
            : '';

        const compactBadge = mediaItems.length > 1 ? `<span class="compact-batch-count" hidden>+${mediaItems.length - 1}</span>` : '';
        return `<article class="feed-card" data-media-type="${item.mediaType}" data-batch-id="${item.id}" data-detail-url="/feed/${item.id}">
            <a class="thumb-link" href="/feed/${item.id}" aria-label="${escapeHtml(title)} 상세보기">
                <div class="feed-slider" data-slider>
                    <div class="feed-slider-track" data-slider-track>
                        ${slideHtml}
                    </div>
                    ${sliderControl}
                </div>
                <span class="media-badge ${item.mediaType}" data-full-text="${mediaLabel}" data-short-text="${item.mediaType === 'video' ? 'V' : 'P'}">${mediaLabel}</span>
                ${item.recent ? `<span class="new-badge" data-full-text="New" data-short-text="N">New</span>` : ""}
                <div class="overlay-meta overlay-bottom"><p>${escapeHtml(item.author || '')}</p></div>
                ${compactBadge}
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

    const buildPhotoCardHtml = (item) => {
        const batchItems = escapeHtml(JSON.stringify(item.batchMediaItems || []));
        const moreCount = Math.max(0, Number(item.batchMediaCount || 0) - 1);
        return `<article class="feed-card photo-item-card" data-media-id="${item.id}" data-batch-id="${item.batchId}" data-batch-items='${batchItems}'>
            <a class="thumb-link" href="#" aria-label="${escapeHtml(item.title || '')}">
                <img src="${item.smallUrl || ''}" alt="${escapeHtml(item.title || '')}" loading="lazy">
                ${moreCount > 0 ? `<span class="compact-batch-count">+${moreCount}</span>` : ''}
            </a>
        </article>`;
    };

    const renderSearchViewer = () => {
        if (!searchViewerContent || photoViewerItems.length === 0) return;
        const item = photoViewerItems[photoViewerIndex];
        const mediaUrl = item.mediumUrl || item.smallUrl || '';
        const html = item.mediaType === 'video'
            ? `<video src="${mediaUrl}" controls autoplay playsinline></video>`
            : `<img src="${mediaUrl}" alt="viewer">`;
        searchViewerContent.innerHTML = html;
        if (searchViewerCurrent) searchViewerCurrent.textContent = String(photoViewerIndex + 1);
        if (searchViewerTotal) searchViewerTotal.textContent = String(photoViewerItems.length);
        if (searchViewerDownloadBtn) searchViewerDownloadBtn.href = `/feed/media/${item.mediaId}/download`;
    };

    const openSearchViewer = (items, index) => {
        photoViewerItems = items || [];
        photoViewerIndex = Math.max(0, index || 0);
        if (!searchViewerBackdrop || photoViewerItems.length === 0) return;
        if (!searchViewerHistoryActive) {
            history.pushState({ searchViewerOpen: true }, '', window.location.href);
            searchViewerHistoryActive = true;
        }
        searchViewerBackdrop.hidden = false;
        document.body.classList.add('modal-open');
        renderSearchViewer();
    };

    const closeSearchViewer = () => {
        if (!searchViewerBackdrop) return;
        searchViewerBackdrop.hidden = true;
        document.body.classList.remove('modal-open');
        searchViewerHistoryActive = false;
    };

    const togglePhotoSelect = (card) => {
        const mediaId = card.dataset.mediaId;
        if (!mediaId) return;
        if (selectedPhotoIds.has(mediaId)) selectedPhotoIds.delete(mediaId);
        else selectedPhotoIds.add(mediaId);
        if (selectedPhotoIds.size === 0) selectingPhotoMode = false;
        card.classList.toggle('is-selected', selectedPhotoIds.has(mediaId));
        if (searchSelectedCount) searchSelectedCount.textContent = String(selectedPhotoIds.size);
        if (searchSelectionBar) searchSelectionBar.hidden = !selectingPhotoMode;
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
        const card = grid.querySelector(`.feed-card[data-batch-id="${mediaId}"]`);
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
        const mediaId = card.dataset.batchId;
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
                const cardBuilder = isSearchMode && state.searchMode === 'photo' ? buildPhotoCardHtml : buildCardHtml;
                grid.classList.toggle('is-photo-mode', isSearchMode && state.searchMode === 'photo');
                grid.insertAdjacentHTML('beforeend', items.map(cardBuilder).join(''));
                Array.from(grid.querySelectorAll('.feed-card')).slice(-items.length).forEach(bindCardEvents);
                applyColumn(state.columns);
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
    searchModeButtons.forEach((button) => button.addEventListener('click', () => {
        searchModeButtons.forEach((b) => b.classList.remove('is-active'));
        button.classList.add('is-active');
        state.searchMode = button.dataset.searchMode || 'feed';
        selectingPhotoMode = false;
        selectedPhotoIds.clear();
        if (searchSelectionBar) searchSelectionBar.hidden = true;
        reloadFromFirstPage();
    }));
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
        if (!calendarCard) return;

        const monthLabel = document.getElementById('calendarMonthLabel');
        const prevBtn = document.getElementById('calendarPrevBtn');
        const nextBtn = document.getElementById('calendarNextBtn');
        const contentArea = document.getElementById('calendarContentArea');

        let state = calendarCard.dataset.calendarState || 'DISABLED';
        let selectedDate = null;

        const zeroPad = (num) => String(num).padStart(2, '0');
        const formatMonth = (year, month) => `${year}.${zeroPad(month)}`;
        const formatKoreanDate = (isoDate) => {
            const [, m, d] = isoDate.split('-').map(Number);
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

        const getStateMessage = (calendarState) => {
            if (calendarState === 'NO_SOURCES') return '등록된 캘린더가 없습니다.';
            if (calendarState === 'ERROR') return '일정을 불러오지 못했습니다.';
            return '캘린더 기능이 비활성화되어 있습니다.';
        };

        const renderEventItems = (events, useDday, emptyText) => {
            if (!events || events.length === 0) {
                return `<li class="empty">${emptyText}</li>`;
            }
            return events.map((event) => `
                <li>
                    <span class="event-time">${useDday ? `${calcDday(event.date)} · ` : ''}${event.timeText}</span>
                    <span class="event-title">${escapeHtml(event.title)}</span>
                </li>
            `).join('');
        };

        const renderCalendar = (monthData) => {
            if (!monthData) {
                contentArea.innerHTML = `<p class="calendar-empty-msg">${getStateMessage(state)}</p>`;
                return;
            }

            const dayButtonsHtml = (monthData.days || []).map((day) => {
                const dayOfWeek = new Date(`${day.date}T00:00:00`).getDay();
                const isSaturday = dayOfWeek === 6;
                const classes = [
                    'calendar-day',
                    day.currentMonth ? '' : 'is-outside',
                    day.today ? 'is-today' : '',
                    (day.sunday || day.holiday) ? 'is-holiday-text' : '',
                    day.currentMonth && isSaturday && !day.holiday ? 'is-saturday-soft' : '',
                    selectedDate === day.date ? 'is-selected' : ''
                ].filter(Boolean).join(' ');

                const personalDot = day.hasPersonalEvent ? '<i class="dot dot-personal"></i>' : '';
                const holidayDot = day.holiday ? '<i class="dot dot-holiday"></i>' : '';
                return `
                    <button type="button" class="${classes}" data-date="${day.date}">
                        <span class="day-number">${day.dayNumber}</span>
                        <span class="event-dots">${personalDot}${holidayDot}</span>
                    </button>
                `;
            }).join('');

            const upcomingEvents = (monthData.upcomingEvents || []);
            const panelTitle = selectedDate ? `${formatKoreanDate(selectedDate)} 일정` : '다가오는 일정';
            const panelEvents = selectedDate
                ? ((monthData.days || []).find((day) => day.date === selectedDate)?.events || [])
                : upcomingEvents;
            const panelList = renderEventItems(panelEvents, !selectedDate, selectedDate ? '일정이 없습니다.' : '다가오는 일정이 없습니다.');
            const panelClass = (panelEvents.length > 3) ? 'calendar-event-list is-scrollable' : 'calendar-event-list';

            contentArea.innerHTML = `
                <div class="calendar-week-head">
                    <span>일</span><span>월</span><span>화</span><span>수</span><span>목</span><span>금</span><span>토</span>
                </div>
                <div class="calendar-grid" id="calendarGrid">${dayButtonsHtml}</div>
                <section class="calendar-event-panel" id="calendarEventPanel">
                    <header class="calendar-event-header" id="calendarEventHeader">${panelTitle}</header>
                    <button type="button" class="calendar-close-btn" id="calendarCloseBtn" ${selectedDate ? '' : 'hidden'}>닫기</button>
                    <ul class="${panelClass}" id="calendarEventList">${panelList}</ul>
                </section>
            `;

            contentArea.querySelectorAll('.calendar-day[data-date]').forEach((button) => {
                button.addEventListener('click', () => {
                    const clickedDate = button.dataset.date;
                    if (selectedDate === clickedDate) {
                        selectedDate = null;
                    } else {
                        selectedDate = clickedDate;
                    }
                    renderCalendar(monthData);
                });
            });

            contentArea.querySelector('#calendarCloseBtn')?.addEventListener('click', () => {
                selectedDate = null;
                renderCalendar(monthData);
            });
        };

        const renderByState = (monthData) => {
            if (state !== 'READY') {
                selectedDate = null;
                contentArea.innerHTML = `<p class="calendar-empty-msg">${getStateMessage(state)}</p>`;
                return;
            }
            renderCalendar(monthData);
        };

        let currentYear = Number(calendarCard.dataset.calendarYear || 0);
        let currentMonth = Number(calendarCard.dataset.calendarMonth || 0);
        let currentMonthData = null;

        const monthDataNode = document.getElementById('calendarEventPanel');
        if (state === 'READY' && monthDataNode?.dataset.calendarMonth) {
            try {
                currentMonthData = JSON.parse(monthDataNode.dataset.calendarMonth || '{}');
            } catch (_) {
                state = 'ERROR';
            }
        }

        if (monthLabel && currentYear > 0 && currentMonth > 0) {
            monthLabel.textContent = formatMonth(currentYear, currentMonth);
        }
        renderByState(currentMonthData);

        const setNavDisabled = (disabled) => {
            if (prevBtn) prevBtn.disabled = disabled;
            if (nextBtn) nextBtn.disabled = disabled;
        };

        const loadMonthAsync = async (nextYear, nextMonth) => {
            setNavDisabled(true);
            try {
                const params = new URLSearchParams({ year: String(nextYear), month: String(nextMonth) });
                const response = await fetch(`/api/calendar/month?${params.toString()}`);
                if (!response.ok) throw new Error('calendar-load-failed');
                const payload = await response.json();

                currentYear = Number(payload.year || nextYear);
                currentMonth = Number(payload.month || nextMonth);
                state = payload.state || 'ERROR';
                currentMonthData = payload.monthData || null;
                selectedDate = null;

                if (monthLabel) {
                    monthLabel.textContent = formatMonth(currentYear, currentMonth);
                }
                renderByState(currentMonthData);
            } catch (_) {
                state = 'ERROR';
                currentMonthData = null;
                renderByState(currentMonthData);
            } finally {
                setNavDisabled(false);
            }
        };

        prevBtn?.addEventListener('click', () => {
            const base = new Date(currentYear, currentMonth - 1, 1);
            base.setMonth(base.getMonth() - 1);
            loadMonthAsync(base.getFullYear(), base.getMonth() + 1);
        });

        nextBtn?.addEventListener('click', () => {
            const base = new Date(currentYear, currentMonth - 1, 1);
            base.setMonth(base.getMonth() + 1);
            loadMonthAsync(base.getFullYear(), base.getMonth() + 1);
        });
    };

    getCards().forEach(bindCardEvents);
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

    searchViewerCloseBtn?.addEventListener('click', closeSearchViewer);
    searchViewerBackdrop?.addEventListener('click', (event) => {
        if (event.target === searchViewerBackdrop) closeSearchViewer();
    });
    window.addEventListener('popstate', () => {
        if (searchViewerBackdrop && !searchViewerBackdrop.hidden) {
            closeSearchViewer();
        }
    });
    searchViewerPrevBtn?.addEventListener('click', () => {
        if (photoViewerItems.length === 0) return;
        photoViewerIndex = (photoViewerIndex - 1 + photoViewerItems.length) % photoViewerItems.length;
        renderSearchViewer();
    });
    searchViewerNextBtn?.addEventListener('click', () => {
        if (photoViewerItems.length === 0) return;
        photoViewerIndex = (photoViewerIndex + 1) % photoViewerItems.length;
        renderSearchViewer();
    });
    searchCancelSelectBtn?.addEventListener('click', () => {
        selectingPhotoMode = false;
        selectedPhotoIds.clear();
        getCards().forEach((card) => card.classList.remove('is-selected'));
        if (searchSelectionBar) searchSelectionBar.hidden = true;
        if (searchSelectedCount) searchSelectedCount.textContent = '0';
    });
    searchDownloadSelectBtn?.addEventListener('click', async () => {
        const mediaIds = Array.from(selectedPhotoIds, (value) => Number(value));
        if (mediaIds.length === 0) return;
        if (mediaIds.length === 1) {
            window.location.href = `/feed/media/${mediaIds[0]}/download`;
            return;
        }
        const res = await fetch('/feed/download-zip', {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify({mediaIds})
        });
        if (!res.ok) return;
        const blob = await res.blob();
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = 'memorybox_selected.zip';
        a.click();
        URL.revokeObjectURL(url);
    });
});
