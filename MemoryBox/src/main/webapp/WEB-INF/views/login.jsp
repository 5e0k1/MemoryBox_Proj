<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>MemoryBox - 로그인</title>
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

        <%-- 실제 인증 로직 연결 전까지 /feed로 더미 이동 --%>
        <form class="login-form" action="/feed" method="get">
            <label for="username">아이디</label>
            <input id="username" name="username" type="text" placeholder="아이디를 입력하세요" autocomplete="username" required>

            <label for="password">비밀번호</label>
            <input id="password" name="password" type="password" placeholder="비밀번호를 입력하세요" autocomplete="current-password" required>

            <button type="submit" class="btn btn-primary">로그인</button>
        </form>

        <section class="account-actions">
            <p class="hint">최초 로그인 시 비밀번호 변경이 필요합니다.</p>
            <div class="action-row">
                <button type="button" class="btn btn-secondary" disabled>최초 비밀번호 변경(준비중)</button>
                <button type="button" class="btn btn-text" disabled>계정관리(추후 연결)</button>
            </div>
        </section>
    </section>
</main>
</body>
</html>
