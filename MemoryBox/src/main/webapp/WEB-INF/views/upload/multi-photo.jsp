<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>다중 사진 업로드 | MemoryBox</title>
    <%@ include file="/WEB-INF/views/common/head-icons.jspf" %>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/common.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/upload.css">
</head>
<body>
<div class="mobile-wrap">
    <a class="upload-home" href="${pageContext.request.contextPath}/feed">MemoryBox</a>
    <h1>사진 여러 장 업로드</h1>
    <c:if test="${not empty errorMessage}"><p class="msg error">${errorMessage}</p></c:if>
    <form method="post" enctype="multipart/form-data" class="upload-form" id="multiUploadForm">
        <label>사진 파일들
            <input type="file" name="imageFiles" accept="image/*" multiple required id="multiImageInput">
        </label>
        <label>공통 제목<input type="text" name="title" value="${form.title}" maxlength="100"></label>
        <label>공통 촬영일시<input type="datetime-local" class="taken-at-input" name="takenAt" value="${form.takenAt}"></label>
        <label>앨범
            <div class="album-select-row" data-widget="album-picker" data-create-url="${pageContext.request.contextPath}/upload/album">
                <select name="albumId" required class="album-select">
                <option value="">선택</option>
                <c:forEach items="${albums}" var="album">
                    <option value="${album.albumId}" <c:if test="${form.albumId == album.albumId}">selected</c:if>>${album.albumName}</option>
                </c:forEach>
            </select>
                <button type="button" class="album-add-btn">+ 새 앨범</button>
            </div>
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
                    <label class="tag-option ${tag.tagScope == 'P' ? 'is-person' : ''}" data-normalized="${tag.normalizedName}" data-scope="${tag.tagScope}">
                        <input type="checkbox" name="selectedTagIds" value="${tag.tagId}" class="tag-check"
                               <c:forEach items="${form.selectedTagIds}" var="selectedId"><c:if test="${selectedId == tag.tagId}">checked</c:if></c:forEach>>
                        <span class="tag-label">${tag.tagName}</span>
                    </label>
                </c:forEach>
            </div>

            <div class="tag-add-row">
                <input type="text" class="tag-add-input" placeholder="새 태그 입력 후 추가 버튼">
                <button type="button" class="tag-add-btn">추가</button>
            </div>
            <input type="hidden" name="newTags" class="new-tags-hidden" value="${form.newTags}">
        </section>

        <section>
            <h2>파일 미리보기</h2>
            <div id="previewList" class="preview-list"></div>
        </section>

        

        <div class="album-modal-backdrop" hidden>
            <section class="album-modal" role="dialog" aria-modal="true" aria-labelledby="albumModalTitle">
                <h2 id="albumModalTitle">새 앨범 만들기</h2>
                <input type="text" class="album-add-input" maxlength="50" placeholder="앨범명을 입력하세요">
                <p class="album-modal-error" hidden></p>
                <div class="album-modal-actions">
                    <button type="button" class="btn btn-secondary album-cancel-btn">취소</button>
                    <button type="button" class="btn btn-primary album-create-btn">생성</button>
                </div>
            </section>
        </div>

        <button type="submit">일괄 업로드</button>
    </form>
</div>
<script src="${pageContext.request.contextPath}/js/upload.js"></script>
</body>
</html>
