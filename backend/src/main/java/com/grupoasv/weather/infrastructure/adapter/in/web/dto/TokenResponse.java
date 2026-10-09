package com.grupoasv.weather.infrastructure.adapter.in.web.dto;

import com.grupoasv.weather.infrastructure.security.JwtTokenService.IssuedToken;

// "expiresIn" en segundos, como en OAuth 2.0
public record TokenResponse(String accessToken, String tokenType, long expiresIn) {

    public static TokenResponse from(IssuedToken token) {
        return new TokenResponse(token.value(), "Bearer", token.expiresIn().toSeconds());
    }
}
