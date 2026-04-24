package com.hogudeul.memorybox.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hogudeul.memorybox.auth.LoginUserSession;
import com.hogudeul.memorybox.calendar.CalendarViewService;
import com.hogudeul.memorybox.dto.calendar.CalendarMonthDto;
import com.hogudeul.memorybox.dto.CommentView;
import com.hogudeul.memorybox.dto.FeedItemView;
import com.hogudeul.memorybox.dto.MediaDetailView;
import com.hogudeul.memorybox.service.DetailService;
import com.hogudeul.memorybox.service.FeedService;
import com.hogudeul.memorybox.service.NotificationService;
import com.hogudeul.memorybox.service.UploadService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
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
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class PageController {

    private static final Logger log = LoggerFactory.getLogger(PageController.class);
    private static final DateTimeFormatter ZIP_FILE_NAME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private static final int FEED_PAGE_SIZE = 24;
    private final FeedService feedService;
    private final DetailService detailService;
    private final NotificationService notificationService;
    private final UploadService uploadService;
    private final CalendarViewService calendarViewService;
    private final ObjectMapper objectMapper;

    public PageController(FeedService feedService,
                          DetailService detailService,
                          NotificationService notificationService,
                          UploadService uploadService,
                          CalendarViewService calendarViewService,
                          ObjectMapper objectMapper) {
        this.feedService = feedService;
        this.detailService = detailService;
        this.notificationService = notificationService;
        this.uploadService = uploadService;
        this.calendarViewService = calendarViewService;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/feed")
    public String feedPage(@RequestParam(required = false) String pwdError,
                           @RequestParam(required = false) String pwdChanged,
                           Model model,
                           HttpSession session) {
        LoginUserSession loginUser = (LoginUserSession) session.getAttribute("loginUser");
        Long userId = loginUser != null ? loginUser.getUserId() : null;
        List<FeedItemView> feedItems = feedService.getFeedItems(null, null, null, null,
                "uploaded_desc", userId, false, false, 1, FEED_PAGE_SIZE);
        int totalCount = feedService.getFeedItemCount(null, null, null, null, userId, false, false);
        List<FeedItemView> optionSource = feedService.getImageFeedItems();

        model.addAttribute("loginUser", loginUser);
        addNotificationModel(model, userId);
        model.addAttribute("pwdError", pwdError);
        model.addAttribute("pwdChanged", "true".equals(pwdChanged));
        model.addAttribute("pageTitle", "피드");
        model.addAttribute("feedItems", feedItems);
        model.addAttribute("totalCount", totalCount);
        return "feed";
    }

    @GetMapping("/search")
    public String searchPage(Model model, HttpSession session) {
        LoginUserSession loginUser = (LoginUserSession) session.getAttribute("loginUser");
        Long userId = loginUser != null ? loginUser.getUserId() : null;
        List<FeedItemView> feedItems = feedService.getFeedItems(null, null, null, null,
                "uploaded_desc", userId, false, false, 1, FEED_PAGE_SIZE);
        int totalCount = feedService.getFeedItemCount(null, null, null, null, userId, false, false);
        List<FeedItemView> optionSource = feedService.getImageFeedItems();
        List<String> albums = feedService.getAlbumFilterOptions(optionSource);
        Map<String, Integer> albumPhotoCounts = new HashMap<>();
        Map<String, Integer> albumVideoCounts = new HashMap<>();
        for (String album : albums) {
            String albumFilter = "전체".equals(album) ? null : album;
            int photoCount = feedService.getFeedItemCount("photo", null, albumFilter, null, userId, false, false);
            int videoCount = feedService.getFeedItemCount("video", null, albumFilter, null, userId, false, false);
            albumPhotoCounts.put(album, photoCount);
            albumVideoCounts.put(album, videoCount);
        }

        model.addAttribute("loginUser", loginUser);
        addNotificationModel(model, userId);
        model.addAttribute("mode", "search");
        model.addAttribute("pageTitle", "검색");
        model.addAttribute("feedItems", feedItems);
        model.addAttribute("totalCount", totalCount);
        model.addAttribute("authors", feedService.getAuthorFilterOptions(optionSource));
        model.addAttribute("albums", albums);
        model.addAttribute("albumPhotoCounts", albumPhotoCounts);
        model.addAttribute("albumVideoCounts", albumVideoCounts);
        model.addAttribute("tags", feedService.getTagFilterOptionsWithoutAll(optionSource));
        return "feed";
    }

    @GetMapping("/likes")
    public String likesPage(Model model, HttpSession session) {
        LoginUserSession loginUser = (LoginUserSession) session.getAttribute("loginUser");
        Long userId = loginUser != null ? loginUser.getUserId() : null;
        List<FeedItemView> feedItems = feedService.getFeedItems(null, null, null, null,
                "uploaded_desc", userId, true, false, 1, FEED_PAGE_SIZE);
        int totalCount = feedService.getFeedItemCount(null, null, null, null, userId, true, false);

        model.addAttribute("loginUser", loginUser);
        addNotificationModel(model, userId);
        model.addAttribute("pageTitle", "좋아요 누른 항목");
        model.addAttribute("mode", "likes");
        model.addAttribute("feedItems", feedItems);
        model.addAttribute("totalCount", totalCount);
        return "feed";
    }

    @GetMapping("/mypage")
    public String myPage(@RequestParam(required = false) Integer calendarYear,
                         @RequestParam(required = false) Integer calendarMonth,
                         Model model,
                         HttpSession session) {
        LoginUserSession loginUser = (LoginUserSession) session.getAttribute("loginUser");
        Long userId = loginUser != null ? loginUser.getUserId() : null;
        List<FeedItemView> feedItems = feedService.getFeedItems(null, null, null, null,
                "uploaded_desc", userId, false, true, 1, FEED_PAGE_SIZE);
        int totalCount = feedService.getFeedItemCount(null, null, null, null, userId, false, true);

        YearMonth targetMonth = resolveTargetMonth(calendarYear, calendarMonth);
        YearMonth prevMonth = targetMonth.minusMonths(1);
        YearMonth nextMonth = targetMonth.plusMonths(1);

        CalendarViewService.CalendarLoadResult calendarLoadResult = calendarViewService.loadCalendarMonth(targetMonth);

        model.addAttribute("loginUser", loginUser);
        addNotificationModel(model, userId);
        model.addAttribute("pageTitle", "마이페이지 & 업로드한 게시물");
        model.addAttribute("mode", "mypage");
        model.addAttribute("feedItems", feedItems);
        model.addAttribute("totalCount", totalCount);
        model.addAttribute("calendarState", calendarLoadResult.getStatus().name());
        model.addAttribute("calendarYear", targetMonth.getYear());
        model.addAttribute("calendarMonth", targetMonth.getMonthValue());
        model.addAttribute("calendarPrevYear", prevMonth.getYear());
        model.addAttribute("calendarPrevMonth", prevMonth.getMonthValue());
        model.addAttribute("calendarNextYear", nextMonth.getYear());
        model.addAttribute("calendarNextMonth", nextMonth.getMonthValue());

        if (calendarLoadResult.getStatus() == CalendarViewService.CalendarLoadResult.Status.READY) {
            CalendarMonthDto monthDto = calendarLoadResult.getMonthDto();
            model.addAttribute("calendarMonthData", monthDto);
            model.addAttribute("calendarMonthDataJson", toJson(monthDto));
        }
        return "feed";
    }


    private YearMonth resolveTargetMonth(Integer year, Integer month) {
        LocalDate now = LocalDate.now();
        int safeYear = year != null ? year : now.getYear();
        int safeMonth = month != null ? month : now.getMonthValue();

        if (safeMonth < 1 || safeMonth > 12) {
            safeMonth = now.getMonthValue();
        }
        if (safeYear < 2000 || safeYear > 2100) {
            safeYear = now.getYear();
        }
        return YearMonth.of(safeYear, safeMonth);
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            log.warn("캘린더 JSON 직렬화 실패", e);
            return "{}";
        }
    }

    @GetMapping("/api/calendar/month")
    @ResponseBody
    public Map<String, Object> calendarMonthApi(@RequestParam int year,
                                                 @RequestParam int month) {
        YearMonth targetMonth = resolveTargetMonth(year, month);
        YearMonth prevMonth = targetMonth.minusMonths(1);
        YearMonth nextMonth = targetMonth.plusMonths(1);

        CalendarViewService.CalendarLoadResult loadResult = calendarViewService.loadCalendarMonth(targetMonth);

        Map<String, Object> response = new HashMap<>();
        response.put("state", loadResult.getStatus().name());
        response.put("year", targetMonth.getYear());
        response.put("month", targetMonth.getMonthValue());
        response.put("prevYear", prevMonth.getYear());
        response.put("prevMonth", prevMonth.getMonthValue());
        response.put("nextYear", nextMonth.getYear());
        response.put("nextMonth", nextMonth.getMonthValue());
        response.put("monthData", loadResult.getMonthDto());
        return response;
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
        int safeSize = Math.max(1, Math.min(size, 60));
        List<FeedItemView> items = feedService.getFeedItems(type, author, album, tag, sort,
                userId, likesOnly, mineOnly, page, safeSize);
        int totalCount = feedService.getFeedItemCount(type, author, album, tag, userId, likesOnly, mineOnly);
        int loadedCount = Math.min(totalCount, Math.max(0, (page - 1) * safeSize + items.size()));

        Map<String, Object> response = new HashMap<>();
        response.put("items", items);
        response.put("totalCount", totalCount);
        response.put("loadedCount", loadedCount);
        response.put("hasMore", loadedCount < totalCount);
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
        addNotificationModel(model, userId);
        model.addAttribute("info", info);
        model.addAttribute("error", error);

        if (detail == null) {
            model.addAttribute("notFound", true);
            return "detail";
        }

        List<CommentView> comments = detailService.getComments(itemId, userId);
        model.addAttribute("detail", detail);
        model.addAttribute("comments", comments);
        model.addAttribute("albums", uploadService.getActiveAlbums(userId));
        model.addAttribute("tags", uploadService.getActiveTags(userId));
        return "detail";
    }

    @PostMapping("/feed/{itemId}/edit-meta")
    public String editMeta(@PathVariable Long itemId,
                           @RequestParam("title") String title,
                           @RequestParam("albumId") Long albumId,
                           HttpSession session,
                           RedirectAttributes redirectAttributes) {
        LoginUserSession loginUser = (LoginUserSession) session.getAttribute("loginUser");
        if (loginUser == null) {
            return "redirect:/login";
        }

        try {
            boolean ok = detailService.updateMediaMeta(itemId, loginUser.getUserId(), title, albumId);
            redirectAttributes.addAttribute(ok ? "info" : "error",
                    ok ? "제목/앨범이 수정되었습니다." : "작성자만 제목/앨범을 수정할 수 있습니다.");
        } catch (Exception e) {
            redirectAttributes.addAttribute("error", "제목/앨범 수정 중 오류가 발생했습니다.");
        }
        return "redirect:/feed/" + itemId;
    }

    @PostMapping("/feed/{itemId}/edit-tags")
    public String editTags(@PathVariable Long itemId,
                           @RequestParam(required = false) List<Long> selectedTagIds,
                           @RequestParam(required = false) String newTags,
                           HttpSession session,
                           RedirectAttributes redirectAttributes) {
        LoginUserSession loginUser = (LoginUserSession) session.getAttribute("loginUser");
        if (loginUser == null) {
            return "redirect:/login";
        }

        try {
            boolean ok = detailService.updateMediaTags(itemId, loginUser.getUserId(), selectedTagIds, newTags);
            redirectAttributes.addAttribute(ok ? "info" : "error",
                    ok ? "태그가 수정되었습니다." : "태그 수정에 실패했습니다.");
        } catch (Exception e) {
            redirectAttributes.addAttribute("error", "태그 수정 중 오류가 발생했습니다.");
        }
        return "redirect:/feed/" + itemId;
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

    @PostMapping("/api/feed/{itemId}/like")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> likeApi(@PathVariable Long itemId,
                                                       @RequestParam("action") String action,
                                                       HttpSession session) {
        LoginUserSession loginUser = (LoginUserSession) session.getAttribute("loginUser");
        if (loginUser == null) {
            return ResponseEntity.status(401).body(buildInteractionResponse(false, "로그인이 필요합니다.", itemId, null));
        }
        try {
            boolean like = "like".equalsIgnoreCase(action);
            boolean ok = detailService.setLike(itemId, loginUser.getUserId(), like);
            if (!ok) {
                return ResponseEntity.badRequest().body(buildInteractionResponse(false, "게시물을 찾을 수 없습니다.", itemId, loginUser.getUserId()));
            }
            return ResponseEntity.ok(buildInteractionResponse(true, "", itemId, loginUser.getUserId()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(buildInteractionResponse(false, "좋아요 처리 중 오류가 발생했습니다.", itemId, loginUser.getUserId()));
        }
    }

    @GetMapping("/api/feed/{itemId}/comments")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> commentsApi(@PathVariable Long itemId, HttpSession session) {
        LoginUserSession loginUser = (LoginUserSession) session.getAttribute("loginUser");
        if (loginUser == null) {
            return ResponseEntity.status(401).body(buildInteractionResponse(false, "로그인이 필요합니다.", itemId, null));
        }

        Map<String, Object> response = buildInteractionResponse(true, "", itemId, loginUser.getUserId());
        response.put("comments", detailService.getComments(itemId, loginUser.getUserId()));
        return ResponseEntity.ok(response);
    }

    @PostMapping("/api/feed/{itemId}/comments")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> addCommentApi(@PathVariable Long itemId,
                                                              @RequestParam("content") String content,
                                                              @RequestParam(required = false) Long parentId,
                                                              HttpSession session) {
        LoginUserSession loginUser = (LoginUserSession) session.getAttribute("loginUser");
        if (loginUser == null) {
            return ResponseEntity.status(401).body(buildInteractionResponse(false, "로그인이 필요합니다.", itemId, null));
        }

        try {
            boolean ok = detailService.addComment(itemId, loginUser.getUserId(), content, parentId);
            if (!ok) {
                if (content == null || content.isBlank()) {
                    return ResponseEntity.badRequest().body(buildInteractionResponse(false, "댓글 내용을 입력해 주세요.", itemId, loginUser.getUserId()));
                }
                return ResponseEntity.badRequest().body(buildInteractionResponse(false, "댓글 등록에 실패했습니다. (대댓글은 1단계까지만 가능합니다)", itemId, loginUser.getUserId()));
            }

            Map<String, Object> response = buildInteractionResponse(true, "댓글이 등록되었습니다.", itemId, loginUser.getUserId());
            response.put("comments", detailService.getComments(itemId, loginUser.getUserId()));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(buildInteractionResponse(false, "댓글 처리 중 오류가 발생했습니다.", itemId, loginUser.getUserId()));
        }
    }

    @GetMapping("/feed/{itemId}/download")
    public ResponseEntity<Resource> downloadOriginal(@PathVariable Long itemId,
                                                     HttpServletRequest request,
                                                     HttpSession session) {
        LoginUserSession loginUser = (LoginUserSession) session.getAttribute("loginUser");
        log.info("Single download request. mediaId={}, hasLogin={}", itemId, loginUser != null);
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
            log.warn("Single download file not found. mediaId={}, userId={}, path={}",
                    itemId, loginUser.getUserId(), fileInfo.getFilePath());
            return ResponseEntity.status(302)
                    .header(HttpHeaders.LOCATION, "/feed/" + itemId + "?error="
                            + URLEncoder.encode("원본 파일을 찾을 수 없습니다.", StandardCharsets.UTF_8))
                    .build();
        }

        MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
        if (fileInfo.getMimeType() != null && !fileInfo.getMimeType().isBlank()) {
            try {
                mediaType = MediaType.parseMediaType(fileInfo.getMimeType());
            } catch (Exception ex) {
                log.warn("Invalid mime type for download. mediaId={}, mimeType={}", itemId, fileInfo.getMimeType());
            }
        }

        String contentDisposition = buildAttachmentContentDisposition(fileInfo.getFileName(), "download");
        log.debug("Single download headers prepared. mediaId={}, userId={}, mimeType={}, fileName={}",
                itemId, loginUser.getUserId(), mediaType, fileInfo.getFileName());

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

        log.info("Single download response ready. mediaId={}, userId={}, contentLength={}",
                itemId, loginUser.getUserId(), contentLength);
        Resource resource = new FileSystemResource(fileInfo.getFilePath());
        return responseBuilder.body(resource);
    }

    @PostMapping(value = "/feed/download-zip", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<StreamingResponseBody> downloadSelectedAsZip(@RequestBody DownloadZipRequest request,
                                                                       HttpServletRequest httpRequest,
                                                                       HttpSession session) {
        LoginUserSession loginUser = (LoginUserSession) session.getAttribute("loginUser");
        log.info("Zip download request. hasLogin={}", loginUser != null);
        if (loginUser == null) {
            return errorStreamingResponse(401, "로그인이 필요합니다.");
        }

        List<Long> mediaIds = request == null ? null : request.getMediaIds();
        log.info("Zip download request detail. userId={}, mediaCount={}, mediaIds={}",
                loginUser.getUserId(), mediaIds == null ? 0 : mediaIds.size(), mediaIds);
        final List<DetailService.DownloadFileInfo> files;
        try {
            files = detailService.getDownloadFileInfos(mediaIds, loginUser.getUserId());
        } catch (DetailService.DownloadException e) {
            log.warn("Zip download validation failed. userId={}, msg={}", loginUser.getUserId(), e.getMessage());
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
            log.info("Zip stream start. userId={}, fileCount={}", loginUser.getUserId(), files.size());
            try {
                detailService.streamZip(files, outputStream);
                log.info("Zip stream complete. userId={}, fileCount={}", loginUser.getUserId(), files.size());
            } catch (DetailService.DownloadException e) {
                log.warn("Zip stream domain error. userId={}, msg={}", loginUser.getUserId(), e.getMessage());
                throw new IOException(e.getMessage(), e);
            } catch (IOException e) {
                if (isClientAbortIOException(e)) {
                    log.warn("Client aborted zip download. userId={}, msg={}",
                            loginUser.getUserId(), e.getMessage());
                    return;
                }
                log.warn("Zip stream failed. userId={}, msg={}", loginUser.getUserId(), e.getMessage());
                throw e;
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

    @ExceptionHandler(AsyncRequestNotUsableException.class)
    @ResponseBody
    public ResponseEntity<Void> handleAsyncRequestNotUsable(AsyncRequestNotUsableException ex) {
        if (isClientAbortIOException(ex)) {
            log.debug("Client aborted async response. msg={}", ex.getMessage());
            return ResponseEntity.noContent().build();
        }
        log.warn("Unhandled async response error. msg={}", ex.getMessage());
        return ResponseEntity.internalServerError().build();
    }

    private boolean isClientAbortIOException(Throwable e) {
        Throwable current = e;
        while (current != null) {
            String message = current.getMessage();
            if (message != null) {
                String normalized = message.toLowerCase(Locale.ROOT);
                if (normalized.contains("broken pipe")
                        || normalized.contains("connection reset by peer")
                        || normalized.contains("forcibly closed")
                        || normalized.contains("현재 연결은")
                        || normalized.contains("호스트 시스템의 소프트웨어")
                        || normalized.contains("원격 호스트에 의해 강제로 끊겼")
                        || normalized.contains("connection aborted")) {
                    return true;
                }
            }
            current = current.getCause();
        }
        return false;
    }


    private void addNotificationModel(Model model, Long userId) {
        model.addAttribute("notificationPanel", notificationService.getNotificationPanel(userId));
    }
    private Map<String, Object> buildInteractionResponse(boolean success, String message, Long itemId, Long userId) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", success);
        response.put("message", message);

        if (userId != null) {
            MediaDetailView detail = detailService.getMediaDetail(itemId, userId);
            if (detail != null) {
                response.put("likeCount", detail.getLikeCount());
                response.put("commentCount", detail.getCommentCount());
                response.put("likedByMe", detail.isLikedByMe());
            }
        }
        return response;
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
