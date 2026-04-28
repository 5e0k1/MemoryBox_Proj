package com.hogudeul.memorybox.config;

import com.hogudeul.memorybox.interceptor.LoginCheckInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final LoginCheckInterceptor loginCheckInterceptor;
    private final String localStorageRoot;
    private final String tempStorageRoot;

    public WebMvcConfig(LoginCheckInterceptor loginCheckInterceptor,
                        StorageProperties storageProperties) {
        this.loginCheckInterceptor = loginCheckInterceptor;
        this.localStorageRoot = storageProperties.getLocalRoot();
        this.tempStorageRoot = storageProperties.getTempRoot();
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loginCheckInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/",
                        "/login",
                        "/auth/kakao/**",
                        "/css/**",
                        "/js/**",
                        "/images/**",
                        "/manifest.json",
                        "/sw.js",
                        "/favicon.ico",
                        "/icon-192.png",
                        "/icon-512.png",
                        "/apple-touch-icon.png",
                        "/files/**",
                        "/temp/zip/**",
                        "/share/**",
                        "/error");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String normalizedRoot = localStorageRoot.endsWith("/") || localStorageRoot.endsWith("\\")
                ? localStorageRoot
                : localStorageRoot + "/";
        registry.addResourceHandler("/files/**")
                .addResourceLocations("file:" + normalizedRoot);

        String normalizedTempRoot = tempStorageRoot.endsWith("/") || tempStorageRoot.endsWith("\\")
                ? tempStorageRoot
                : tempStorageRoot + "/";
        registry.addResourceHandler("/temp/zip/**")
                .addResourceLocations("file:" + normalizedTempRoot + "zip/");
    }
}
