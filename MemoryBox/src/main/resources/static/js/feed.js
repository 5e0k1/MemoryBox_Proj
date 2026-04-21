/**
 * 피드 목업 단계용 UI 스크립트.
 * - 미디어 타입 탭 상태 전환
 * - 열 수(1/3/5) 레이아웃 전환
 * - 모바일 길게 누르기 기반 다중 선택 모드 UI
 * - 태그 다중 선택 표시 UI
 */
document.addEventListener('DOMContentLoaded', () => {
    const MAX_SELECTION_COUNT = 30;
    const DOWNLOAD_API_URL = '/feed/download-zip';
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
        let ignoreContextMenuUntil = 0;
        const thumbLink = card.querySelector('.thumb-link');

        card.addEventListener('contextmenu', (event) => {
            event.preventDefault();

            if (Date.now() < ignoreContextMenuUntil) {
                return;
            }

            if (!selectionMode) {
                enterSelectionMode();
            }
            toggleCardSelection(card);
        });

        card.addEventListener('touchstart', () => {
            longPressTriggered = false;
            longPressTimer = setTimeout(() => {
                longPressTriggered = true;
                ignoreContextMenuUntil = Date.now() + 800;
                if (!selectionMode) {
                    enterSelectionMode();
                }
                if (!card.classList.contains('is-selected')) {
                    toggleCardSelection(card);
                }
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

    const setDownloadButtonLoading = (loading) => {
        downloadSelectedBtn.disabled = loading;
        downloadSelectedBtn.textContent = loading ? '다운로드 준비중...' : '다운로드';
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

    const extractDownloadFileName = (contentDispositionHeader) => {
        if (!contentDispositionHeader) {
            return '';
        }

        const utf8Match = contentDispositionHeader.match(/filename\*=UTF-8''([^;]+)/i);
        if (utf8Match && utf8Match[1]) {
            return decodeURIComponent(utf8Match[1].trim());
        }

        const plainMatch = contentDispositionHeader.match(/filename="?([^\";]+)"?/i);
        if (plainMatch && plainMatch[1]) {
            return plainMatch[1].trim();
        }

        return '';
    };

    downloadSelectedBtn.addEventListener('click', async () => {
        if (selectedIds.size === 0) {
            window.alert('다운로드할 파일을 먼저 선택해 주세요.');
            return;
        }

        setDownloadButtonLoading(true);
        try {
            const response = await fetch(DOWNLOAD_API_URL, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({
                    mediaIds: Array.from(selectedIds, (id) => Number(id))
                })
            });

            if (!response.ok) {
                let message = '다중 다운로드 처리 중 오류가 발생했습니다.';
                try {
                    const responseText = await response.text();
                    if (responseText) {
                        try {
                            const errorPayload = JSON.parse(responseText);
                            if (errorPayload && errorPayload.message) {
                                message = errorPayload.message;
                            } else {
                                message = responseText;
                            }
                        } catch (ignore) {
                            message = responseText;
                        }
                    }
                } catch (ignore) {
                    // 오류 본문 파싱 실패 시 기본 메시지 사용
                }
                window.alert(message);
                return;
            }

            const contentDisposition = response.headers.get('Content-Disposition') || '';
            const decodedFileName = extractDownloadFileName(contentDisposition) || 'memorybox_download.zip';
            const blob = await response.blob();
            startDownload(blob, decodedFileName);
        } catch (error) {
            window.alert('네트워크 오류로 다운로드에 실패했습니다. 잠시 후 다시 시도해 주세요.');
        } finally {
            setDownloadButtonLoading(false);
        }
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
