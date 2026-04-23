<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>MemoryBox - <c:out value="${empty pageTitle ? '피드' : pageTitle}"/></title>
    <link rel="stylesheet" href="/css/common.css">
    <link rel="stylesheet" href="/css/feed.css">
</head>
<body class="page page-feed" data-mode="${empty mode ? 'feed' : mode}">
<main class="feed-layout with-bottom-nav">
    <header class="feed-header">
        <div class="brand-wrap">
            <a href="/feed" class="brand-title">MemoryBox</a>
            <span class="page-chip"><c:out value="${empty pageTitle ? '피드' : pageTitle}"/></span>
        </div>
        <div class="header-actions">
            <span class="login-user">${loginUser.displayName}</span>
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

    <c:if test="${mode eq 'mypage'}">
        <section class="mypage-panel">
            <p><strong>${loginUser.displayName}</strong> (${loginUser.loginId})</p>
            <button class="btn btn-secondary" type="button" id="openPasswordModalBtn">비밀번호 변경</button>
        </section>
    </c:if>

    <c:if test="${not empty pwdError}">
        <div class="feedback-msg is-error">${pwdError}</div>
    </c:if>
    <c:if test="${pwdChanged}">
        <div class="feedback-msg is-success">비밀번호가 변경되었습니다.</div>
    </c:if>

    <c:if test="${empty mode or mode eq 'feed'}">
        <section class="control-panel compact-control-panel">
            <div class="type-tabs" role="tablist" aria-label="미디어 타입 선택">
                <button class="tab-btn is-active" data-filter-type="all">전체</button>
                <button class="tab-btn" data-filter-type="photo">사진</button>
                <button class="tab-btn" data-filter-type="video">영상</button>
            </div>
        </section>
    </c:if>

    <c:if test="${mode eq 'search'}">
        <section class="control-panel">
            <div class="type-tabs" role="tablist" aria-label="미디어 타입 선택">
                <button class="tab-btn is-active" data-filter-type="all">전체</button>
                <button class="tab-btn" data-filter-type="photo">사진</button>
                <button class="tab-btn" data-filter-type="video">영상</button>
            </div>
            <div class="inline-filters-row">
                <label class="inline-filter">앨범
                    <select id="albumFilter">
                        <c:forEach var="album" items="${albums}">
                            <option value="${album}">${album}</option>
                        </c:forEach>
                    </select>
                </label>
                <label class="inline-filter">작성자
                    <select id="authorFilter">
                        <c:forEach var="author" items="${authors}">
                            <option value="${author}">${author}</option>
                        </c:forEach>
                    </select>
                </label>
            </div>
            <div class="inline-filters-row">
                <label class="inline-filter">태그
                    <select id="tagFilter">
                        <option value="">선택 안함</option>
                        <c:forEach var="tag" items="${tags}">
                            <option value="${tag}">#${tag}</option>
                        </c:forEach>
                    </select>
                </label>
                <label class="inline-filter">정렬
                    <select id="sortOption">
                        <option value="uploaded_desc">업로드 최신순</option>
                        <option value="uploaded_asc">업로드 과거순</option>
                        <option value="taken_desc">촬영연도 최신순</option>
                        <option value="taken_asc">촬영연도 과거순</option>
                        <option value="likes_desc">좋아요 많은 순</option>
                    </select>
                </label>
            </div>
        </section>
    </c:if>

    <div class="floating-head">
        <div class="view-sort-bar">
            <div class="column-controls" aria-label="피드 보기 방식 선택">
                <span>보기</span>
                <button class="col-btn is-active" data-columns="1">1</button>
                <button class="col-btn" data-columns="3">3</button>
                <button class="col-btn" data-columns="5">5</button>
            </div>
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
                    <span class="select-check" aria-hidden="true">✔</span>
                    <div class="overlay-meta overlay-top">
                        <p class="overlay-desc">${item.title}</p>
                        <p class="overlay-album">앨범 ${item.albumName}</p>
                    </div>
                    <div class="overlay-meta overlay-bottom">
                        <p>💬 ${item.commentCount} · ❤ ${item.likeCount}</p>
                        <p>${item.author}</p>
                    </div>
                </a>
                <div class="feed-meta">
                    <h2>${item.title}</h2>
                    <p>${item.author} · 촬영 ${empty item.takenAt ? "-" : item.takenAt} · 업로드 ${item.uploadedAt}</p>
                    <ul class="tag-list"><c:forEach var="tag" items="${item.tags}"><li>#${tag}</li></c:forEach></ul>
                    <div class="engagement">
                        <c:if test="${item.likeCount > 0}"><span>❤ ${item.likeCount}</span></c:if>
                        <c:if test="${item.commentCount > 0}"><span>💬 ${item.commentCount}</span></c:if>
                    </div>
                </div>
            </article>
        </c:forEach>
        <c:if test="${empty feedItems}"><p class="empty-feed-msg">표시할 이미지 피드가 없습니다.</p></c:if>
    </section>

    <div class="infinite-loader" id="infiniteLoader" hidden><span class="spinner"></span><span>불러오는 중...</span></div>
    <div id="feedSentinel" class="feed-sentinel" aria-hidden="true"></div>

    <a href="/upload" class="fab-upload" aria-label="업로드 페이지로 이동">＋</a>
</main>

<nav class="bottom-nav" aria-label="하단 메뉴">
    <a class="nav-item ${empty mode or mode eq 'feed' ? 'is-active' : ''}" href="/feed"><span>🏠</span><em>피드</em></a>
    <a class="nav-item ${mode eq 'search' ? 'is-active' : ''}" href="/search"><span>🔎</span><em>검색</em></a>
    <a class="nav-item ${mode eq 'likes' ? 'is-active' : ''}" href="/likes"><span>❤</span><em>좋아요</em></a>
    <a class="nav-item ${mode eq 'mypage' ? 'is-active' : ''}" href="/mypage"><span>👤</span><em>마이</em></a>
</nav>

<div id="passwordModalBackdrop" class="modal-backdrop" hidden>
    <section class="password-modal" role="dialog" aria-modal="true" aria-labelledby="passwordModalTitle">
        <header class="modal-header">
            <h2 id="passwordModalTitle">비밀번호 변경</h2>
            <button type="button" class="modal-close-btn" id="closePasswordModalBtn" aria-label="닫기">✕</button>
        </header>
        <form action="/account/password" method="post" class="password-form">
            <label>사용자명</label><input type="text" value="${loginUser.displayName}" readonly>
            <label>아이디</label><input type="text" value="${loginUser.loginId}" readonly>
            <label for="currentPassword">현재 비밀번호</label><input id="currentPassword" name="currentPassword" type="password" required autocomplete="current-password">
            <label for="newPassword">새 비밀번호</label><input id="newPassword" name="newPassword" type="password" required minlength="8" autocomplete="new-password">
            <label for="newPasswordConfirm">새 비밀번호 재확인</label><input id="newPasswordConfirm" name="newPasswordConfirm" type="password" required minlength="8" autocomplete="new-password">
            <div class="modal-actions"><button type="button" class="btn btn-secondary modal-cancel" id="cancelPasswordModalBtn">취소</button><button type="submit" class="btn btn-primary">수정</button></div>
        </form>
    </section>
</div>

<script src="/js/feed.js"></script>
</body>
</html>
