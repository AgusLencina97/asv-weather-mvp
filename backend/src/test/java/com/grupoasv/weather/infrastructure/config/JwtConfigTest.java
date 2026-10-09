package com.grupoasv.weather.infrastructure.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtConfigTest {

    private final JwtConfig jwtConfig = new JwtConfig();

    @Test
    @DisplayName("Secret key - should fail fast when the secret is shorter than 32 bytes")
    void jwtSecretKey_ShouldFailWhenSecretIsTooShort() {
        JwtProperties properties = new JwtProperties("demasiado-corto", Duration.ofHours(1), "issuer");

        assertThrows(IllegalStateException.class, () -> jwtConfig.jwtSecretKey(properties));
    }

    @Test
    @DisplayName("Secret key - should accept a secret of 32 bytes")
    void jwtSecretKey_ShouldAcceptSecretOf32Bytes() {
        JwtProperties properties = new JwtProperties("a".repeat(JwtConfig.MIN_SECRET_BYTES), Duration.ofHours(1), "issuer");

        assertDoesNotThrow(() -> jwtConfig.jwtSecretKey(properties));
    }
}
