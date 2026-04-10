<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>MemoryBox - 메인 피드</title>
    <link rel="stylesheet" href="/css/common.css">
    <link rel="stylesheet" href="/css/feed.css">
</head>
<body class="page page-feed">
<main class="feed-layout">
    <header class="feed-header">
        <h1>MemoryBox Feed</h1>
        <div class="header-actions">
            <span class="login-user">${loginUser.displayName}</span>
            <button class="icon-btn icon-settings" type="button" id="openPasswordModalBtn" aria-label="비밀번호 변경">
                <svg viewBox="0 0 24 24" aria-hidden="true">
                    <path d="M19.14,12.94a7.43,7.43,0,0,0,.05-.94,7.43,7.43,0,0,0-.05-.94l2.11-1.65a.5.5,0,0,0,.12-.64l-2-3.46a.5.5,0,0,0-.6-.22L16.29,6.1a7.28,7.28,0,0,0-1.63-.94L14.3,2.5a.49.49,0,0,0-.49-.4H10.19a.49.49,0,0,0-.49.4L9.34,5.16a7.28,7.28,0,0,0-1.63.94L5.23,5.09a.5.5,0,0,0-.6.22l-2,3.46a.5.5,0,0,0,.12.64L4.86,11.06a7.43,7.43,0,0,0-.05.94,7.43,7.43,0,0,0,.05.94L2.75,14.59a.5.5,0,0,0-.12.64l2,3.46a.5.5,0,0,0,.6.22l2.48-1a7.28,7.28,0,0,0,1.63.94l.36,2.66a.49.49,0,0,0,.49.4h3.62a.49.49,0,0,0,.49-.4l.36-2.66a7.28,7.28,0,0,0,1.63-.94l2.48,1a.5.5,0,0,0,.6-.22l2-3.46a.5.5,0,0,0-.12-.64Zm-7.14,2.56A3.5,3.5,0,1,1,15.5,12,3.5,3.5,0,0,1,12,15.5Z"/>
                </svg>
            </button>
            <form action="/logout" method="post">
                <button class="icon-btn icon-logout" type="submit" aria-label="로그아웃">
                    <svg viewBox="0 0 24 24" aria-hidden="true">
                        <path class="door" d="M5 3h8a2 2 0 0 1 2 2v2h-2V5H5v14h8v-2h2v2a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2z"/>
                        <path class="arrow" d="M21.7 12.7a1 1 0 0 0 0-1.4l-3-3a1 1 0 1 0-1.4 1.4L18.6 11H10a1 1 0 1 0 0 2h8.6l-1.3 1.3a1 1 0 1 0 1.4 1.4z"/>
                    </svg>
                </button>
            </form>
        </div>
    </header>

    <c:if test="${not empty pwdError}">
        <div class="feedback-msg is-error">${pwdError}</div>
    </c:if>
    <c:if test="${pwdChanged}">
        <div class="feedback-msg is-success">비밀번호가 변경되었습니다.</div>
    </c:if>

    <section class="control-panel">
        <div class="type-tabs" role="tablist" aria-label="미디어 타입 선택">
            <button class="tab-btn is-active" data-filter-type="all">전체</button>
            <button class="tab-btn" data-filter-type="photo">사진</button>
            <button class="tab-btn" data-filter-type="video">영상</button>
        </div>

        <div class="inline-filters-row">
            <label class="inline-filter">작성자
                <select id="authorFilter">
                    <c:forEach var="author" items="${authors}">
                        <option value="${author}">${author}</option>
                    </c:forEach>
                </select>
            </label>

            <label class="inline-filter">앨범
                <select id="albumFilter">
                    <c:forEach var="year" items="${years}">
                        <option value="${year}">${year}</option>
                    </c:forEach>
                </select>
            </label>
        </div>

        <div class="tag-filter-wrap" aria-label="태그 다중 선택">
            <span class="filter-label">태그</span>
            <details class="tag-filter-box" id="tagFilterBox">
                <summary><span id="selectedTagText">전체 태그</span></summary>
                <div class="tag-options">
                    <c:forEach var="tag" items="${tags}" varStatus="status">
                        <label class="tag-option">
                            <input type="checkbox" class="tag-check" value="${tag}" <c:if test="${status.index lt 2}">checked</c:if>>
                            <span>#${tag}</span>
                        </label>
                    </c:forEach>
                </div>
            </details>
        </div>
    </section>

    <div class="floating-head">
        <div class="view-sort-bar">
            <div class="column-controls" aria-label="피드 보기 방식 선택">
                <span>보기</span>
                <button class="col-btn is-active" data-columns="1">1</button>
                <button class="col-btn" data-columns="3">3</button>
                <button class="col-btn" data-columns="5">5</button>
            </div>

            <label class="sort-inline">정렬
                <select id="sortOption">
                    <option>업로드 최신순</option>
                    <option>업로드 과거순</option>
                    <option>촬영연도 최신순</option>
                    <option>촬영연도 과거순</option>
                    <option>좋아요 많은 순</option>
                </select>
            </label>
        </div>

        <div class="mobile-selection-bar" id="mobileSelectionBar" aria-live="polite" hidden>
            <span><strong id="selectedCount">0</strong>개 선택됨</span>
            <div class="selection-actions">
                <button type="button" class="btn btn-secondary" id="cancelSelectionBtn">취소</button>
                <button type="button" class="btn" id="downloadSelectedBtn">다운로드</button>
            </div>
        </div>
    </div>

    <section id="feedGrid" class="feed-grid columns-1" aria-live="polite">
        <c:forEach var="item" items="${feedItems}">
            <article class="feed-card" data-media-type="${item.mediaType}" data-item-id="${item.id}">
                <a class="thumb-link" href="/feed/${item.id}" aria-label="${item.title} 상세보기">
                    <img src="${item.thumbnailUrl}" alt="${item.title} 썸네일" loading="lazy">
                    <span class="media-badge ${item.mediaType}" data-full-text="${item.mediaType eq 'video' ? 'Video' : 'Photo'}" data-short-text="${item.mediaType eq 'video' ? 'V' : 'P'}">${item.mediaType eq 'video' ? 'Video' : 'Photo'}</span>

                    <div class="overlay-meta overlay-top">
                        <p class="overlay-desc">${item.title}</p>
                        <p class="overlay-album">앨범 ${item.shotYear}</p>
                    </div>
                    <div class="overlay-meta overlay-bottom">
                        <p>💬 ${item.commentCount} · ❤ ${item.likeCount}</p>
                        <p>${item.author}</p>
                    </div>
                </a>

                <div class="feed-meta">
                    <h2>${item.title}</h2>
                    <p>${item.author} · 촬영 ${item.shotYear} · 업로드 ${item.uploadedAt}</p>

                    <ul class="tag-list">
                        <c:forEach var="tag" items="${item.tags}">
                            <li>#${tag}</li>
                        </c:forEach>
                    </ul>

                    <div class="engagement">
                        <c:if test="${item.likeCount > 0}">
                            <span>❤ ${item.likeCount}</span>
                        </c:if>
                        <c:if test="${item.commentCount > 0}">
                            <span>💬 ${item.commentCount}</span>
                        </c:if>
                    </div>
                </div>
            </article>
        </c:forEach>
    </section>

    <div class="load-more-wrap">
        <button type="button" class="btn btn-secondary" id="loadMoreBtn">더 보기 (UI 더미)</button>
    </div>

    <a href="/upload" class="fab-upload" aria-label="업로드 페이지로 이동">＋</a>
