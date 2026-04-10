<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>다중 사진 업로드 | MemoryBox</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/common.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/upload.css">
</head>
<body>
<div class="mobile-wrap">
    <h1>사진 여러 장 업로드</h1>
    <c:if test="${not empty errorMessage}"><p class="msg error">${errorMessage}</p></c:if>
    <form method="post" enctype="multipart/form-data" class="upload-form" id="multiUploadForm">
        <label>사진 파일들
            <input type="file" name="imageFiles" accept="image/*" multiple required id="multiImageInput">
        </label>
        <label>공통 촬영일시<input type="datetime-local" name="takenAt" value="${form.takenAt}"></label>
        <label>앨범
            <select name="albumId" required>
                <option value="">선택</option>
                <c:forEach items="${albums}" var="album">
                    <option value="${album.albumId}" <c:if test="${form.albumId == album.albumId}">selected</c:if>>${album.albumName}</option>
                </c:forEach>
            </select>
        </label>

        <section>
            <h2>파일별 미리보기 / 태그</h2>
            <div id="previewList" class="preview-list"></div>
        </section>

        <button type="submit">일괄 업로드</button>
    </form>
</div>
<script src="${pageContext.request.contextPath}/js/upload.js"></script>
</body>
</html>
