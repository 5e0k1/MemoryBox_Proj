package com.hogudeul.memorybox.config;

import java.util.Base64;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "webpush.vapid")
public class WebPushProperties {

    private String publicKey;
    private String privateKey;
    private String subject = "mailto:test@example.com";

    public String getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = normalizePublicKey(publicKey);
    }

    public String getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(String privateKey) {
        this.privateKey = normalizePrivateKey(privateKey);
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public boolean hasValidClientPublicKey() {
        return isValidBase64Url(publicKey);
    }

    public boolean hasValidServerPrivateKey() {
        return isValidBase64Url(privateKey);
    }

    private String normalizePublicKey(String raw) {
        if (raw == null) {
            return null;
        }
        return raw.replaceAll("\\s", "");
    }

    private String normalizePrivateKey(String raw) {
        if (raw == null) {
            return null;
        }
        return raw.replaceAll("\\s", "");
    }

    private boolean isValidBase64Url(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        try {
            byte[] decoded = Base64.getUrlDecoder().decode(value);
            return decoded.length > 0;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
