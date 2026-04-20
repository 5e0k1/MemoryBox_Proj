package com.hogudeul.memorybox.controller;

import com.hogudeul.memorybox.auth.LoginUserSession;
import com.hogudeul.memorybox.dto.CommentView;
import com.hogudeul.memorybox.dto.FeedItemView;
import com.hogudeul.memorybox.dto.MediaDetailView;
import com.hogudeul.memorybox.service.DetailService;
import com.hogudeul.memorybox.service.FeedService;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class PageController {

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
        List<FeedItemView> feedItems = feedService.getImageFeedItems();

        model.addAttribute("loginUser", loginUser);
        model.addAttribute("pwdError", pwdError);
        model.addAttribute("pwdChanged", "true".equals(pwdChanged));
        model.addAttribute("feedItems", feedItems);
        model.addAttribute("authors", feedService.getAuthorFilterOptions(feedItems));
        model.addAttribute("tags", feedService.getTagFilterOptions(feedItems));
        model.addAttribute("years", feedService.getAlbumFilterOptions(feedItems));
        return "feed";
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
                             HttpSession session,
                             RedirectAttributes redirectAttributes) {
        LoginUserSession loginUser = (LoginUserSession) session.getAttribute("loginUser");
        if (loginUser == null) {
            return "redirect:/login";
        }

        try {
            boolean ok = detailService.addComment(itemId, loginUser.getUserId(), content);
            if (!ok) {
                if (content == null || content.isBlank()) {
                    redirectAttributes.addAttribute("error", "댓글 내용을 입력해 주세요.");
                } else {
                    redirectAttributes.addAttribute("error", "댓글 등록에 실패했습니다.");
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
    public ResponseEntity<?> downloadOriginal(@PathVariable Long itemId,
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

        try {
            if (!Files.exists(fileInfo.getFilePath()) || !Files.isReadable(fileInfo.getFilePath())) {
                return ResponseEntity.status(302)
                        .header(HttpHeaders.LOCATION, "/feed/" + itemId + "?error="
                                + URLEncoder.encode("원본 파일을 찾을 수 없습니다.", StandardCharsets.UTF_8))
                        .build();
            }

            byte[] data = Files.readAllBytes(fileInfo.getFilePath());
            MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
            if (fileInfo.getMimeType() != null && !fileInfo.getMimeType().isBlank()) {
                mediaType = MediaType.parseMediaType(fileInfo.getMimeType());
            }

            ContentDisposition disposition = ContentDisposition.attachment()
                    .filename(fileInfo.getFileName(), StandardCharsets.UTF_8)
                    .build();

            return ResponseEntity.ok()
                    .contentType(mediaType)
                    .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                    .body(new ByteArrayResource(data));
        } catch (IOException e) {
            return ResponseEntity.status(302)
                    .header(HttpHeaders.LOCATION, "/feed/" + itemId + "?error="
                            + URLEncoder.encode("다운로드 처리 중 오류가 발생했습니다.", StandardCharsets.UTF_8))
                    .build();
        }
    }
}
