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

    @Transactional
    public void deactivateByEndpoint(Long userId, String endpoint) {
        if (userId == null || endpoint == null || endpoint.isBlank()) {
            return;
        }
        webPushSubscriptionMapper.deactivateByEndpointAndUserId(endpoint, userId);
    }

    @Transactional
    public void deactivateAllByUserId(Long userId) {
        if (userId == null) {
            return;
        }
        webPushSubscriptionMapper.deactivateAllByUserId(userId);
    }

    @Transactional
    public void deactivateBySubscriptionId(Long subscriptionId) {
        if (subscriptionId == null) {
            return;
        }
        webPushSubscriptionMapper.deactivateBySubscriptionId(subscriptionId);
    }

    @Transactional
    public void deactivateInvalidActiveByUserId(Long userId) {
        if (userId == null) {
            return;
        }
        webPushSubscriptionMapper.deactivateInvalidActiveByUserId(userId);
    }

    public List<WebPushSubscription> findActiveByUserId(Long userId) {
        return webPushSubscriptionMapper.findByUserId(userId);
    }

    public boolean hasActiveSubscription(Long userId) {
        if (userId == null) {
            return false;
        }
        return webPushSubscriptionMapper.countActiveByUserId(userId) > 0;
    }
}
