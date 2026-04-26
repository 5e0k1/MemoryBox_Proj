package com.hogudeul.memorybox.controller;

import com.hogudeul.memorybox.auth.LoginUserSession;
import com.hogudeul.memorybox.config.WebPushProperties;
import com.hogudeul.memorybox.dto.WebPushSubscriptionRequest;
import com.hogudeul.memorybox.model.WebPushSubscription;
import com.hogudeul.memorybox.service.WebPushService;
import com.hogudeul.memorybox.service.WebPushSubscriptionService;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class PushController {

    private final WebPushSubscriptionService webPushSubscriptionService;
    private final WebPushService webPushService;
    private final WebPushProperties webPushProperties;

    public PushController(WebPushSubscriptionService webPushSubscriptionService,
                          WebPushService webPushService,
                          WebPushProperties webPushProperties) {
        this.webPushSubscriptionService = webPushSubscriptionService;
        this.webPushService = webPushService;
        this.webPushProperties = webPushProperties;
    }

    @GetMapping("/api/push/public-key")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> publicKey(HttpSession session) {
        LoginUserSession loginUser = (LoginUserSession) session.getAttribute("loginUser");
        if (loginUser == null) {
            return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
        }
        if (!webPushProperties.hasValidClientPublicKey()) {
            return ResponseEntity.status(500).body(Map.of("message", "VAPID 공개키가 비어 있거나 형식이 올바르지 않습니다."));
        }
        return ResponseEntity.ok(Map.of("publicKey", webPushProperties.getPublicKey()));
    }

    @GetMapping("/api/push/status")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> pushStatus(HttpSession session) {
        LoginUserSession loginUser = (LoginUserSession) session.getAttribute("loginUser");
        if (loginUser == null) {
            return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
        }
        boolean enabled = webPushSubscriptionService.hasActiveSubscription(loginUser.getUserId());
        return ResponseEntity.ok(Map.of("enabled", enabled));
    }

    @PostMapping("/push/subscribe")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> subscribe(@RequestBody WebPushSubscriptionRequest request,
                                                          HttpSession session) {
        LoginUserSession loginUser = (LoginUserSession) session.getAttribute("loginUser");
        if (loginUser == null) {
            return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
        }
        if (request == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "구독 정보가 올바르지 않습니다."));
        }

        String endpoint = trimToNull(request.getEndpoint());
        String p256dh = trimToNull(request.resolveP256dh());
        String auth = trimToNull(request.resolveAuth());

        if (endpoint == null || p256dh == null || auth == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "endpoint, p256dh, auth는 필수입니다."));
        }

        webPushSubscriptionService.deactivateInvalidActiveByUserId(loginUser.getUserId());

        WebPushSubscription subscription = new WebPushSubscription();
        subscription.setUserId(loginUser.getUserId());
        subscription.setEndpoint(endpoint);
        subscription.setP256dh(p256dh);
        subscription.setAuth(auth);
        subscription.setActiveYn("Y");
        webPushSubscriptionService.upsert(subscription);

        return ResponseEntity.ok(Map.of("success", true));
    }


    @PostMapping("/push/unsubscribe")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> unsubscribe(@RequestBody(required = false) Map<String, String> request,
                                                            HttpSession session) {
        LoginUserSession loginUser = (LoginUserSession) session.getAttribute("loginUser");
        if (loginUser == null) {
            return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
        }

        String endpoint = request != null ? trimToNull(request.get("endpoint")) : null;
        if (endpoint == null) {
            webPushSubscriptionService.deactivateAllByUserId(loginUser.getUserId());
            return ResponseEntity.ok(Map.of("success", true));
        }

        webPushSubscriptionService.deactivateByEndpoint(loginUser.getUserId(), endpoint);
        return ResponseEntity.ok(Map.of("success", true));
    }

    @PostMapping("/push/test")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> testPush(HttpSession session) {
        LoginUserSession loginUser = (LoginUserSession) session.getAttribute("loginUser");
        if (loginUser == null) {
            return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
        }
        if (!"ADMIN".equalsIgnoreCase(loginUser.getRole())) {
            return ResponseEntity.status(403).body(Map.of("message", "관리자만 사용할 수 있습니다."));
        }

        List<WebPushSubscription> subscriptions = webPushSubscriptionService.findActiveByUserId(loginUser.getUserId());
        int successCount = 0;
        for (WebPushSubscription subscription : subscriptions) {
            if (webPushService.sendTestPush(subscription)) {
                successCount++;
            }
        }

        return ResponseEntity.ok(Map.of(
                "success", true,
                "total", subscriptions.size(),
                "sent", successCount
        ));
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
