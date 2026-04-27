<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>

<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>MemoryBox - 오류</title>

    <%@ include file="/WEB-INF/views/common/head-icons.jspf" %>

    <link rel="stylesheet" href="/css/common.css">
    <link rel="stylesheet" href="/css/common-error.css">
</head>

<body class="page page-error">

<div class="error-wrapper">
    <div class="error-card">

        <!-- 기존 favicon 이미지 활용 -->
        <img src="/images/default-image.png" class="error-logo" alt="MemoryBox">

        <div class="error-title">
            앗! 문제가 발생했어요
        </div>

        <div class="error-desc">
            요청하신 페이지를 처리하는 중 오류가 발생했습니다.<br>
            잠시 후 다시 시도해 주세요.
        </div>

        <div class="error-actions">
            <button class="btn btn-primary" onclick="location.href='/feed'">
                홈으로 돌아가기
            </button>

            <button class="btn btn-secondary" onclick="location.reload()">
                다시 시도
            </button>
        </div>

    </div>
</div>

<div class="error-bg"></div>

</body>
</html>