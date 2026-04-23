<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
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
            <span class="page-chip strong-chip"><c:out value="${empty pageTitle ? '피드' : pageTitle}"/></span>
        </div>
        <div class="header-actions">
            <span class="login-user">${loginUser.displayName}</span>
            <button type="button" class="icon-btn icon-notification" id="notificationToggleBtn" aria-label="알림 열기">
                🔔
                <c:if test="${notificationPanel.unreadCount > 0}"><span class="notification-badge" id="notificationUnreadBadge">${notificationPanel.unreadCount}</span></c:if>
                <c:if test="${notificationPanel.unreadCount == 0}"><span class="notification-badge" id="notificationUnreadBadge" hidden>0</span></c:if>
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


    <div class="notification-dropdown" id="notificationDropdown" hidden>
        <header>알림</header>
        <ul id="notificationList" class="notification-list">
            <c:forEach var="noti" items="${notificationPanel.items}">
                <li class="${noti.isRead ? '' : 'is-unread'}">
                    <a href="/notifications/${noti.notificationId}/open">
                        <p>${noti.message}</p>
                        <small>${noti.relativeCreatedAt}</small>
                    </a>
                </li>
            </c:forEach>
            <c:if test="${empty notificationPanel.items}"><li class="empty">새 알림이 없습니다.</li></c:if>
        </ul>
    </div>

    <c:if test="${mode eq 'mypage'}">
        <section class="mypage-panel">
            <p><strong>${loginUser.displayName}</strong> (${loginUser.loginId})</p>
            <button class="btn btn-secondary" type="button" id="openPasswordModalBtn">비밀번호 변경</button>
        </section>
    </c:if>

    <c:if test="${not empty pwdError}"><div class="feedback-msg is-error">${pwdError}</div></c:if>
    <c:if test="${pwdChanged}"><div class="feedback-msg is-success">비밀번호가 변경되었습니다.</div></c:if>

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
                        <c:forEach var="album" items="${albums}"><option value="${album}">${album}</option></c:forEach>
                    </select>
                </label>
                <label class="inline-filter">작성자
                    <select id="authorFilter">
                        <c:forEach var="author" items="${authors}"><option value="${author}">${author}</option></c:forEach>
                    </select>
                </label>
            </div>
            <div class="tag-filter-wrap">
                <span class="filter-label">태그</span>
                <details class="tag-filter-box" id="tagFilterBox">
                    <summary><span id="selectedTagText">태그 전체</span></summary>
                    <div class="tag-options">
                        <c:forEach var="tag" items="${tags}">
                            <label class="tag-option ${fn:startsWith(tag, '@') ? 'is-person' : ''}">
                                <input type="checkbox" class="tag-check" value="${tag}">
                                <span>${tag}</span>
                            </label>
                        </c:forEach>
                    </div>
                </details>
            </div>
        </section>
    </c:if>

    <div class="floating-head">
        <div class="feed-count-summary" id="feedCountSummary">
            <span>불러온 <strong id="loadedCountText">${fn:length(feedItems)}</strong>개</span>
            <span>/ 전체 <strong id="totalCountText">${totalCount}</strong>개</span>
        </div>
        <div class="view-sort-bar">
            <div class="column-controls" aria-label="피드 보기 방식 선택">
                <span>보기</span>
                <button class="col-btn is-active" data-columns="1">1</button>
                <button class="col-btn" data-columns="3">3</button>
                <button class="col-btn" data-columns="5">5</button>
            </div>
            <label class="sort-inline">정렬
                <select id="sortOption">
                    <option value="uploaded_desc">업로드 최신순</option>
                    <option value="uploaded_asc">업로드 과거순</option>
                    <option value="taken_desc">촬영연도 최신순</option>
                    <option value="taken_asc">촬영연도 과거순</option>
                    <option value="likes_desc">좋아요 많은 순</option>
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
            <article class="feed-card" data-media-type="${item.mediaType}" data-item-id="${item.id}" data-detail-url="/feed/${item.id}">
                <a class="thumb-link" href="/feed/${item.id}" aria-label="${item.title} 상세보기">
                    <img src="${item.thumbnailUrl}" alt="${item.title} 썸네일" loading="lazy">
                    <span class="media-badge ${item.mediaType}" data-full-text="${item.mediaType eq 'video' ? 'Video' : 'Photo'}" data-short-text="${item.mediaType eq 'video' ? 'V' : 'P'}">${item.mediaType eq 'video' ? 'Video' : 'Photo'}</span>
                    <c:if test="${item.new}"><span class="new-badge" data-full-text="New" data-short-text="N">New</span></c:if>
                    <span class="select-check" aria-hidden="true">✔</span>
                    <div class="overlay-meta overlay-bottom"><p>${item.author}</p></div>
                </a>
                <button type="button" class="like-toggle-btn ${item.likedByMe ? 'is-liked' : ''}" data-action="like-toggle" aria-label="좋아요 토글" aria-pressed="${item.likedByMe}">
                    <span class="heart">${item.likedByMe ? '❤' : '♡'}</span>
                </button>
                <div class="feed-meta">
                    <h2>${item.title}</h2>
                    <p>${item.author} · 촬영 ${empty item.takenAt ? "-" : item.takenAt} · 업로드 ${item.relativeUploadedAt}</p>
                    <ul class="tag-list">
                        <c:forEach var="tag" items="${item.tags}">
                            <li class="${fn:startsWith(tag, '@') ? 'person-tag' : ''}">${tag}</li>
                        </c:forEach>
                    </ul>
                    <div class="engagement" data-action="meta-actions">
                        <button type="button" class="meta-btn like-meta-btn ${item.likedByMe ? 'is-liked' : ''}" data-action="like-toggle" aria-label="좋아요 토글">
                            ❤ <span class="like-count">${item.likeCount}</span>
                        </button>
                        <button type="button" class="meta-btn comment-meta-btn" data-action="open-comments" aria-label="댓글 열기">
                            💬 <span class="comment-count">${item.commentCount}</span>
                        </button>
                    </div>
                </div>
            </article>
        </c:forEach>
        <c:if test="${empty feedItems}"><p class="empty-feed-msg">표시할 이미지 피드가 없습니다.</p></c:if>
    </section>

    <div class="infinite-loader" id="infiniteLoader" hidden><span class="spinner"></span><span>불러오는 중...</span></div>
    <div class="feed-end-message" id="feedEndMessage" hidden>불러올 데이터가 없습니다.</div>
    <div id="feedSentinel" class="feed-sentinel" aria-hidden="true"></div>
    <a href="/upload" class="fab-upload" aria-label="업로드 페이지로 이동">＋</a>