</main>

<div id="passwordModalBackdrop" class="modal-backdrop" hidden>
    <section class="password-modal" role="dialog" aria-modal="true" aria-labelledby="passwordModalTitle">
        <header class="modal-header">
            <h2 id="passwordModalTitle">비밀번호 변경</h2>
            <button type="button" class="modal-close-btn" id="closePasswordModalBtn" aria-label="닫기">✕</button>
        </header>

        <form action="/account/password" method="post" class="password-form">
            <label>사용자명</label>
            <input type="text" value="${loginUser.displayName}" readonly>

            <label>아이디</label>
            <input type="text" value="${loginUser.loginId}" readonly>

            <label for="currentPassword">현재 비밀번호</label>
            <input id="currentPassword" name="currentPassword" type="password" required autocomplete="current-password">

            <label for="newPassword">새 비밀번호</label>
            <input id="newPassword" name="newPassword" type="password" required minlength="8" autocomplete="new-password">

            <label for="newPasswordConfirm">새 비밀번호 재확인</label>
            <input id="newPasswordConfirm" name="newPasswordConfirm" type="password" required minlength="8" autocomplete="new-password">

            <div class="modal-actions">
                <button type="button" class="btn btn-secondary modal-cancel" id="cancelPasswordModalBtn">취소</button>
                <button type="submit" class="btn btn-primary">수정</button>
            </div>
        </form>
    </section>
</div>

<script src="/js/feed.js"></script>
<script>
    const passwordModalBackdrop = document.getElementById('passwordModalBackdrop');
    const openPasswordModalBtn = document.getElementById('openPasswordModalBtn');
    const closePasswordModalBtn = document.getElementById('closePasswordModalBtn');
    const cancelPasswordModalBtn = document.getElementById('cancelPasswordModalBtn');

    function openPasswordModal() {
        passwordModalBackdrop.hidden = false;
        document.body.classList.add('modal-open');
    }

    function closePasswordModal() {
        passwordModalBackdrop.hidden = true;
        document.body.classList.remove('modal-open');
    }

    openPasswordModalBtn.addEventListener('click', openPasswordModal);
    closePasswordModalBtn.addEventListener('click', closePasswordModal);
    cancelPasswordModalBtn.addEventListener('click', closePasswordModal);

    passwordModalBackdrop.addEventListener('click', (event) => {
        if (event.target === passwordModalBackdrop) {
            closePasswordModal();
        }
    });

    <c:if test="${not empty pwdError}">
    openPasswordModal();
    </c:if>
</script>
</body>
</html>
