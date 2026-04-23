(function () {
    initPreview();
    initTagWidgets();
    initAlbumWidgets();
    initTakenAtDefault();
    initUploadSubmission();

    function initPreview() {
        const multiInput = document.getElementById('multiImageInput');
        const previewList = document.getElementById('previewList');
        const singleInput = document.getElementById('singleImageInput');
        const singlePreview = document.getElementById('singlePreview');

        if (multiInput && previewList) {
            multiInput.addEventListener('change', function () {
                previewList.innerHTML = '';
                const files = Array.from(multiInput.files || []);
                files.forEach(function (file) {
                    const wrapper = document.createElement('div');
                    wrapper.className = 'preview-item';

                    const image = document.createElement('img');
                    image.alt = file.name;
                    image.src = URL.createObjectURL(file);

                    const right = document.createElement('div');
                    const name = document.createElement('div');
                    name.textContent = file.name;

                    right.appendChild(name);
                    wrapper.appendChild(image);
                    wrapper.appendChild(right);
                    previewList.appendChild(wrapper);
                });
            });
        }

        if (singleInput && singlePreview) {
            singleInput.addEventListener('change', function () {
                singlePreview.innerHTML = '';
                const file = (singleInput.files || [])[0];
                if (!file) {
                    return;
                }

                const wrapper = document.createElement('div');
                wrapper.className = 'preview-item single-preview-item';

                const image = document.createElement('img');
                image.alt = file.name;
                image.src = URL.createObjectURL(file);

                wrapper.appendChild(image);
                singlePreview.appendChild(wrapper);
            });
        }
    }

    function initTagWidgets() {
        const widgets = document.querySelectorAll('[data-widget="tag-picker"]');
        widgets.forEach(function (widget) {
            const optionList = widget.querySelector('.tag-option-list');
            const addInput = widget.querySelector('.tag-add-input');
            const addBtn = widget.querySelector('.tag-add-btn');
            const hiddenNewTags = widget.querySelector('.new-tags-hidden');
            const count = widget.querySelector('.tag-count');
            const createUrl = widget.dataset.createUrl || '';
            if (!optionList || !addInput || !addBtn || !hiddenNewTags || !count) return;

            hiddenNewTags.value = '';
            refreshCount();

            optionList.querySelectorAll('.tag-check').forEach(function (checkbox) {
                checkbox.addEventListener('change', refreshCount);
            });

            addBtn.addEventListener('click', async function () {
                await addNewTag(addInput.value);
                addInput.value = '';
                addInput.focus();
            });

            addInput.addEventListener('keydown', function (event) {
                if (event.key === 'Enter') {
                    event.preventDefault();
                    addBtn.click();
                }
            });

            async function addNewTag(rawValue) {
                const tagName = (rawValue || '').trim();
                if (!tagName) return;

                const normalized = normalize(tagName);
                const existing = findOptionByNormalized(normalized);
                if (existing) {
                    const input = existing.querySelector('input[type="checkbox"]');
                    if (input) {
                        input.checked = true;
                        input.dispatchEvent(new Event('change'));
                    }
                    return;
                }

                if (!createUrl) {
                    return;
                }

                try {
                    addBtn.disabled = true;
                    const response = await fetch(createUrl, {
                        method: 'POST',
                        headers: {
                            'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8'
                        },
                        body: 'tagName=' + encodeURIComponent(tagName)
                    });

                    if (!response.ok) {
                        alert('태그 추가 중 오류가 발생했습니다.');
                        return;
                    }

                    const data = await response.json();
                    if (!data.success) {
                        alert(data.message || '태그를 추가할 수 없습니다.');
                        return;
                    }

                    appendServerTagOption(data.tagId, data.tagName, data.normalizedName, true);
                    refreshCount();
                } catch (error) {
                    alert('태그 추가 중 오류가 발생했습니다.');
                } finally {
                    addBtn.disabled = false;
                }
            }

            function appendServerTagOption(tagId, tagName, normalizedName, checked) {
                const normalized = normalize(normalizedName || tagName);
                if (!normalized || findOptionByNormalized(normalized)) return;

                const wrapper = document.createElement('label');
                wrapper.className = 'tag-option';
                wrapper.dataset.normalized = normalized;

                const checkbox = document.createElement('input');
                checkbox.type = 'checkbox';
                checkbox.className = 'tag-check';
                checkbox.name = 'selectedTagIds';
                checkbox.value = String(tagId);
                checkbox.checked = !!checked;
                checkbox.addEventListener('change', refreshCount);

                const text = document.createElement('span');
                text.className = 'tag-label';
                text.textContent = tagName;

                wrapper.appendChild(checkbox);
                wrapper.appendChild(text);
                optionList.prepend(wrapper);
            }

            function findOptionByNormalized(normalized) {
                const options = optionList.querySelectorAll('.tag-option');
                return Array.from(options).find(function (option) {
                    const source = option.dataset.normalized
                        || (option.querySelector('.tag-label') ? option.querySelector('.tag-label').textContent : '');
                    return normalize(source) === normalized;
                });
            }

            function refreshCount() {
                const existingChecked = optionList.querySelectorAll('.tag-check:checked').length;
                count.textContent = '선택 ' + existingChecked + '개';
            }
        });
    }



    function initAlbumWidgets() {
        const widgets = document.querySelectorAll('[data-widget="album-picker"]');
        widgets.forEach(function (widget) {
            const select = widget.querySelector('.album-select');
            const openBtn = widget.querySelector('.album-add-btn');
            const form = widget.closest('form');
            if (!select || !openBtn || !form) return;

            const modal = form.querySelector('.album-modal-backdrop');
            const input = modal?.querySelector('.album-add-input');
            const createBtn = modal?.querySelector('.album-create-btn');
            const cancelBtn = modal?.querySelector('.album-cancel-btn');
            const errorBox = modal?.querySelector('.album-modal-error');
            const createUrl = widget.dataset.createUrl || '';
            if (!modal || !input || !createBtn || !cancelBtn || !errorBox || !createUrl) return;

            let modalOpen = false;

            const showError = function (message) {
                errorBox.textContent = message || '앨범 생성 중 오류가 발생했습니다.';
                errorBox.hidden = false;
            };

            const openModal = function () {
                if (modalOpen) return;
                modal.hidden = false;
                modalOpen = true;
                errorBox.hidden = true;
                history.pushState({ albumModal: true }, '', location.href);
                setTimeout(function () { input.focus(); }, 0);
            };

            const closeModal = function (consumeHistory) {
                if (!modalOpen) return;
                modal.hidden = true;
                modalOpen = false;
                input.value = '';
                errorBox.hidden = true;
                if (consumeHistory) {
                    history.back();
                }
            };

            openBtn.addEventListener('click', openModal);

            cancelBtn.addEventListener('click', function () {
                closeModal(true);
            });

            createBtn.addEventListener('click', async function () {
                const albumName = (input.value || '').trim();
                if (!albumName) {
                    showError('앨범명을 입력해 주세요.');
                    input.focus();
                    return;
                }

                try {
                    createBtn.disabled = true;
                    errorBox.hidden = true;
                    const response = await fetch(createUrl, {
                        method: 'POST',
                        headers: {
                            'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8'
                        },
                        body: 'albumName=' + encodeURIComponent(albumName)
                    });

                    const data = await response.json();
                    if (!response.ok || !data.success) {
                        showError(data.message || '앨범 생성에 실패했습니다.');
                        return;
                    }

                    const option = document.createElement('option');
                    option.value = String(data.albumId);
                    option.textContent = data.albumName;
                    select.appendChild(option);
                    select.value = option.value;
                    closeModal(true);
                } catch (error) {
                    showError('앨범 생성 중 오류가 발생했습니다.');
                } finally {
                    createBtn.disabled = false;
                }
            });

            window.addEventListener('popstate', function () {
                if (!modalOpen) return;
                modal.hidden = true;
                modalOpen = false;
                input.value = '';
                errorBox.hidden = true;
            });
        });
    }
    function initTakenAtDefault() {
        const inputs = document.querySelectorAll('.taken-at-input');
        if (!inputs.length) return;

        const now = new Date();
        const yyyy = now.getFullYear();
        const mm = String(now.getMonth() + 1).padStart(2, '0');
        const dd = String(now.getDate()).padStart(2, '0');
        const defaultValue = yyyy + '-' + mm + '-' + dd + 'T00:00';

        inputs.forEach(function (input) {
            if (!input.value) {
                input.value = defaultValue;
            }
        });
    }

    function initUploadSubmission() {
        const forms = document.querySelectorAll('.upload-form');
        if (!forms.length) return;

        forms.forEach(function (form) {
            form.addEventListener('submit', function (event) {
                event.preventDefault();
                submitWithProgress(form);
            });
        });
    }

    function submitWithProgress(form) {
        const overlay = createProgressOverlay();
        lockPage(overlay);
        updateProgress(overlay, 0, '업로드 준비 중...');

        const xhr = new XMLHttpRequest();
        xhr.open((form.method || 'POST').toUpperCase(), form.action || window.location.pathname, true);
        xhr.setRequestHeader('X-Requested-With', 'XMLHttpRequest');
        xhr.setRequestHeader('Accept', 'application/json');

        xhr.upload.addEventListener('progress', function (event) {
            if (!event.lengthComputable) {
                updateProgress(overlay, 0, '업로드 중...');
                return;
            }
            const percent = Math.min(99, Math.round((event.loaded / event.total) * 100));
            updateProgress(overlay, percent, '업로드 중... ' + percent + '%');
        });

        xhr.addEventListener('load', function () {
            let payload = {};
            try {
                payload = JSON.parse(xhr.responseText || '{}');
            } catch (ignore) {
                payload = {};
            }

            if (xhr.status >= 200 && xhr.status < 300 && payload.success) {
                updateProgress(overlay, 100, '처리 완료');
                showCompletionAlert(payload.message || '정상적으로 완료되었습니다');
                setTimeout(function () {
                    window.location.href = payload.redirectUrl || '/feed';
                }, 1000);
                return;
            }

            const errorMessage = payload.message || '업로드 처리 중 오류가 발생했습니다.';
            unlockPage();
            alert(errorMessage);
        });

        xhr.addEventListener('error', function () {
            unlockPage();
            alert('업로드 중 네트워크 오류가 발생했습니다.');
        });

        xhr.send(new FormData(form));
    }

    function createProgressOverlay() {
        const overlay = document.createElement('div');
        overlay.className = 'upload-progress-overlay';
        overlay.innerHTML = '' +
            '<div class="upload-progress-panel">' +
            '<strong>업로드 진행 중</strong>' +
            '<p class="upload-progress-text">업로드 준비 중...</p>' +
            '<div class="upload-progress-track"><div class="upload-progress-fill" style="width:0%"></div></div>' +
            '</div>';
        return overlay;
    }

    function updateProgress(overlay, percent, message) {
        const fill = overlay.querySelector('.upload-progress-fill');
        const text = overlay.querySelector('.upload-progress-text');
        if (fill) fill.style.width = Math.max(0, Math.min(percent, 100)) + '%';
        if (text) text.textContent = message;
    }

    function lockPage(overlay) {
        document.body.classList.add('upload-busy');
        document.body.appendChild(overlay);
    }

    function unlockPage() {
        document.body.classList.remove('upload-busy');
        const overlay = document.querySelector('.upload-progress-overlay');
        if (overlay) overlay.remove();
        const alert = document.querySelector('.upload-complete-alert');
        if (alert) alert.remove();
    }

    function showCompletionAlert(message) {
        const notice = document.createElement('div');
        notice.className = 'upload-complete-alert';
        notice.innerHTML = '' +
            '<div class="upload-complete-card">' +
            '<div class="icon">✓</div>' +
            '<p>' + escapeHtml(message) + '</p>' +
            '</div>';
        document.body.appendChild(notice);
    }

    function escapeHtml(raw) {
        return (raw || '')
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#39;');
    }

    function normalize(value) {
        return (value || '').trim().toLowerCase().replace(/\s+/g, ' ');
    }
})();
