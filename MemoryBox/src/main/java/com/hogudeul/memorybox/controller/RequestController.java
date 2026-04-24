package com.hogudeul.memorybox.controller;

import com.hogudeul.memorybox.auth.LoginUserSession;
import com.hogudeul.memorybox.service.RequestService;
import jakarta.servlet.http.HttpSession;
import java.util.Map;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class RequestController {

    private final RequestService requestService;

    public RequestController(RequestService requestService) {
        this.requestService = requestService;
    }

    @GetMapping("/requests")
    public String requestList(Model model, HttpSession session) {
        model.addAttribute("loginUser", session.getAttribute("loginUser"));
        model.addAttribute("mode", "requests");
        model.addAttribute("pageTitle", "요청");
        model.addAttribute("requestPosts", requestService.getRequestPosts());
        return "request/list";
    }

    @GetMapping("/requests/new")
    public String requestWrite(Model model, HttpSession session) {
        model.addAttribute("loginUser", session.getAttribute("loginUser"));
        model.addAttribute("mode", "requests");
        return "request/write";
    }

    @PostMapping("/requests")
    public String createRequest(@RequestParam("title") String title,
                                @RequestParam("content") String content,
                                HttpSession session,
                                RedirectAttributes redirectAttributes) {
        LoginUserSession loginUser = (LoginUserSession) session.getAttribute("loginUser");
        if (loginUser == null) {
            return "redirect:/login";
        }
        boolean ok = requestService.createRequestPost(loginUser.getUserId(), title, content);
        if (!ok) {
            redirectAttributes.addFlashAttribute("error", "제목/내용을 입력해 주세요.");
            return "redirect:/requests/new";
        }
        return "redirect:/requests";
    }

    @GetMapping("/requests/{requestId}")
    public String requestDetail(@PathVariable Long requestId,
                                Model model,
                                HttpSession session) {
        Map<String, Object> requestPost = requestService.getRequestPost(requestId);
        model.addAttribute("loginUser", session.getAttribute("loginUser"));
        model.addAttribute("mode", "requests");
        model.addAttribute("requestPost", requestPost);
        model.addAttribute("notFound", requestPost == null);
        return "request/detail";
    }

    @PostMapping("/requests/{requestId}/comments")
    public String addRequestComment(@PathVariable Long requestId,
                                    @RequestParam("content") String content,
                                    HttpSession session,
                                    RedirectAttributes redirectAttributes) {
        LoginUserSession loginUser = (LoginUserSession) session.getAttribute("loginUser");
        if (loginUser == null) {
            return "redirect:/login";
        }
        boolean ok = requestService.createRequestComment(requestId, loginUser.getUserId(), content);
        if (!ok) {
            redirectAttributes.addFlashAttribute("error", "댓글 내용을 입력해 주세요.");
        }
        return "redirect:/requests/" + requestId;
    }
}
