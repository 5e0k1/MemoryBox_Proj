package com.hogudeul.memorybox.config;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private final Feed feed = new Feed();
    private final Ffmpeg ffmpeg = new Ffmpeg();
    private final Video video = new Video();
    private final Calendar calendar = new Calendar();

    public Feed getFeed() {
        return feed;
    }

    public Ffmpeg getFfmpeg() {
        return ffmpeg;
    }

    public Video getVideo() {
        return video;
    }

    public Calendar getCalendar() {
        return calendar;
    }

    public static class Feed {
        private long newThresholdHours = 24;

        public long getNewThresholdHours() {
            return newThresholdHours;
        }

        public void setNewThresholdHours(long newThresholdHours) {
            this.newThresholdHours = newThresholdHours;
        }
    }

    public static class Ffmpeg {
        private String command = "ffmpeg";

        public String getCommand() {
            return command;
        }

        public void setCommand(String command) {
            this.command = command;
        }
    }

    public static class Video {
        private long maxFileSizeBytes = 314572800L;

        public long getMaxFileSizeBytes() {
            return maxFileSizeBytes;
        }

        public void setMaxFileSizeBytes(long maxFileSizeBytes) {
            this.maxFileSizeBytes = maxFileSizeBytes;
        }
    }

    public static class Calendar {
        private boolean enabled = false;
        private List<CalendarSource> sources = new ArrayList<>();

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public List<CalendarSource> getSources() {
            return sources;
        }

        public void setSources(List<CalendarSource> sources) {
            this.sources = sources;
        }
    }

    public static class CalendarSource {
        private String name;
        private String url;
        private String type = "PERSONAL";

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }
    }
}
