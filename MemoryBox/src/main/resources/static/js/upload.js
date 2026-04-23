(function () {
    initPreview();
    initTagWidgets();
    initTakenAtDefault();

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
})();