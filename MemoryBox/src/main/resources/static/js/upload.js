(function () {
    const input = document.getElementById('multiImageInput');
    const previewList = document.getElementById('previewList');
    if (!input || !previewList) return;

    input.addEventListener('change', function () {
        previewList.innerHTML = '';
        const files = Array.from(input.files || []);
        files.forEach(function (file, idx) {
            const wrapper = document.createElement('div');
            wrapper.className = 'preview-item';

            const image = document.createElement('img');
            image.alt = file.name;
            image.src = URL.createObjectURL(file);

            const right = document.createElement('div');
            const name = document.createElement('div');
            name.textContent = file.name;
            const tagInput = document.createElement('input');
            tagInput.type = 'text';
            tagInput.name = 'fileTags';
            tagInput.placeholder = '파일별 태그 입력 (쉼표 구분)';
            tagInput.setAttribute('data-index', String(idx));

            right.appendChild(name);
            right.appendChild(tagInput);
            wrapper.appendChild(image);
            wrapper.appendChild(right);
            previewList.appendChild(wrapper);
        });
    });
})();
