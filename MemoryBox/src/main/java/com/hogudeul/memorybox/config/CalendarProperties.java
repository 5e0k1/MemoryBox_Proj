package com.hogudeul.memorybox.config;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.calendar")
public class CalendarProperties {

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
