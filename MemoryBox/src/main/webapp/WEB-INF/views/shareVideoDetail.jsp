<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html><html lang="ko"><head><meta charset="UTF-8"><title>공유 영상</title><link rel="stylesheet" href="/css/common.css"></head>
<body><main class="detail-layout">
    <a href="/share/${shareToken}" class="back-link">← 공유 상세</a>
    <c:if test="${notFound}"><p>영상을 찾을 수 없습니다.</p></c:if>
    <c:if test="${not notFound}">
        <h1>${video.title}</h1>
        <video controls playsinline preload="metadata" poster="${video.posterUrl}" src="${video.playbackUrl}" style="width:100%;max-width:780px;"></video>
    </c:if>
</main></body></html>
