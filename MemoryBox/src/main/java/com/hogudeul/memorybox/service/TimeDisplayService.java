package com.hogudeul.memorybox.service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class TimeDisplayService {

    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final long newThresholdHours;

    public TimeDisplayService(@Value("${app.feed.new-threshold-hours:24}") long newThresholdHours) {
        this.newThresholdHours = Math.max(1, newThresholdHours);
    }

    public String formatRelativeUploadedAt(LocalDateTime uploadedAt) {
        if (uploadedAt == null) {
            return "";
        }
        LocalDateTime now = LocalDateTime.now();
        Duration duration = Duration.between(uploadedAt, now);
        if (duration.isNegative()) {
            return "방금 전";
        }

        long minutes = duration.toMinutes();
        if (minutes < 1) {
            return "방금 전";
        }
        if (minutes < 60) {
            return minutes + "분 전";
        }

        long hours = duration.toHours();
        if (hours < 24) {
            return hours + "시간 전";
        }

        long days = duration.toDays();
        if (days == 1) {
            return "하루 전";
        }
        if (days == 2) {
            return "이틀 전";
        }
        if (days < 7) {
            return days + "일 전";
        }

        return uploadedAt.format(DATE_TIME_FORMAT);
    }

    public String formatTakenDate(LocalDateTime takenAt) {
        if (takenAt == null) {
            return "";
        }
        LocalDate date = takenAt.toLocalDate();
        return date.format(DATE_FORMAT);
    }

    public boolean isNew(LocalDateTime uploadedAt) {
        if (uploadedAt == null) {
            return false;
        }
        LocalDateTime threshold = LocalDateTime.now().minusHours(newThresholdHours);
        return !uploadedAt.isBefore(threshold);
    }

    public long getNewThresholdHours() {
        return newThresholdHours;
    }
}
