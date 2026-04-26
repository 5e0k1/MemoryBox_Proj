<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>MemoryBox - 로그인</title>
    <%@ include file="/WEB-INF/views/common/head-icons.jspf" %>
    <link rel="stylesheet" href="/css/common.css">
    <link rel="stylesheet" href="/css/login.css">
</head>
<body class="page page-login">
<main class="mobile-shell">
    <section class="login-card">
        <header class="login-header">
            <h1>MemoryBox</h1>
            <p>우리만 보는 소규모 사진/영상 공유함</p>
        </header>

        <c:if test="${not empty globalError}">
            <div class="form-error" role="alert"><c:out value="${globalError}"/></div>
        </c:if>

        <form class="login-form" action="/login" method="post">
            <input type="hidden" name="redirect" value="<c:out value='${redirect}'/>">
            <label for="loginId">아이디</label>
            <input id="loginId" name="loginId" type="text" value="<c:out value='${loginForm.loginId}'/>" placeholder="아이디를 입력하세요" autocomplete="username" required>

            <label for="password">비밀번호</label>
            <input id="password" name="password" type="password" placeholder="비밀번호를 입력하세요" autocomplete="current-password" required>

            <label class="remember-me-option" for="rememberMe">
                <input id="rememberMe" name="rememberMe" type="checkbox" value="true" ${loginForm.rememberMe ? 'checked' : ''}>
                자동 로그인
            </label>

            <button type="submit" class="btn btn-primary">로그인</button>
        </form>

        <section class="account-actions">
            <p class="hint">관리자가 등록한 계정으로만 로그인할 수 있습니다.</p>
        </section>
    </section>
</main>
</body>
</html>
