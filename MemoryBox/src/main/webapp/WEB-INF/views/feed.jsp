<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>MemoryBox - <c:out value="${empty pageTitle ? '피드' : pageTitle}"/></title>
    <%@ include file="/WEB-INF/views/common/head-icons.jspf" %>
    <link rel="stylesheet" href="/css/common.css">
    <link rel="stylesheet" href="/css/feed.css">
    <link rel="stylesheet" href="/css/sweetalert2/sweetalert2.min.css">
    <link rel="stylesheet" href="/css/photoswipe/photoswipe.css">
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
                <span class="notification-badge" id="notificationUnreadBadge" style="${notificationPanel.unreadCount == 0 ? 'display:none;' : ''}">${notificationPanel.unreadCount}</span>
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
                    <a href="/notifications/${noti.notificationId}/open" class="notification-link">
                        <p>${noti.message}</p>
                        <small>${noti.relativeCreatedAt}</small>
                    </a>
                    <div class="notification-actions">
                        <button type="button" class="notification-action-btn" data-action="notification-read" data-id="${noti.notificationId}">읽음</button>
                        <button type="button" class="notification-action-btn is-delete" data-action="notification-delete" data-id="${noti.notificationId}">삭제</button>
                    </div>
                </li>
            </c:forEach>
            <c:if test="${empty notificationPanel.items}"><li class="empty">새 알림이 없습니다.</li></c:if>
        </ul>
    </div>

    <c:if test="${mode eq 'mypage'}">
        <section class="mypage-panel">
            <div class="mypage-user-info">
                <strong>${loginUser.displayName}</strong>
                <span>${loginUser.loginId}</span>
            </div>
            <div class="mypage-actions">
                <button class="btn btn-secondary btn-sm" type="button" id="openPasswordModalBtn">비밀번호 변경</button>
                <label class="push-toggle" for="webPushToggle">
                    <span>알림 설정</span>
                    <input type="checkbox" id="webPushToggle" class="push-toggle-input">
                    <span class="push-toggle-slider" aria-hidden="true"></span>
                </label>
                <c:if test="${loginUser.role eq 'ADMIN'}">
                    <button class="btn btn-secondary btn-sm" type="button" id="sendWebPushTestBtn">테스트 푸시</button>
                </c:if>
            </div>
            <c:if test="${loginUser.role eq 'ADMIN'}">
                <p class="webpush-status-msg" id="webPushStatusMsg" aria-live="polite"></p>
            </c:if>
        </section>

        <section class="calendar-card" id="sharedCalendarCard" data-calendar-state="${calendarState}" data-calendar-year="${calendarYear}" data-calendar-month="${calendarMonth}">
            <header class="calendar-card-header">
                <h2>우리 일정</h2>
                <div class="calendar-month-nav">
                    <button type="button" class="calendar-nav-btn" id="calendarPrevBtn" aria-label="이전 달">&lt;</button>
                    <strong id="calendarMonthLabel">${calendarYear}.<c:if test="${calendarMonth lt 10}">0</c:if>${calendarMonth}</strong>
                    <button type="button" class="calendar-nav-btn" id="calendarNextBtn" aria-label="다음 달">&gt;</button>
                </div>
            </header>

            <div id="calendarContentArea">
            <c:choose>
                <c:when test="${calendarState eq 'READY'}">
                    <div class="calendar-week-head">
                        <span>일</span><span>월</span><span>화</span><span>수</span><span>목</span><span>금</span><span>토</span>
                    </div>
                    <div class="calendar-grid" id="calendarGrid">
                        <c:forEach var="day" items="${calendarMonthData.days}">
                            <button type="button"
                                    class="calendar-day ${day.currentMonth ? '' : 'is-outside'} ${day.today ? 'is-today' : ''} ${day.sunday or day.holiday ? 'is-holiday-text' : ''}"
                                    data-date="${day.date}">
                                <span class="day-number">${day.dayNumber}</span>
                                <span class="event-dots">
                                    <c:if test="${day.hasPersonalEvent}"><i class="dot dot-personal"></i></c:if>
                                    <c:if test="${day.holiday}"><i class="dot dot-holiday"></i></c:if>
                                </span>
                            </button>
                        </c:forEach>
                    </div>

                    <section class="calendar-event-panel" id="calendarEventPanel" data-calendar-month='${fn:escapeXml(calendarMonthDataJson)}'>
                        <header class="calendar-event-header" id="calendarEventHeader">다가오는 일정</header>
                        <button type="button" class="calendar-close-btn" id="calendarCloseBtn" hidden>닫기</button>
                        <ul class="calendar-event-list" id="calendarEventList">
                            <c:forEach var="event" items="${calendarMonthData.upcomingEvents}">
                                <li>
                                    <span class="event-time">${event.date} ${event.timeText}</span>
                                    <span class="event-title">${event.title}</span>
                                </li>
                            </c:forEach>
                            <c:if test="${empty calendarMonthData.upcomingEvents}">
                                <li class="empty">다가오는 일정이 없습니다.</li>
                            </c:if>
                        </ul>
                    </section>
                </c:when>
                <c:when test="${calendarState eq 'NO_SOURCES'}">
                    <p class="calendar-empty-msg">등록된 캘린더가 없습니다.</p>
                </c:when>
                <c:when test="${calendarState eq 'ERROR'}">
                    <p class="calendar-empty-msg">일정을 불러오지 못했습니다.</p>
                </c:when>
                <c:otherwise>
                    <p class="calendar-empty-msg">캘린더 기능이 비활성화되어 있습니다.</p>
                </c:otherwise>
            </c:choose>
            </div>
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
        <section class="search-mode-tabs" id="searchModeTabs" hidden>
            <button type="button" class="search-mode-btn is-active" data-search-mode="feed">피드</button>
            <button type="button" class="search-mode-btn" data-search-mode="photo">사진</button>
        </section>

        <section class="album-picker-section" id="albumPickerSection">
            <header class="album-picker-header">
                <h2>앨범 선택</h2>
                <p>원하는 앨범을 먼저 선택해 주세요.</p>
            </header>
            <div class="album-picker-grid" id="albumPickerGrid">
                <c:forEach var="album" items="${albums}">
                    <button type="button" class="album-picker-card"
                            data-album-value="${album}"
                            data-feed-count="${empty albumFeedCounts[album] ? 0 : albumFeedCounts[album]}"
                            data-photo-count="${empty albumPhotoCounts[album] ? 0 : albumPhotoCounts[album]}"
                            data-video-count="${empty albumVideoCounts[album] ? 0 : albumVideoCounts[album]}"
                            aria-label="${album} 앨범 보기">
                        <span class="album-picker-icon" aria-hidden="true">📁</span>
                        <span class="album-picker-name">${album}</span>
                        <span class="album-picker-count" data-album-count-label>
                            <c:out value="${empty albumFeedCounts[album] ? 0 : albumFeedCounts[album]}"/>개의 피드
                        </span>
                    </button>
                </c:forEach>
            </div>
        </section>

        <section class="selected-album-header" id="selectedAlbumHeader" hidden>
            <h2 id="selectedAlbumTitle">전체</h2>
            <p>선택한 앨범의 파일을 조회 중입니다.</p>
        </section>

        <section class="control-panel">
            <div class="type-tabs" role="tablist" aria-label="미디어 타입 선택">
                <button class="tab-btn is-active" data-filter-type="all">전체</button>
                <button class="tab-btn" data-filter-type="photo">사진</button>
                <button class="tab-btn" data-filter-type="video">영상</button>
            </div>
            <div class="inline-filters-row">
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

    <div class="floating-head" id="floatingHead">
        <div class="feed-count-summary" id="feedCountSummary">
            <span>불러온 <strong id="loadedCountText">${mode eq 'search' ? 0 : fn:length(feedItems)}</strong>개</span>
            <span>/ 전체 <strong id="totalCountText">${mode eq 'search' ? 0 : totalCount}</strong>개</span>
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

    </div>

    <section id="feedGrid" class="feed-grid columns-1" aria-live="polite">
        <c:if test="${mode ne 'search'}">
            <c:forEach var="item" items="${feedItems}">
                <article class="feed-card" data-media-type="${item.mediaType}" data-batch-id="${item.id}" data-detail-url="${item.mediaType eq 'video' ? '/video/'.concat(item.mediaItems[0].mediaId) : '/feed/'.concat(item.id)}">
                    <a class="thumb-link" href="${item.mediaType eq 'video' ? '/video/'.concat(item.mediaItems[0].mediaId) : '/feed/'.concat(item.id)}" aria-label="${item.title} 상세보기">
                        <c:choose>
                            <c:when test="${not empty item.mediaItems}">
                                <div class="feed-slider" data-slider>
                                    <div class="feed-slider-track" data-slider-track>
                                        <c:forEach var="media" items="${item.mediaItems}" varStatus="status">
                                            <div class="feed-slide" data-slide-index="${status.index}">
                                                <c:choose>
                                                    <c:when test="${media.mediaType eq 'video'}">
                                                        <c:choose>
                                                            <c:when test="${not empty media.previewUrl}">
                                                                <video class="feed-preview-video"
                                                                       src="${media.previewUrl}"
                                                                       poster="${media.smallUrl}"
                                                                       autoplay
                                                                       muted
                                                                       playsinline
                                                                       loop
                                                                       preload="metadata"
                                                                       data-has-preview="true"></video>
                                                            </c:when>
                                                            <c:when test="${not empty media.smallUrl}">
                                                                <img src="${media.smallUrl}" alt="${item.title} 비디오 썸네일" loading="lazy">
                                                            </c:when>
                                                            <c:otherwise>
                                                                <img src="/images/default-image.png" alt="${item.title} placeholder" loading="lazy">
                                                            </c:otherwise>
                                                        </c:choose>
                                                    </c:when>
                                                    <c:otherwise>
                                                        <img src="${media.smallUrl}" alt="${item.title} 썸네일" loading="lazy">
                                                    </c:otherwise>
                                                </c:choose>
                                            </div>
                                        </c:forEach>
                                    </div>
                                    <c:if test="${fn:length(item.mediaItems) gt 1}">
                                        <button type="button" class="slider-nav prev" data-action="slide-prev" aria-label="이전 미디어">‹</button>
                                        <button type="button" class="slider-nav next" data-action="slide-next" aria-label="다음 미디어">›</button>
                                        <span class="slide-counter" data-slide-counter>1 / ${fn:length(item.mediaItems)}</span>
                                    </c:if>
                                </div>
                            </c:when>
                            <c:otherwise>
                                <c:choose>
                                    <c:when test="${item.mediaType eq 'video'}">
                                        <c:choose>
                                            <c:when test="${not empty item.previewUrl}">
                                                <video class="feed-preview-video"
                                                       src="${item.previewUrl}"
                                                       poster="${item.thumbnailUrl}"
                                                       autoplay
                                                       muted
                                                       playsinline
                                                       loop
                                                       preload="metadata"
                                                       data-has-preview="true"></video>
                                            </c:when>
                                            <c:when test="${not empty item.thumbnailUrl}">
                                                <img src="${item.thumbnailUrl}" alt="${item.title} 썸네일" loading="lazy">
                                            </c:when>
                                            <c:otherwise>
                                                <img src="/images/default-image.png" alt="${item.title} placeholder" loading="lazy">
                                            </c:otherwise>
                                        </c:choose>
                                    </c:when>
                                    <c:otherwise>
                                        <img src="${item.thumbnailUrl}" alt="${item.title} 썸네일" loading="lazy">
                                    </c:otherwise>
                                </c:choose>
                            </c:otherwise>
                        </c:choose>
                        <span class="media-badge ${item.mediaType}" data-full-text="${item.mediaType eq 'video' ? 'Video' : 'Photo'}" data-short-text="${item.mediaType eq 'video' ? 'V' : 'P'}">${item.mediaType eq 'video' ? 'Video' : 'Photo'}</span>
                        <c:if test="${item.recent}"><span class="new-badge" data-full-text="New" data-short-text="N">New</span></c:if>
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
        </c:if>
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

<c:if test="${mode eq 'search'}">
    <div class="selection-bar" id="searchSelectionBar" hidden>
        <span><strong id="searchSelectedCount">0</strong>개 선택됨</span>
        <div class="selection-actions">
            <button type="button" class="btn btn-secondary" id="searchCancelSelectBtn">취소</button>
            <button type="button" class="btn btn-secondary" id="searchDownloadSelectBtn">선택 다운로드</button>
        </div>
    </div>
</c:if>
<script src="/js/sweetalert2/sweetalert2.all.min.js"></script>
<script src="/js/photoswipe/photoswipe.umd.min.js"></script>
<script src="/js/photoswipe/photoswipe-lightbox.umd.min.js"></script>
<script src="/js/feed.js"></script>
<script src="/js/push.js"></script>
</body>
</html>
