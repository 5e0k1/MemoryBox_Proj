package com.hogudeul.memorybox.dto;

public class WebPushSubscriptionRequest {

    private String endpoint;
    private SubscriptionKeys keys;
    private String p256dh;
    private String auth;

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public SubscriptionKeys getKeys() {
        return keys;
    }

    public void setKeys(SubscriptionKeys keys) {
        this.keys = keys;
    }

    public String getP256dh() {
        return p256dh;
    }

    public void setP256dh(String p256dh) {
        this.p256dh = p256dh;
    }

    public String getAuth() {
        return auth;
    }

    public void setAuth(String auth) {
        this.auth = auth;
    }

    public String resolveP256dh() {
        if (keys != null && keys.getP256dh() != null) {
            return keys.getP256dh();
        }
        return p256dh;
    }

    public String resolveAuth() {
        if (keys != null && keys.getAuth() != null) {
            return keys.getAuth();
        }
        return auth;
    }

    public static class SubscriptionKeys {
        private String p256dh;
        private String auth;

        public String getP256dh() {
            return p256dh;
        }

        public void setP256dh(String p256dh) {
            this.p256dh = p256dh;
        }

        public String getAuth() {
            return auth;
        }

        public void setAuth(String auth) {
            this.auth = auth;
        }
    }
}
