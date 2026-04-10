package com.hogudeul.memorybox.auth;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class PasswordEncoderTest {

    @Test
    void bcryptMatchWorks() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String encoded = encoder.encode("Passw0rd!23");

        assertTrue(encoder.matches("Passw0rd!23", encoded));
    }
}
