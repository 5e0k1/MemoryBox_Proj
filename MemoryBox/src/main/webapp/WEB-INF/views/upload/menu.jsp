<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>업로드 메뉴 | MemoryBox</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/common.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/upload.css">
</head>
<body>
<div class="mobile-wrap">
    <header class="upload-header">
        <h1>업로드 방식 선택</h1>
        <p>${loginUser.displayName}님, 어떤 업로드를 진행할까요?</p>
    </header>

    <main class="upload-branch-list">
        <a class="upload-branch-card" href="${pageContext.request.contextPath}/upload/photo">
            <h2>사진 1장 업로드</h2>
            <p>제목/앨범/태그를 입력해서 한 장만 업로드합니다.</p>
        </a>
        <a class="upload-branch-card" href="${pageContext.request.contextPath}/upload/photos">
            <h2>사진 여러 장 업로드</h2>
            <p>여러 장 선택 + 파일별 태그 입력 + 미리보기 제공.</p>
        </a>
        <a class="upload-branch-card" href="${pageContext.request.contextPath}/upload/video">
            <h2>동영상 업로드</h2>
            <p>동영상 1개 업로드 및 썸네일 생성.</p>
        </a>
    </main>
</div>
</body>
</html>
