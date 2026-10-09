package com.grupoasv.weather.infrastructure.adapter.in.web;

import com.grupoasv.weather.infrastructure.adapter.in.web.dto.LoginRequest;
import com.grupoasv.weather.infrastructure.adapter.in.web.dto.TokenResponse;
import com.grupoasv.weather.infrastructure.security.JwtTokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticación", description = "Obtención del token de acceso")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenService jwtTokenService;

    @PostMapping("/login")
    @SecurityRequirements // Endpoint público: no requiere token
    @Operation(summary = "Valida las credenciales y devuelve un JWT de acceso")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        // Si las credenciales no son válidas lanza AuthenticationException -> 401 (GlobalExceptionHandler)
        Authentication authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(request.username(), request.password()));
        log.info("Login correcto para el usuario '{}'", authentication.getName());
        return TokenResponse.from(jwtTokenService.issue(authentication));
    }
}
