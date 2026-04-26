package com.hogudeul.memorybox.service;

import com.hogudeul.memorybox.mapper.WebPushSubscriptionMapper;
import com.hogudeul.memorybox.model.WebPushSubscription;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WebPushSubscriptionService {

    private final WebPushSubscriptionMapper webPushSubscriptionMapper;

    public WebPushSubscriptionService(WebPushSubscriptionMapper webPushSubscriptionMapper) {
        this.webPushSubscriptionMapper = webPushSubscriptionMapper;
    }

    @Transactional
    public void upsert(WebPushSubscription subscription) {
        WebPushSubscription existing = webPushSubscriptionMapper.findByEndpoint(subscription.getEndpoint());
        if (existing == null) {
            webPushSubscriptionMapper.insert(subscription);
            return;
        }
        webPushSubscriptionMapper.update(subscription);
    }

    public List<WebPushSubscription> findActiveByUserId(Long userId) {
        return webPushSubscriptionMapper.findByUserId(userId);
    }
}
