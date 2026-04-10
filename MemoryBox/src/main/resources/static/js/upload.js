(function () {
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
})();
