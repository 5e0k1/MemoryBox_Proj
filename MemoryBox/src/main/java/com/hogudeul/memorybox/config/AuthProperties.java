package com.hogudeul.memorybox.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.auth")
public class AuthProperties {

    private int rememberDays = 30;
    private String rememberCookieName = "remember_token";
    private boolean rememberSecure = false;
    private int lastAccessUpdateMinutes = 10;

    public int getRememberDays() {
        return rememberDays;
    }

    public void setRememberDays(int rememberDays) {
        this.rememberDays = rememberDays;
    }

    public String getRememberCookieName() {
        return rememberCookieName;
    }

    public void setRememberCookieName(String rememberCookieName) {
        this.rememberCookieName = rememberCookieName;
    }

    public boolean isRememberSecure() {
        return rememberSecure;
    }

    public void setRememberSecure(boolean rememberSecure) {
        this.rememberSecure = rememberSecure;
    }

    public int getLastAccessUpdateMinutes() {
        return lastAccessUpdateMinutes;
    }

    public void setLastAccessUpdateMinutes(int lastAccessUpdateMinutes) {
        this.lastAccessUpdateMinutes = lastAccessUpdateMinutes;
    }
}
