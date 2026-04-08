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
        <a href="/login" class="btn btn-text">로그아웃(더미)</a>
    </header>

    <section class="control-panel">
        <div class="type-tabs" role="tablist" aria-label="미디어 타입 선택">
            <button class="tab-btn is-active" data-filter-type="all">전체</button>
            <button class="tab-btn" data-filter-type="photo">사진</button>
            <button class="tab-btn" data-filter-type="video">영상</button>
        </div>

        <div class="controls-grid">
            <label>정렬
                <select id="sortOption">
                    <option>업로드 최신순</option>
                    <option>업로드 과거순</option>
                    <option>촬영연도 최신순</option>
                    <option>촬영연도 과거순</option>
                    <option>좋아요 많은 순</option>
                </select>
            </label>

            <label>작성자
                <select id="authorFilter">
                    <c:forEach var="author" items="${authors}">
                        <option value="${author}">${author}</option>
                    </c:forEach>
                </select>
            </label>

            <label>태그
                <select id="tagFilter">
                    <c:forEach var="tag" items="${tags}">
                        <option value="${tag}">${tag}</option>
                    </c:forEach>
                </select>
            </label>

            <label>연도
                <select id="yearFilter">
                    <c:forEach var="year" items="${years}">
                        <option value="${year}">${year}</option>
                    </c:forEach>
                </select>
            </label>
        </div>

        <div class="toolbar-row">
            <div class="column-controls" aria-label="피드 열 개수 선택">
                <span>열 수</span>
                <button class="col-btn is-active" data-columns="1">1</button>
                <button class="col-btn" data-columns="3">3</button>
                <button class="col-btn" data-columns="5">5</button>
            </div>

            <div class="bulk-actions">
                <button class="btn btn-secondary" type="button">다중 선택 모드</button>
                <button class="btn btn-secondary" type="button">일괄 다운로드</button>
            </div>
        </div>
    </section>

    <%-- 카드 클릭(짧은 터치) 시 상세로, 길게 누르기/우클릭은 JS에서 후속 확장 훅 제공 --%>
    <section id="feedGrid" class="feed-grid columns-1" aria-live="polite">
        <c:forEach var="item" items="${feedItems}">
            <article class="feed-card" data-media-type="${item.mediaType}" data-item-id="${item.id}">
                <a class="thumb-link" href="/feed/${item.id}" aria-label="${item.title} 상세보기">
                    <img src="${item.thumbnailUrl}" alt="${item.title} 썸네일" loading="lazy">
                    <span class="media-badge ${item.mediaType}">${item.mediaType eq 'video' ? 'VIDEO' : 'PHOTO'}</span>
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

<script src="/js/feed.js"></script>
</body>
</html>
