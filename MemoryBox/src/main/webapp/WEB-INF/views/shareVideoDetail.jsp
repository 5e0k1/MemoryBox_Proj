<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html><html lang="ko"><head><meta charset="UTF-8"><title>공유 영상</title><link rel="stylesheet" href="/css/common.css"><link rel="stylesheet" href="/css/detail.css"></head>
<body class="page page-detail"><main class="detail-layout">
    <header class="detail-header"><a href="/share/${shareToken}" class="back-link">← 공유 상세</a><div class="login-chip">게스트 공유</div></header>
    <c:if test="${notFound}"><section class="detail-panel state-panel"><p>영상을 찾을 수 없습니다.</p></section></c:if>
    <c:if test="${not notFound}">
        <section class="detail-panel meta-panel"><h1 class="detail-title">${video.title}</h1></section>
        <section class="detail-panel media-panel"><video class="detail-image" controls playsinline preload="metadata" poster="${video.posterUrl}" src="${video.playbackUrl}"></video></section>
    </c:if>
</main></body></html>
