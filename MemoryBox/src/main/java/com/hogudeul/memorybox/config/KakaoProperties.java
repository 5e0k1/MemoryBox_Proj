package com.hogudeul.memorybox.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "kakao")
public class KakaoProperties {

    private String clientId;
    private String redirectUri;
    private String loginRedirectUri;
    private String javascriptKey;

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getRedirectUri() {
        return redirectUri;
    }

    public void setRedirectUri(String redirectUri) {
        this.redirectUri = redirectUri;
    }

    public String getJavascriptKey() {
        return javascriptKey;
    }

    public void setJavascriptKey(String javascriptKey) {
        this.javascriptKey = javascriptKey;
    }

    public String getLoginRedirectUri() {
        return loginRedirectUri;
    }

    public void setLoginRedirectUri(String loginRedirectUri) {
        this.loginRedirectUri = loginRedirectUri;
    }
}
