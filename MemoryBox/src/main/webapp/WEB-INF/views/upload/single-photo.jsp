<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>사진 1장 업로드 | MemoryBox</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/common.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/upload.css">
</head>
<body>
<div class="mobile-wrap">
    <h1>사진 1장 업로드</h1>
    <c:if test="${not empty errorMessage}"><p class="msg error">${errorMessage}</p></c:if>
    <form method="post" enctype="multipart/form-data" class="upload-form">
        <label>사진 파일<input type="file" name="imageFile" accept="image/*" required></label>
        <label>제목<input type="text" name="title" value="${form.title}" maxlength="100"></label>
        <label>촬영일시<input type="datetime-local" name="takenAt" value="${form.takenAt}"></label>
        <label>앨범
            <select name="albumId" required>
                <option value="">선택</option>
                <c:forEach items="${albums}" var="album">
                    <option value="${album.albumId}" <c:if test="${form.albumId == album.albumId}">selected</c:if>>${album.albumName}</option>
                </c:forEach>
            </select>
        </label>
        <label>태그(쉼표 구분)<input type="text" name="tags" value="${form.tags}" placeholder="여행, 가족, 제주"></label>
        <button type="submit">업로드</button>
    </form>
</div>
</body>
</html>
