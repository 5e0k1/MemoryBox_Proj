<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>요청 작성 | MemoryBox</title>
    <link rel="stylesheet" href="/css/common.css">
    <link rel="stylesheet" href="/css/request.css">
</head>
<body>
<div class="mobile-wrap">
    <a class="upload-home" href="/requests">← 요청 목록</a>
    <h1>요청 작성</h1>
    <form method="post" action="/requests" class="upload-form">
        <label>제목<input name="title" maxlength="200" required></label>
        <label>내용<textarea name="content" maxlength="3000" rows="8" required></textarea></label>
        <button type="submit">등록</button>
    </form>
</div>
</body>
</html>
