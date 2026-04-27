package com.hogudeul.memorybox.controller;

import com.hogudeul.memorybox.auth.LoginUserSession;
import com.hogudeul.memorybox.dto.CommentView;
import com.hogudeul.memorybox.dto.MediaDetailView;
import com.hogudeul.memorybox.dto.ShareLinkCreateRequest;
import com.hogudeul.memorybox.dto.ShareLinkCreateResponse;
import com.hogudeul.memorybox.model.ShareLink;
import com.hogudeul.memorybox.service.DetailService;
import com.hogudeul.memorybox.service.ShareLinkService;
import jakarta.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class ShareController {

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

        model.addAttribute("detail", detail);
        model.addAttribute("comments", comments);
        model.addAttribute("allowComments", showComments);
        model.addAttribute("allowDownload", allowDownload);
        return "shareDetail";
    }
}
