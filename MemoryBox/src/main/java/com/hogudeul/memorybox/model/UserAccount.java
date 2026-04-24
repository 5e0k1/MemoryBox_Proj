package com.hogudeul.memorybox.model;

import java.time.LocalDateTime;

public class UserAccount {

    private Long userId;
    private String loginId;
    private String passwordHash;
    private String displayName;
    private String role;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime lastLoginAt;
    private String rememberTokenHash;
    private LocalDateTime rememberTokenExpiresAt;
    private LocalDateTime lastAccessAt;
    private String delYn;
    private LocalDateTime delAt;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getLoginId() {
        return loginId;
    }

    public void setLoginId(String loginId) {
        this.loginId = loginId;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public LocalDateTime getLastLoginAt() {
        return lastLoginAt;
    }

    public void setLastLoginAt(LocalDateTime lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }

    public String getRememberTokenHash() {
        return rememberTokenHash;
    }

    public void setRememberTokenHash(String rememberTokenHash) {
        this.rememberTokenHash = rememberTokenHash;
    }

    public LocalDateTime getRememberTokenExpiresAt() {
        return rememberTokenExpiresAt;
    }

    public void setRememberTokenExpiresAt(LocalDateTime rememberTokenExpiresAt) {
        this.rememberTokenExpiresAt = rememberTokenExpiresAt;
    }

    public LocalDateTime getLastAccessAt() {
        return lastAccessAt;
    }

    public void setLastAccessAt(LocalDateTime lastAccessAt) {
        this.lastAccessAt = lastAccessAt;
    }

    public String getDelYn() {
        return delYn;
    }

    public void setDelYn(String delYn) {
        this.delYn = delYn;
    }

    public LocalDateTime getDelAt() {
        return delAt;
    }

    public void setDelAt(LocalDateTime delAt) {
        this.delAt = delAt;
    }
}
