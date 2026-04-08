/**
 * 피드 목업 단계용 UI 스크립트.
 * - 미디어 타입 탭 상태 전환
 * - 열 수(1/3/5) 레이아웃 전환
 * - 모바일 길게 누르기 기반 다중 선택 모드 UI
 * - 태그 다중 선택 표시 UI
 */
document.addEventListener('DOMContentLoaded', () => {
    const grid = document.getElementById('feedGrid');
    const tabButtons = document.querySelectorAll('.tab-btn');
    const colButtons = document.querySelectorAll('.col-btn');
    const cards = document.querySelectorAll('.feed-card');
    const mediaBadges = document.querySelectorAll('.media-badge');
    const mobileSelectionBar = document.getElementById('mobileSelectionBar');
    const selectedCountText = document.getElementById('selectedCount');
    const cancelSelectionBtn = document.getElementById('cancelSelectionBtn');
    const downloadSelectedBtn = document.getElementById('downloadSelectedBtn');
    const tagChecks = document.querySelectorAll('.tag-check');
    const selectedTagText = document.getElementById('selectedTagText');

    let selectionMode = false;
    const selectedIds = new Set();

    const updateSelectionUI = () => {
        selectedCountText.textContent = String(selectedIds.size);
        mobileSelectionBar.hidden = !selectionMode;
    };

    const updateBadgeLabels = (columns) => {
        const shortMode = columns === '3' || columns === '5';
        mediaBadges.forEach((badge) => {
            badge.textContent = shortMode ? badge.dataset.shortText : badge.dataset.fullText;
        });
    };

    const enterSelectionMode = () => {
        selectionMode = true;
        updateSelectionUI();
    };

    const clearSelectionMode = () => {
        selectionMode = false;
        selectedIds.clear();
        cards.forEach((card) => card.classList.remove('is-selected'));
        updateSelectionUI();
    };

    const toggleCardSelection = (card) => {
        const id = card.dataset.itemId;
        if (selectedIds.has(id)) {
            selectedIds.delete(id);
            card.classList.remove('is-selected');
        } else {
            selectedIds.add(id);
            card.classList.add('is-selected');
        }

        if (selectedIds.size === 0) {
            clearSelectionMode();
            return;
        }

        updateSelectionUI();
    };

    tabButtons.forEach((button) => {
        button.addEventListener('click', () => {
            tabButtons.forEach((b) => b.classList.remove('is-active'));
            button.classList.add('is-active');
            const type = button.dataset.filterType;

            cards.forEach((card) => {
                const matched = type === 'all' || card.dataset.mediaType === type;
                card.style.display = matched ? '' : 'none';
            });
        });
    });

    colButtons.forEach((button) => {
        button.addEventListener('click', () => {
            colButtons.forEach((b) => b.classList.remove('is-active'));
            button.classList.add('is-active');
            const columns = button.dataset.columns;
            grid.classList.remove('columns-1', 'columns-3', 'columns-5');
            grid.classList.add(`columns-${columns}`);
            updateBadgeLabels(columns);
        });
    });

    cards.forEach((card) => {
        let longPressTimer;
        let longPressTriggered = false;
        const thumbLink = card.querySelector('.thumb-link');

        card.addEventListener('contextmenu', (event) => {
            event.preventDefault();
            if (!selectionMode) {
                enterSelectionMode();
            }
            toggleCardSelection(card);
        });

        card.addEventListener('touchstart', () => {
            longPressTriggered = false;
            longPressTimer = setTimeout(() => {
                longPressTriggered = true;
                if (!selectionMode) {
                    enterSelectionMode();
                }
                toggleCardSelection(card);
            }, 500);
        }, { passive: true });

        card.addEventListener('touchmove', () => {
            clearTimeout(longPressTimer);
        }, { passive: true });

        card.addEventListener('touchend', () => {
            clearTimeout(longPressTimer);
            if (longPressTriggered) {
                thumbLink.dataset.ignoreClickOnce = 'true';
            }
        });

        card.addEventListener('touchcancel', () => {
            clearTimeout(longPressTimer);
            longPressTriggered = false;
        });

        thumbLink.addEventListener('click', (event) => {
            if (thumbLink.dataset.ignoreClickOnce === 'true') {
                thumbLink.dataset.ignoreClickOnce = 'false';
                event.preventDefault();
                return;
            }

            if (!selectionMode) {
                return;
            }

            event.preventDefault();
            toggleCardSelection(card);
        });
    });

    cancelSelectionBtn.addEventListener('click', clearSelectionMode);

    downloadSelectedBtn.addEventListener('click', () => {
        console.info('다중 선택 다운로드(추후 서버 연결):', Array.from(selectedIds));
    });

    const updateTagSummary = () => {
        const selectedTags = Array.from(tagChecks)
            .filter((check) => check.checked)
            .map((check) => check.value);

        if (selectedTags.length === 0) {
            selectedTagText.textContent = '전체 태그';
            return;
        }

        if (selectedTags.length <= 2) {
            selectedTagText.textContent = selectedTags.map((tag) => `#${tag}`).join(', ');
            return;
        }

        selectedTagText.textContent = `${selectedTags.slice(0, 2).map((tag) => `#${tag}`).join(', ')} 외 ${selectedTags.length - 2}`;
    };

    tagChecks.forEach((check) => check.addEventListener('change', updateTagSummary));
    updateBadgeLabels('1');
    updateTagSummary();
    updateSelectionUI();
});
