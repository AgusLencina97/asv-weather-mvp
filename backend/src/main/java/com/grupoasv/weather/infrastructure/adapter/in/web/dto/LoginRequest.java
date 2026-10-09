package com.grupoasv.weather.infrastructure.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank @Size(max = 100) String username,
        @NotBlank @Size(max = 100) String password
) {
    // Evita que la contraseña aparezca si el objeto llega a imprimirse en un log
    @Override
    public String toString() {
        return "LoginRequest[username=" + username + ", password=***]";
    }
}
