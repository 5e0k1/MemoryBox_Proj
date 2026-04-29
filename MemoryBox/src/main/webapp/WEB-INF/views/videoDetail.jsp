<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="ko"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1.0"><title>Video Detail</title><link rel="stylesheet" href="/css/common.css"><link rel="stylesheet" href="/css/detail.css"></head>
<body class="page page-detail"><main class="detail-layout with-bottom-nav">
    <header class="detail-header"><a href="/feed" class="back-link">← 피드</a><div class="login-chip">${loginUser.displayName}</div></header>
    <c:if test="${notFound}"><section class="detail-panel state-panel"><p>영상을 찾을 수 없습니다.</p></section></c:if>
    <c:if test="${not notFound}">
        <section class="detail-panel meta-panel">
            <div class="title-row">
                <h1 class="detail-title">${video.title}</h1>
                <div class="meta-action-buttons">
                    <button type="button" class="share-open-btn" id="shareOpenBtn" aria-label="공유하기">
                        <img src="/images/share-btn-img.png" alt="공유하기" width="20" height="20">
                    </button>
                    <a href="${video.downloadUrl}" class="btn btn-secondary">다운로드</a>
                </div>
            </div>
            <p class="meta-line">작성자 ${detail.authorName}</p>
            <p class="meta-line">업로드 ${detail.relativeUploadedAt}</p>
            <p class="meta-line">앨범 ${detail.albumName} · ${detail.commentCount} 댓글</p>
            <div class="engagement-row">
                    <form action="/feed/${video.batchId}/like" method="post" class="inline-form">
                        <input type="hidden" name="action" value="${detail.likedByMe ? 'unlike' : 'like'}">
                        <input type="hidden" name="redirectTo" value="/video/${video.mediaId}">
                    <button type="submit" class="btn btn-secondary like-btn ${detail.likedByMe ? 'is-liked' : ''}">
                        ${detail.likedByMe ? '❤ 좋아요 취소' : '♡ 좋아요'} · ${detail.likeCount}
                    </button>
                </form>
            </div>
        </section>
        <section class="detail-panel media-panel">
            <video class="detail-image" controls playsinline preload="metadata" poster="${video.posterUrl}" src="${video.playbackUrl}"></video>
        </section>
        <section class="detail-panel comment-panel">
            <h2>댓글</h2>
            <ul class="comment-list">
                <c:forEach var="comment" items="${comments}">
                    <li class="comment-item"><div class="comment-head"><strong>${comment.authorName}</strong><span>${comment.createdAt}</span></div><p>${comment.content}</p></li>
                </c:forEach>
                <c:if test="${empty comments}"><li class="comment-empty">아직 댓글이 없습니다. 첫 댓글을 남겨보세요.</li></c:if>
            </ul>
            <form method="post" action="/feed/${video.batchId}/comments" class="comment-form">
                <input type="hidden" name="redirectTo" value="/video/${video.mediaId}">
                <textarea name="content" maxlength="500" placeholder="댓글을 입력하세요." required></textarea>
                <button type="submit" class="btn btn-primary">댓글 등록</button>
            </form>
        </section>
    </c:if>
</main>
<nav class="bottom-nav" aria-label="하단 메뉴">
    <a href="/feed" class="nav-item">피드</a>
    <a href="/search" class="nav-item">검색</a>
    <a href="/upload" class="nav-item">업로드</a>
    <a href="/likes" class="nav-item">좋아요</a>
    <a href="/mypage" class="nav-item">마이</a>
</nav>
<div class="share-modal" id="shareModal" hidden>
    <div class="share-modal-backdrop" id="shareBackdrop"></div>
    <section class="share-modal-panel" role="dialog" aria-modal="true" aria-labelledby="shareModalTitle">
        <div class="share-modal-header">
            <h2 id="shareModalTitle">게시물 공유</h2>
            <button type="button" class="share-close-btn" id="shareCloseBtn" aria-label="공유 모달 닫기">×</button>
        </div>
        <form class="share-form" id="shareForm">
            <label class="share-option-row"><input type="radio" name="shareScope" value="member" checked><span>회원 전용 링크</span></label>
            <label class="share-option-row"><input type="radio" name="shareScope" value="guest"><span>게스트 링크</span></label>
            <div class="guest-option-wrap" id="guestOptionWrap" hidden>
                <label class="share-option-row"><input type="checkbox" name="allowComments" id="allowCommentsChk"><span>댓글 보기 허용</span></label>
                <label class="share-option-row"><input type="checkbox" name="allowDownload" id="allowDownloadChk"><span>다운로드 허용</span></label>
            </div>
            <div class="share-action-row">
                <button type="submit" class="btn btn-primary" id="shareCreateBtn">링크 생성</button>
                <button type="button" class="btn btn-secondary" id="shareCopyBtn" disabled>복사</button>
            </div>
            <input type="text" class="share-url-output" id="shareUrlOutput" readonly>
            <p class="share-feedback" id="shareFeedback"></p>
        </form>
    </section>
</div>
<script>
(() => {
  const shareOpenBtn = document.getElementById('shareOpenBtn');
  const shareModal = document.getElementById('shareModal');
  const shareBackdrop = document.getElementById('shareBackdrop');
  const shareCloseBtn = document.getElementById('shareCloseBtn');
  const shareForm = document.getElementById('shareForm');
  const guestOptionWrap = document.getElementById('guestOptionWrap');
  const shareCopyBtn = document.getElementById('shareCopyBtn');
  const shareUrlOutput = document.getElementById('shareUrlOutput');
  const shareFeedback = document.getElementById('shareFeedback');
  const batchId = '${video.batchId}';
  if (!shareOpenBtn) return;
  const open = () => shareModal.hidden = false;
  const close = () => shareModal.hidden = true;
  shareOpenBtn.addEventListener('click', open);
  shareBackdrop.addEventListener('click', close);
  shareCloseBtn.addEventListener('click', close);
  shareForm.addEventListener('change', () => {
    guestOptionWrap.hidden = shareForm.shareScope.value !== 'guest';
  });
  shareForm.addEventListener('submit', async (e) => {
    e.preventDefault();
    const guest = shareForm.shareScope.value === 'guest';
    const res = await fetch('/share/batch/' + batchId, {method:'POST', headers:{'Content-Type':'application/json'}, body:JSON.stringify({guest, allowComments: document.getElementById('allowCommentsChk').checked, allowDownload: document.getElementById('allowDownloadChk').checked})});
    const data = await res.json();
    const url = guest ? data.guestUrl : data.memberUrl;
    shareUrlOutput.value = url || '';
    shareCopyBtn.disabled = !url;
    shareFeedback.textContent = url ? '공유 링크가 생성되었습니다.' : '링크 생성 실패';
  });
  shareCopyBtn.addEventListener('click', async () => {
    if (!shareUrlOutput.value) return;
    await navigator.clipboard.writeText(shareUrlOutput.value);
    shareFeedback.textContent = '복사되었습니다.';
  });
})();
</script>
</body></html>
