package com.grupoasv.weather.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties("api.security.jwt")
public record JwtProperties(String secret, Duration expiration, String issuer) {}