</main>

<nav class="bottom-nav" aria-label="하단 메뉴">
    <a class="nav-item ${empty mode or mode eq 'feed' ? 'is-active' : ''}" href="/feed"><span>🏠</span><em>피드</em></a>
    <a class="nav-item ${mode eq 'search' ? 'is-active' : ''}" href="/search"><span>🔎</span><em>검색</em></a>
    <a class="nav-item ${mode eq 'likes' ? 'is-active' : ''}" href="/likes"><span>❤</span><em>좋아요</em></a>
    <a class="nav-item ${mode eq 'requests' ? 'is-active' : ''}" href="/requests"><span>📝</span><em>요청</em></a>
    <a class="nav-item ${mode eq 'mypage' ? 'is-active' : ''}" href="/mypage"><span>👤</span><em>마이</em></a>
</nav>

<div id="commentSheetBackdrop" class="comment-sheet-backdrop" hidden>
    <section class="comment-sheet" role="dialog" aria-modal="true" aria-labelledby="commentSheetTitle">
        <header class="comment-sheet-header">
            <h2 id="commentSheetTitle">댓글</h2>
            <button type="button" class="modal-close-btn" id="commentSheetCloseBtn" aria-label="닫기">✕</button>
        </header>
        <div id="commentSheetBody" class="comment-sheet-body"></div>
        <form id="commentSheetForm" class="comment-sheet-form">
            <input type="hidden" id="commentParentId" name="parentId" value="">
            <textarea id="commentSheetInput" name="content" maxlength="3000" placeholder="댓글을 입력해 주세요" required></textarea>
            <div class="comment-sheet-actions">
                <button type="button" class="btn btn-secondary" id="commentReplyCancelBtn" hidden>답글 취소</button>
                <button type="submit" class="btn btn-primary">등록</button>
            </div>
        </form>
    </section>
</div>

<div id="passwordModalBackdrop" class="modal-backdrop" hidden>
    <section class="password-modal" role="dialog" aria-modal="true" aria-labelledby="passwordModalTitle">
        <header class="modal-header"><h2 id="passwordModalTitle">비밀번호 변경</h2><button type="button" class="modal-close-btn" id="closePasswordModalBtn" aria-label="닫기">✕</button></header>
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
