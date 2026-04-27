<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>MemoryBox - 공유 링크 오류</title>
    <%@ include file="/WEB-INF/views/common/head-icons.jspf" %>
    <link rel="stylesheet" href="/css/common.css">
</head>
<body class="page">
<main class="detail-layout">
    <section class="detail-panel state-panel">
        <img src="/images/share-error.png" alt="공유 링크 오류 안내 이미지" style="width:min(220px, 70%); margin:0 auto; display:block;">
        <h1>유효하지 않거나 만료된 공유 페이지입니다.</h1>
        <p>${reason}</p>
        <p>링크를 다시 확인하거나, 공유한 사용자에게 새 링크를 요청해 주세요.</p>
    </section>
</main>
</body>
</html>
