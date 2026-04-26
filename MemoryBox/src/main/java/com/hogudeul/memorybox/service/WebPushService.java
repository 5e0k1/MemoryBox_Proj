package com.hogudeul.memorybox.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hogudeul.memorybox.config.WebPushProperties;
import com.hogudeul.memorybox.model.WebPushSubscription;
import java.security.Security;
import java.util.Map;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import nl.martijndwars.webpush.Utils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class WebPushService {

    private static final Logger log = LoggerFactory.getLogger(WebPushService.class);

    private final WebPushProperties webPushProperties;
    private final ObjectMapper objectMapper;
    private final WebPushSubscriptionService webPushSubscriptionService;

    public WebPushService(WebPushProperties webPushProperties,
                          ObjectMapper objectMapper,
                          WebPushSubscriptionService webPushSubscriptionService) {
        this.webPushProperties = webPushProperties;
        this.objectMapper = objectMapper;
        this.webPushSubscriptionService = webPushSubscriptionService;
        ensureBouncyCastleProvider();
    }

    private void ensureBouncyCastleProvider() {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    public boolean sendTestPush(WebPushSubscription subscription) {
        return sendPush(subscription, "MemoryBox 알림 테스트", "웹 푸시 알림이 정상적으로 연결되었습니다.", "/mypage");
    }

    public boolean sendPush(WebPushSubscription subscription, String title, String body, String url) {
        if (subscription == null || hasBlank(subscription.getEndpoint())
                || hasBlank(subscription.getP256dh()) || hasBlank(subscription.getAuth())) {
            Long subscriptionId = subscription == null ? null : subscription.getSubscriptionId();
            log.warn("Skip invalid web push subscription. subscriptionId={}", subscriptionId);
            deactivateIfPossible(subscriptionId);
            return false;
        }

        try {
            String payload = objectMapper.writeValueAsString(Map.of(
                    "title", title,
                    "body", body,
                    "url", url
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
            log.warn("Web push send failed. subscriptionId={}", subscription.getSubscriptionId(), e);
            if (isSubscriptionKeyError(e)) {
                deactivateIfPossible(subscription.getSubscriptionId());
            }
            return false;
        }
    }

    private void deactivateIfPossible(Long subscriptionId) {
        if (subscriptionId == null) {
            return;
        }
        webPushSubscriptionService.deactivateBySubscriptionId(subscriptionId);
    }

    private boolean hasBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private boolean isSubscriptionKeyError(Exception e) {
        if (e instanceof ArrayIndexOutOfBoundsException || e instanceof IllegalArgumentException) {
            return true;
        }
        String message = e.getMessage();
        return message != null && (
                message.contains("p256dh")
                        || message.contains("public key")
                        || message.contains("Invalid point")
        );
    }
}
