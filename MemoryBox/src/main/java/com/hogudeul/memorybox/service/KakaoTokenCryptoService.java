package com.hogudeul.memorybox.service;

import org.springframework.stereotype.Component;

@Component
public class KakaoTokenCryptoService {

    public String encrypt(String plainTextToken) {
        return plainTextToken;
    }

    public String decrypt(String encryptedToken) {
        return encryptedToken;
    }
}
