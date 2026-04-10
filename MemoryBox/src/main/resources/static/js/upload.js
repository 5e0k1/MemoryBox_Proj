(function () {
    initPreview();
    initTagWidgets();
    initTakenAtDefault();

    function initPreview() {
        const input = document.getElementById('multiImageInput');
        const previewList = document.getElementById('previewList');
        if (!input || !previewList) return;

        input.addEventListener('change', function () {
            previewList.innerHTML = '';
            const files = Array.from(input.files || []);
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

    function initTagWidgets() {
        const widgets = document.querySelectorAll('[data-widget="tag-picker"]');
        widgets.forEach(function (widget) {
            const select = widget.querySelector('.tag-select');
            const addInput = widget.querySelector('.tag-add-input');
            const addBtn = widget.querySelector('.tag-add-btn');
            const hiddenNewTags = widget.querySelector('.new-tags-hidden');
            const pillList = widget.querySelector('.tag-pill-list');
            const count = widget.querySelector('.tag-count');
            if (!select || !addInput || !addBtn || !hiddenNewTags || !pillList || !count) return;

            const customTags = new Set();
            hydrateCustomTags(hiddenNewTags.value);
            refreshCount();

            select.addEventListener('change', refreshCount);
            addBtn.addEventListener('click', function () {
                addNewTag(addInput.value);
                addInput.value = '';
                addInput.focus();
            });
            addInput.addEventListener('keydown', function (event) {
                if (event.key === 'Enter') {
                    event.preventDefault();
                    addBtn.click();
                }
            });

            function hydrateCustomTags(raw) {
                if (!raw) return;
                raw.split(',').map(function (x) { return x.trim(); }).filter(Boolean).forEach(addCustomTag);
            }

            function addNewTag(rawValue) {
                const tagName = (rawValue || '').trim();
                if (!tagName) return;

                const normalized = normalize(tagName);
                const options = Array.from(select.options || []);
                const existingOption = options.find(function (opt) {
                    return normalize(opt.textContent || '') === normalized;
                });

                if (existingOption) {
                    existingOption.selected = true;
                    refreshCount();
                    return;
                }
                addCustomTag(tagName);
            }

            function addCustomTag(tagName) {
                const normalized = normalize(tagName);
                if (!normalized || customTags.has(normalized)) return;
                customTags.add(normalized);
                renderPill(tagName, normalized);
                syncHidden();
                refreshCount();
            }

            function renderPill(label, key) {
                const pill = document.createElement('span');
                pill.className = 'tag-pill';
                pill.dataset.tagKey = key;
                pill.innerHTML = '<span>' + escapeHtml(label) + '</span><button type="button" aria-label="삭제">✕</button>';
                const removeBtn = pill.querySelector('button');
                removeBtn.addEventListener('click', function () {
                    customTags.delete(key);
                    pill.remove();
                    syncHidden();
                    refreshCount();
                });
                pillList.appendChild(pill);
            }

            function syncHidden() {
                const labels = Array.from(pillList.querySelectorAll('.tag-pill > span')).map(function (node) {
                    return (node.textContent || '').trim();
                }).filter(Boolean);
                hiddenNewTags.value = labels.join(',');
            }

            function refreshCount() {
                const selectedExisting = Array.from(select.selectedOptions || []).length;
                const selectedTotal = selectedExisting + customTags.size;
                count.textContent = '선택 ' + selectedTotal + '개';
            }
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

    function normalize(value) {
        return (value || '').trim().toLowerCase().replace(/\s+/g, ' ');
    }

    function escapeHtml(value) {
        return String(value)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#39;');
    }
})();
