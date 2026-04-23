package com.hogudeul.memorybox.controller;

import com.hogudeul.memorybox.auth.LoginUserSession;
import com.hogudeul.memorybox.service.NotificationService;
import jakarta.servlet.http.HttpSession;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping("/api/notifications")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> notifications(HttpSession session) {
        LoginUserSession loginUser = (LoginUserSession) session.getAttribute("loginUser");
        if (loginUser == null) {
            return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
        }
        return ResponseEntity.ok(notificationService.getNotificationPanel(loginUser.getUserId()));
    }

    @GetMapping("/notifications/{notificationId}/open")
    public String openNotification(@PathVariable Long notificationId, HttpSession session) {
        LoginUserSession loginUser = (LoginUserSession) session.getAttribute("loginUser");
        if (loginUser == null) {
            return "redirect:/login";
        }
        String redirectUrl = notificationService.openNotification(loginUser.getUserId(), notificationId);
        return "redirect:" + redirectUrl;
    }

    @PostMapping("/api/notifications/{notificationId}/read")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> markRead(@PathVariable Long notificationId, HttpSession session) {
        LoginUserSession loginUser = (LoginUserSession) session.getAttribute("loginUser");
        if (loginUser == null) {
            return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
        }
        boolean ok = notificationService.markRead(loginUser.getUserId(), notificationId);
        if (!ok) {
            return ResponseEntity.badRequest().body(Map.of("message", "알림 읽음 처리에 실패했습니다."));
        }
        return ResponseEntity.ok(Map.of("success", true));
    }

    @PostMapping("/api/notifications/{notificationId}/delete")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> deleteNotification(@PathVariable Long notificationId, HttpSession session) {
        LoginUserSession loginUser = (LoginUserSession) session.getAttribute("loginUser");
        if (loginUser == null) {
            return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
        }
        boolean ok = notificationService.deleteNotification(loginUser.getUserId(), notificationId);
        if (!ok) {
            return ResponseEntity.badRequest().body(Map.of("message", "알림 삭제에 실패했습니다."));
        }
        return ResponseEntity.ok(Map.of("success", true));
    }
}
