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
        <h1>유효하지 않은 공유 링크입니다.</h1>
        <p>${reason}</p>
    </section>
</main>
</body>
</html>
