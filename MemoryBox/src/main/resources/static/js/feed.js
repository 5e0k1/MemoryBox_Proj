/**
 * 피드 목업 단계용 UI 스크립트.
 * - 미디어 타입 탭 상태 전환
 * - 열 수(1/3/5) 레이아웃 전환
 * - 길게 누르기/우클릭 시 추후 컨텍스트 메뉴 연결을 위한 훅
 */
document.addEventListener('DOMContentLoaded', () => {
    const grid = document.getElementById('feedGrid');
    const tabButtons = document.querySelectorAll('.tab-btn');
    const colButtons = document.querySelectorAll('.col-btn');
    const cards = document.querySelectorAll('.feed-card');

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
        });
    });

    cards.forEach((card) => {
        let longPressTimer;

        card.addEventListener('contextmenu', (event) => {
            event.preventDefault();
            console.info('추후 다운로드/링크 메뉴 연결 예정, itemId:', card.dataset.itemId);
        });

        card.addEventListener('touchstart', () => {
            longPressTimer = setTimeout(() => {
                console.info('모바일 길게 누르기 감지 - 추후 메뉴 연결 예정, itemId:', card.dataset.itemId);
            }, 600);
        });

        card.addEventListener('touchend', () => clearTimeout(longPressTimer));
        card.addEventListener('touchcancel', () => clearTimeout(longPressTimer));
    });
});
