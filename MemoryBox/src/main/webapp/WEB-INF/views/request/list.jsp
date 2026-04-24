<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>MemoryBox - 요청</title>
    <link rel="stylesheet" href="/css/common.css">
    <link rel="stylesheet" href="/css/feed.css">
    <link rel="stylesheet" href="/css/request.css">
</head>
<body class="page page-feed" data-mode="requests">
<main class="feed-layout with-bottom-nav">
    <header class="feed-header">
        <div class="brand-wrap"><a href="/feed" class="brand-title">MemoryBox</a><span class="page-chip strong-chip">요청</span></div>
        <div class="header-actions"><span class="login-user">${loginUser.displayName}</span></div>
    </header>

    <a href="/requests/new" class="btn btn-primary request-write-btn">요청 작성</a>

    <section class="request-list">
        <c:forEach var="post" items="${requestPosts}">
            <a href="/requests/${post.requestId}" class="request-card">
                <h2>${post.title}</h2>
                <p class="request-card-content">${post.content}</p>
                <div class="request-card-meta">
                    <span>${post.authorName}</span>
                    <span>${post.relativeCreatedAt}</span>
                    <span>댓글 ${post.commentCount}</span>
                </div>
            </a>
        </c:forEach>
        <c:if test="${empty requestPosts}"><p class="empty-feed-msg">등록된 요청이 없습니다.</p></c:if>
    </section>
</main>

<nav class="bottom-nav" aria-label="하단 메뉴">
    <a class="nav-item" href="/feed"><span>🏠</span><em>피드</em></a>
    <a class="nav-item" href="/search"><span>🔎</span><em>검색</em></a>
    <a class="nav-item" href="/likes"><span>❤</span><em>좋아요</em></a>
    <a class="nav-item is-active" href="/requests"><span>📝</span><em>요청</em></a>
    <a class="nav-item" href="/mypage"><span>👤</span><em>마이</em></a>
</nav>
</body>
</html>
