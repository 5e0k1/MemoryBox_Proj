package com.hogudeul.memorybox.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class NotificationPushScheduler {

    private static final Logger log = LoggerFactory.getLogger(NotificationPushScheduler.class);

    private final NotificationService notificationService;

    public NotificationPushScheduler(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Scheduled(fixedDelay = 600000)
    public void sendUploadBatchPush() {
        int sentUsers = notificationService.sendPendingUploadPushBatch();
        if (sentUsers > 0) {
            log.info("Upload push scheduler executed. sentUsers={}", sentUsers);
        }
    }
}
