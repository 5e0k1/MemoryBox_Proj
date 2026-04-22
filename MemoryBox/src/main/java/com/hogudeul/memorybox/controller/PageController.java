package com.hogudeul.memorybox.controller;

import com.hogudeul.memorybox.auth.LoginUserSession;
import com.hogudeul.memorybox.dto.CommentView;
import com.hogudeul.memorybox.dto.FeedItemView;
import com.hogudeul.memorybox.dto.MediaDetailView;
import com.hogudeul.memorybox.service.DetailService;
import com.hogudeul.memorybox.service.FeedService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class PageController {

    private static final Logger log = LoggerFactory.getLogger(PageController.class);
    private static final DateTimeFormatter ZIP_FILE_NAME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private static final int FEED_PAGE_SIZE = 24;
    private final FeedService feedService;
    private final DetailService detailService;

    public PageController(FeedService feedService, DetailService detailService) {
        this.feedService = feedService;
        this.detailService = detailService;
    }

    @GetMapping("/feed")
    public String feedPage(@RequestParam(required = false) String pwdError,
                           @RequestParam(required = false) String pwdChanged,
                           Model model,
                           HttpSession session) {
        LoginUserSession loginUser = (LoginUserSession) session.getAttribute("loginUser");
        List<FeedItemView> feedItems = feedService.getFeedItems(null, null, null, null,
                "uploaded_desc", null, false, false, 1, FEED_PAGE_SIZE);
        List<FeedItemView> optionSource = feedService.getImageFeedItems();

        model.addAttribute("loginUser", loginUser);
        model.addAttribute("pwdError", pwdError);
        model.addAttribute("pwdChanged", "true".equals(pwdChanged));
        model.addAttribute("feedItems", feedItems);
        model.addAttribute("authors", feedService.getAuthorFilterOptions(optionSource));
        model.addAttribute("tags", feedService.getTagFilterOptions(optionSource));
        model.addAttribute("years", feedService.getAlbumFilterOptions(optionSource));
        return "feed";
    }

    @GetMapping("/likes")
    public String likesPage(Model model, HttpSession session) {
        LoginUserSession loginUser = (LoginUserSession) session.getAttribute("loginUser");
        Long userId = loginUser != null ? loginUser.getUserId() : null;
        List<FeedItemView> feedItems = feedService.getFeedItems(null, null, null, null,
                "uploaded_desc", userId, true, false, 1, 60);

        model.addAttribute("loginUser", loginUser);
        model.addAttribute("pageTitle", "좋아요");
        model.addAttribute("mode", "likes");
        model.addAttribute("feedItems", feedItems);
        return "feed";
    }

    @GetMapping("/mypage")
    public String myPage(Model model, HttpSession session) {
        LoginUserSession loginUser = (LoginUserSession) session.getAttribute("loginUser");
        Long userId = loginUser != null ? loginUser.getUserId() : null;
        List<FeedItemView> feedItems = feedService.getFeedItems(null, null, null, null,
                "uploaded_desc", userId, false, true, 1, 60);

        model.addAttribute("loginUser", loginUser);
        model.addAttribute("pageTitle", "마이페이지");
        model.addAttribute("mode", "mypage");
        model.addAttribute("feedItems", feedItems);
        return "feed";
    }

    @GetMapping("/api/feed/items")
    @ResponseBody
    public Map<String, Object> feedItemsApi(@RequestParam(defaultValue = "1") int page,
                                            @RequestParam(defaultValue = "24") int size,
                                            @RequestParam(required = false) String type,
                                            @RequestParam(required = false) String author,
                                            @RequestParam(required = false) String album,
                                            @RequestParam(required = false) String tag,
                                            @RequestParam(required = false, defaultValue = "uploaded_desc") String sort,
                                            @RequestParam(required = false, defaultValue = "false") boolean likesOnly,
                                            @RequestParam(required = false, defaultValue = "false") boolean mineOnly,
                                            HttpSession session) {
        LoginUserSession loginUser = (LoginUserSession) session.getAttribute("loginUser");
        Long userId = loginUser != null ? loginUser.getUserId() : null;
        List<FeedItemView> items = feedService.getFeedItems(type, author, album, tag, sort,
                userId, likesOnly, mineOnly, page, size);

        Map<String, Object> response = new HashMap<>();
        response.put("items", items);
        response.put("hasMore", items.size() >= Math.max(1, Math.min(size, 60)));
        return response;
    }

    @GetMapping("/feed/{itemId}")
    public String feedDetail(@PathVariable Long itemId,
                             @RequestParam(required = false) String info,
                             @RequestParam(required = false) String error,
                             Model model,
                             HttpSession session) {
        LoginUserSession loginUser = (LoginUserSession) session.getAttribute("loginUser");
        Long userId = loginUser != null ? loginUser.getUserId() : null;

        MediaDetailView detail = detailService.getMediaDetail(itemId, userId);
        model.addAttribute("loginUser", loginUser);
        model.addAttribute("info", info);
        model.addAttribute("error", error);

        if (detail == null) {
            model.addAttribute("notFound", true);
            return "detail";
        }

        List<CommentView> comments = detailService.getComments(itemId, userId);
        model.addAttribute("detail", detail);
        model.addAttribute("comments", comments);
        return "detail";
    }

    @PostMapping("/feed/{itemId}/like")
    public String like(@PathVariable Long itemId,
                       @RequestParam("action") String action,
                       HttpSession session,
                       RedirectAttributes redirectAttributes) {
        LoginUserSession loginUser = (LoginUserSession) session.getAttribute("loginUser");
        if (loginUser == null) {
            return "redirect:/login";
        }
        boolean like = "like".equalsIgnoreCase(action);

        try {
            boolean ok = detailService.setLike(itemId, loginUser.getUserId(), like);
            if (!ok) {
                redirectAttributes.addAttribute("error", "게시물을 찾을 수 없습니다.");
            }
        } catch (Exception e) {
            redirectAttributes.addAttribute("error", "좋아요 처리 중 오류가 발생했습니다.");
        }
        return "redirect:/feed/" + itemId;
    }

    @PostMapping("/feed/{itemId}/comments")
    public String addComment(@PathVariable Long itemId,
                             @RequestParam("content") String content,
                             @RequestParam(required = false) Long parentId,
                             HttpSession session,
                             RedirectAttributes redirectAttributes) {
        LoginUserSession loginUser = (LoginUserSession) session.getAttribute("loginUser");
        if (loginUser == null) {
            return "redirect:/login";
        }

        try {
            boolean ok = detailService.addComment(itemId, loginUser.getUserId(), content, parentId);
            if (!ok) {
                if (content == null || content.isBlank()) {
                    redirectAttributes.addAttribute("error", "댓글 내용을 입력해 주세요.");
                } else {
                    redirectAttributes.addAttribute("error", "댓글 등록에 실패했습니다. (대댓글은 1단계까지만 가능합니다)");
                }
            } else {
                redirectAttributes.addAttribute("info", "댓글이 등록되었습니다.");
            }
        } catch (Exception e) {
            redirectAttributes.addAttribute("error", "댓글 처리 중 오류가 발생했습니다.");
        }
        return "redirect:/feed/" + itemId;
    }

    @GetMapping("/feed/{itemId}/download")
    public ResponseEntity<StreamingResponseBody> downloadOriginal(@PathVariable Long itemId,
                                                                  HttpServletRequest request,
                                                                  HttpSession session) {
        LoginUserSession loginUser = (LoginUserSession) session.getAttribute("loginUser");
        if (loginUser == null) {
            return ResponseEntity.status(302).header(HttpHeaders.LOCATION, "/login").build();
        }
        DetailService.DownloadFileInfo fileInfo = detailService.getDownloadFileInfo(itemId, loginUser.getUserId());

        if (fileInfo == null) {
            return ResponseEntity.status(302)
                    .header(HttpHeaders.LOCATION, "/feed/" + itemId + "?error="
                            + URLEncoder.encode("다운로드 가능한 원본 파일이 없습니다.", StandardCharsets.UTF_8))
                    .build();
        }

        if (!fileInfo.existsReadable()) {
            return ResponseEntity.status(302)
                    .header(HttpHeaders.LOCATION, "/feed/" + itemId + "?error="
                            + URLEncoder.encode("원본 파일을 찾을 수 없습니다.", StandardCharsets.UTF_8))
                    .build();
        }

        MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
        if (fileInfo.getMimeType() != null && !fileInfo.getMimeType().isBlank()) {
            mediaType = MediaType.parseMediaType(fileInfo.getMimeType());
        }

        String contentDisposition = buildAttachmentContentDisposition(fileInfo.getFileName(), "download");

        detailService.logDownloadAttempt(
                itemId,
                loginUser.getUserId(),
                request.getRemoteAddr(),
                request.getHeader(HttpHeaders.USER_AGENT),
                true,
                null
        );

        long contentLength = -1L;
        try {
            contentLength = Files.size(fileInfo.getFilePath());
        } catch (IOException ignore) {
            // 길이 조회 실패 시 chunked 전송으로 처리.
        }

        ResponseEntity.BodyBuilder responseBuilder = ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition);
        if (contentLength >= 0) {
            responseBuilder.contentLength(contentLength);
        }

        return responseBuilder.body((StreamingResponseBody) outputStream -> {
            try (var inputStream = fileInfo.openInputStream()) {
                inputStream.transferTo(outputStream);
            } catch (IOException e) {
                if (isClientAbortIOException(e)) {
                    log.debug("Client aborted single download. mediaId={}, userId={}, msg={}",
                            itemId, loginUser.getUserId(), e.getMessage());
                    return;
                }
                throw e;
            }
        });
    }

    @PostMapping(value = "/feed/download-zip", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<StreamingResponseBody> downloadSelectedAsZip(@RequestBody DownloadZipRequest request,
                                                                       HttpServletRequest httpRequest,
                                                                       HttpSession session) {
        LoginUserSession loginUser = (LoginUserSession) session.getAttribute("loginUser");
        if (loginUser == null) {
            return errorStreamingResponse(401, "로그인이 필요합니다.");
        }

        List<Long> mediaIds = request == null ? null : request.getMediaIds();
        final List<DetailService.DownloadFileInfo> files;
        try {
            files = detailService.getDownloadFileInfos(mediaIds, loginUser.getUserId());
        } catch (DetailService.DownloadException e) {
            return errorStreamingResponse(400, e.getMessage());
        }

        String requesterIp = httpRequest.getRemoteAddr();
        String requesterAgent = httpRequest.getHeader(HttpHeaders.USER_AGENT);
        for (Long mediaId : mediaIds) {
            detailService.logDownloadAttempt(mediaId, loginUser.getUserId(), requesterIp, requesterAgent, true, null);
        }

        String zipFileName = "memorybox_" + LocalDateTime.now().format(ZIP_FILE_NAME_FORMAT) + ".zip";
        String disposition = "attachment; filename=\"memorybox_download.zip\"; filename*=UTF-8''"
                + URLEncoder.encode(zipFileName, StandardCharsets.UTF_8).replace("+", "%20");

        StreamingResponseBody body = outputStream -> {
            try {
                detailService.streamZip(files, outputStream);
            } catch (DetailService.DownloadException e) {
                throw new IOException(e.getMessage(), e);
            }
        };

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/zip"))
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition)
                .body(body);
    }

    private ResponseEntity<StreamingResponseBody> errorStreamingResponse(int statusCode, String message) {
        StreamingResponseBody body = outputStream -> outputStream.write(message.getBytes(StandardCharsets.UTF_8));
        return ResponseEntity.status(statusCode)
                .contentType(MediaType.TEXT_PLAIN)
                .body(body);
    }

    private String buildAttachmentContentDisposition(String utf8FileName, String fallbackName) {
        String normalizedFileName = (utf8FileName == null || utf8FileName.isBlank())
                ? fallbackName
                : utf8FileName;
        String asciiFallback = normalizedFileName.replaceAll("[^\\x20-\\x7E]", "_");
        if (asciiFallback.isBlank()) {
            asciiFallback = fallbackName;
        }
        return "attachment; filename=\"" + asciiFallback + "\"; filename*=UTF-8''"
                + URLEncoder.encode(normalizedFileName, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private boolean isClientAbortIOException(IOException e) {
        Throwable current = e;
        while (current != null) {
            String message = current.getMessage();
            if (message != null) {
                String normalized = message.toLowerCase(Locale.ROOT);
                if (normalized.contains("broken pipe")
                        || normalized.contains("connection reset by peer")
                        || normalized.contains("forcibly closed")) {
                    return true;
                }
            }
            current = current.getCause();
        }
        return false;
    }

    public static class DownloadZipRequest {
        private List<Long> mediaIds;

        public List<Long> getMediaIds() {
            return mediaIds;
        }

        public void setMediaIds(List<Long> mediaIds) {
            this.mediaIds = mediaIds;
        }
    }
}
