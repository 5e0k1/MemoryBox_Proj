package com.hogudeul.memorybox.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hogudeul.memorybox.config.WebPushProperties;
import com.hogudeul.memorybox.model.WebPushSubscription;
import java.util.Map;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import nl.martijndwars.webpush.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class WebPushService {

    private static final Logger log = LoggerFactory.getLogger(WebPushService.class);

    private final WebPushProperties webPushProperties;
    private final ObjectMapper objectMapper;

    public WebPushService(WebPushProperties webPushProperties, ObjectMapper objectMapper) {
        this.webPushProperties = webPushProperties;
        this.objectMapper = objectMapper;
    }

    public boolean sendTestPush(WebPushSubscription subscription) {
        try {
            String payload = objectMapper.writeValueAsString(Map.of(
                    "title", "MemoryBox 알림 테스트",
                    "body", "웹 푸시 알림이 정상적으로 연결되었습니다.",
                    "url", "/mypage"
            ));

            PushService pushService = new PushService();
            pushService.setSubject(webPushProperties.getSubject());
            pushService.setPublicKey(Utils.loadPublicKey(webPushProperties.getPublicKey()));
            pushService.setPrivateKey(Utils.loadPrivateKey(webPushProperties.getPrivateKey()));

            Notification notification = new Notification(
                    subscription.getEndpoint(),
                    subscription.getP256dh(),
                    subscription.getAuth(),
                    payload
            );
            pushService.send(notification);
            return true;
        } catch (Exception e) {
            log.warn("Web push send failed. subscriptionId={}, endpointPrefix={}",
                    subscription.getSubscriptionId(),
                    maskEndpoint(subscription.getEndpoint()),
                    e);
            return false;
        }
    }

    private String maskEndpoint(String endpoint) {
        if (endpoint == null || endpoint.isBlank()) {
            return "(empty)";
        }
        int visible = Math.min(endpoint.length(), 24);
        return endpoint.substring(0, visible) + "...";
    }
}
