<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>동영상 업로드 | MemoryBox</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/common.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/upload.css">
</head>
<body>
<div class="mobile-wrap">
    <h1>동영상 업로드</h1>
    <c:if test="${not empty errorMessage}"><p class="msg error">${errorMessage}</p></c:if>
    <form method="post" enctype="multipart/form-data" class="upload-form">
        <label>동영상 파일<input type="file" name="videoFile" accept="video/*" required></label>
        <label>제목<input type="text" name="title" value="${form.title}" maxlength="100"></label>
        <label>촬영일시<input type="datetime-local" class="taken-at-input" name="takenAt" value="${form.takenAt}"></label>
        <label>앨범
            <select name="albumId" required>
                <option value="">선택</option>
                <c:forEach items="${albums}" var="album">
                    <option value="${album.albumId}" <c:if test="${form.albumId == album.albumId}">selected</c:if>>${album.albumName}</option>
                </c:forEach>
            </select>
        </label>

        <section class="tag-widget" data-widget="tag-picker" data-create-url="${pageContext.request.contextPath}/upload/tag">
            <div class="tag-widget-header">
                <h2>태그 목록</h2>
                <span class="tag-count">선택 0개</span>
            </div>
            <div class="tag-title-row">
                <small>추천 태그를 고르거나 새 태그를 추가하세요.</small>
            </div>

            <div class="tag-option-list">
                <c:forEach items="${tags}" var="tag">
                    <label class="tag-option" data-normalized="${tag.normalizedName}">
                        <input type="checkbox" name="selectedTagIds" value="${tag.tagId}" class="tag-check"
                               <c:forEach items="${form.selectedTagIds}" var="selectedId"><c:if test="${selectedId == tag.tagId}">checked</c:if></c:forEach>>
                        <span class="tag-label">${tag.tagName}</span>
                    </label>
                </c:forEach>
            </div>

            <div class="tag-add-row">
                <input type="text" class="tag-add-input" placeholder="새 태그 입력 후 추가 버튼">
                <button type="button" class="tag-add-btn">목록에 추가</button>
            </div>
            <input type="hidden" name="newTags" class="new-tags-hidden" value="${form.newTags}">
        </section>

        <button type="submit">업로드</button>
    </form>
</div>
<script src="${pageContext.request.contextPath}/js/upload.js"></script>
</body>
</html>