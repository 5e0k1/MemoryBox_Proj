<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>요청 상세 | MemoryBox</title>
    <link rel="stylesheet" href="/css/common.css">
    <link rel="stylesheet" href="/css/request.css">
</head>
<body>
<div class="mobile-wrap">
    <a class="upload-home" href="/requests">← 요청 목록</a>
    <c:if test="${notFound}"><p class="msg error">요청글이 존재하지 않습니다.</p></c:if>
    <c:if test="${not notFound}">
        <article class="request-detail-card">
            <h1>${requestPost.title}</h1>
            <p class="request-meta">${requestPost.authorName} · ${requestPost.relativeCreatedAt}</p>
            <div class="request-body">${requestPost.content}</div>
        </article>

        <section class="request-comments">
            <h2>댓글</h2>
            <ul>
                <c:forEach var="comment" items="${requestPost.comments}">
                    <li>
                        <strong>${comment.authorName}</strong>
                        <span>${comment.relativeCreatedAt}</span>
                        <p>${comment.content}</p>
                    </li>
                </c:forEach>
                <c:if test="${empty requestPost.comments}"><li>첫 댓글을 남겨보세요.</li></c:if>
            </ul>
            <form method="post" action="/requests/${requestPost.requestId}/comments" class="upload-form">
                <label>댓글<textarea name="content" maxlength="1000" rows="4" required></textarea></label>
                <button type="submit">댓글 등록</button>
            </form>
        </section>
    </c:if>
</div>
</body>
</html>
