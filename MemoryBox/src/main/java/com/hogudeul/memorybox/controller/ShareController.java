package com.hogudeul.memorybox.controller;

import com.hogudeul.memorybox.auth.LoginUserSession;
import com.hogudeul.memorybox.dto.CommentView;
import com.hogudeul.memorybox.dto.DetailMediaItemView;
import com.hogudeul.memorybox.dto.MediaDetailView;
import com.hogudeul.memorybox.dto.ShareLinkCreateRequest;
import com.hogudeul.memorybox.dto.ShareLinkCreateResponse;
import com.hogudeul.memorybox.model.ShareLink;
import com.hogudeul.memorybox.service.DetailService;
import com.hogudeul.memorybox.service.ShareLinkService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@Controller
public class ShareController {
    private static final DateTimeFormatter ZIP_FILE_NAME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private final ShareLinkService shareLinkService;
    private final DetailService detailService;

    public ShareController(ShareLinkService shareLinkService,
                           DetailService detailService) {
        this.shareLinkService = shareLinkService;
        this.detailService = detailService;
    }

    @PostMapping("/share/batch/{batchId}")
    @ResponseBody
    public ResponseEntity<?> createShareLink(@PathVariable Long batchId,
                                             @RequestBody(required = false) ShareLinkCreateRequest request,
                                             HttpSession session) {
        LoginUserSession loginUser = (LoginUserSession) session.getAttribute("loginUser");
        if (loginUser == null) {
            Map<String, Object> body = new HashMap<>();
            body.put("message", "로그인 사용자만 공유 링크를 생성할 수 있습니다.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
        }

        MediaDetailView detail = detailService.getMediaDetail(batchId, loginUser.getUserId());
        if (detail == null) {
            Map<String, Object> body = new HashMap<>();
            body.put("message", "공유할 미디어를 찾을 수 없습니다.");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
        }

        boolean guest = request == null || request.getGuest() == null || request.getGuest();
        boolean allowComments = request != null && Boolean.TRUE.equals(request.getAllowComments());
        boolean allowDownload = request != null && Boolean.TRUE.equals(request.getAllowDownload());
        Integer expiresMinutes = request != null ? request.getExpiresMinutes() : null;

        String memberUrl = shareLinkService.buildMemberShareUrl(batchId);
        if (!guest) {
            return ResponseEntity.ok(new ShareLinkCreateResponse(memberUrl, null, null));
        }

        ShareLink shareLink = shareLinkService.createGuestShareLink(
                batchId,
                loginUser.getUserId(),
                allowComments,
                allowDownload,
                expiresMinutes
        );

        return ResponseEntity.ok(new ShareLinkCreateResponse(
                memberUrl,
                shareLinkService.buildGuestShareUrl(shareLink.getShareToken()),
                shareLink.getExpiresAt()
        ));
    }

    @GetMapping("/share/{token}")
    public String guestShareDetail(@PathVariable String token,
                                   Model model) {
        ShareLink shareLink = shareLinkService.findActiveByToken(token);
        if (shareLink == null) {
            model.addAttribute("reason", "만료되었거나 사용할 수 없는 공유 링크입니다.");
            return "shareInvalid";
        }

        MediaDetailView detail = detailService.getMediaDetail(shareLink.getBatchId(), null);
        if (detail == null) {
            model.addAttribute("reason", "공유된 미디어를 찾을 수 없습니다.");
            return "shareInvalid";
        }

        boolean showComments = "Y".equalsIgnoreCase(shareLink.getAllowComments());
        boolean allowDownload = "Y".equalsIgnoreCase(shareLink.getAllowDownload());
        List<CommentView> comments = showComments ? detailService.getComments(shareLink.getBatchId(), null) : List.of();
        List<DetailMediaItemView> detailItems = detailService.getBatchMediaItems(shareLink.getBatchId(), null);

        model.addAttribute("detail", detail);
        model.addAttribute("detailItems", detailItems);
        model.addAttribute("comments", comments);
        model.addAttribute("allowComments", showComments);
        model.addAttribute("allowDownload", allowDownload);
        model.addAttribute("shareToken", token);
        return "shareDetail";
    }

    @GetMapping("/share/{token}/media/{mediaId}/download")
    public ResponseEntity<Resource> guestSingleDownload(@PathVariable String token,
                                                        @PathVariable Long mediaId,
                                                        HttpServletRequest request) {
        ShareLink shareLink = validateDownloadShareLink(token);
        if (shareLink == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        boolean belongsToBatch = detailService.getBatchMediaItems(shareLink.getBatchId(), null)
                .stream()
                .anyMatch(item -> mediaId.equals(item.getMediaId()));
        if (!belongsToBatch) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        DetailService.DownloadFileInfo fileInfo = detailService.getDownloadFileInfo(mediaId, null);
        if (fileInfo == null || !fileInfo.existsReadable()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
        if (fileInfo.getMimeType() != null && !fileInfo.getMimeType().isBlank()) {
            try {
                mediaType = MediaType.parseMediaType(fileInfo.getMimeType());
            } catch (Exception ignore) {
                mediaType = MediaType.APPLICATION_OCTET_STREAM;
            }
        }
        String contentDisposition = buildAttachmentContentDisposition(fileInfo.getFileName(), "download");
        detailService.logDownloadAttempt(mediaId, null, request.getRemoteAddr(), request.getHeader(HttpHeaders.USER_AGENT), true, null);
        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition)
                .body(new FileSystemResource(fileInfo.getFilePath()));
    }

    @GetMapping("/share/{token}/download-all")
    public ResponseEntity<StreamingResponseBody> guestDownloadAll(@PathVariable String token,
                                                                  HttpServletRequest request) {
        ShareLink shareLink = validateDownloadShareLink(token);
        if (shareLink == null) {
            return errorStreamingResponse(403, "다운로드가 허용되지 않은 링크입니다.");
        }
        final List<DetailService.DownloadFileInfo> files;
        try {
            files = detailService.getBatchDownloadFileInfos(shareLink.getBatchId(), null);
        } catch (DetailService.DownloadException e) {
            return errorStreamingResponse(400, e.getMessage());
        }
        detailService.logDownloadAttempt(null, null, request.getRemoteAddr(), request.getHeader(HttpHeaders.USER_AGENT), true, null);
        String zipFileName = "memorybox_share_" + LocalDateTime.now().format(ZIP_FILE_NAME_FORMAT) + ".zip";
        String disposition = "attachment; filename=\"memorybox_share.zip\"; filename*=UTF-8''"
                + URLEncoder.encode(zipFileName, StandardCharsets.UTF_8).replace("+", "%20");
        StreamingResponseBody body = outputStream -> detailService.streamZip(files, outputStream);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/zip"))
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition)
                .body(body);
    }

    @PostMapping(value = "/share/{token}/download-zip", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<StreamingResponseBody> guestDownloadSelected(@PathVariable String token,
                                                                       @RequestBody(required = false) DownloadZipRequest request,
                                                                       HttpServletRequest httpRequest) {
        ShareLink shareLink = validateDownloadShareLink(token);
        if (shareLink == null) {
            return errorStreamingResponse(403, "다운로드가 허용되지 않은 링크입니다.");
        }
        List<Long> mediaIds = request == null ? null : request.getMediaIds();
        final List<Long> safeMediaIds = mediaIds == null ? List.of() : mediaIds;
        if (safeMediaIds.isEmpty()) {
            return errorStreamingResponse(400, "다운로드할 파일을 선택해 주세요.");
        }
        List<Long> batchMediaIds = detailService.getBatchMediaItems(shareLink.getBatchId(), null)
                .stream()
                .map(DetailMediaItemView::getMediaId)
                .toList();
        if (!batchMediaIds.containsAll(safeMediaIds)) {
            return errorStreamingResponse(403, "다른 게시물 미디어는 다운로드할 수 없습니다.");
        }
        final List<DetailService.DownloadFileInfo> files;
        try {
            files = detailService.getDownloadFileInfos(safeMediaIds, null);
        } catch (DetailService.DownloadException e) {
            return errorStreamingResponse(400, e.getMessage());
        }
        detailService.logDownloadAttempt(null, null, httpRequest.getRemoteAddr(), httpRequest.getHeader(HttpHeaders.USER_AGENT), true, null);
        String zipFileName = "memorybox_share_" + LocalDateTime.now().format(ZIP_FILE_NAME_FORMAT) + ".zip";
        String disposition = "attachment; filename=\"memorybox_share.zip\"; filename*=UTF-8''"
                + URLEncoder.encode(zipFileName, StandardCharsets.UTF_8).replace("+", "%20");
        StreamingResponseBody body = outputStream -> detailService.streamZip(files, outputStream);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/zip"))
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition)
                .body(body);
    }

    private ShareLink validateDownloadShareLink(String token) {
        ShareLink shareLink = shareLinkService.findActiveByToken(token);
        if (shareLink == null || !"Y".equalsIgnoreCase(shareLink.getAllowDownload())) {
            return null;
        }
        return shareLink;
    }

    private ResponseEntity<StreamingResponseBody> errorStreamingResponse(int statusCode, String message) {
        StreamingResponseBody body = outputStream -> outputStream.write(message.getBytes(StandardCharsets.UTF_8));
        return ResponseEntity.status(statusCode).contentType(MediaType.TEXT_PLAIN).body(body);
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
