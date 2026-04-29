<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html><html lang="ko"><head><meta charset="UTF-8"><title>Video Detail</title><link rel="stylesheet" href="/css/common.css"></head>
<body class="page page-detail"><main class="detail-layout">
    <a href="/feed" class="back-link">← 피드</a>
    <c:if test="${notFound}"><p>영상을 찾을 수 없습니다.</p></c:if>
    <c:if test="${not notFound}">
        <h1>${video.title}</h1><p>${video.author} · ${video.uploadedAt}</p>
        <video controls playsinline preload="metadata" poster="${video.posterUrl}" src="${video.playbackUrl}" style="width:100%;max-width:780px;"></video>
        <p><a class="btn btn-secondary" href="${video.downloadUrl}">원본 다운로드</a></p>
    </c:if>
</main></body></html>
