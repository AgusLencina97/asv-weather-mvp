package com.grupoasv.weather.infrastructure.adapter.in.web;

import com.grupoasv.weather.domain.exception.ResourceNotFoundException;
import com.grupoasv.weather.domain.exception.WeatherDomainException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Traduce excepciones a respuestas HTTP con formato estándar RFC 9457 (ProblemDetail).
 * La clase base ya resuelve los errores de petición de Spring MVC (parámetros que faltan,
 * tipos inválidos, validaciones) como 400.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleNotFound(ResourceNotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, "Recurso no encontrado", ex.getMessage());
    }

    @ExceptionHandler(AuthenticationException.class)
    public ProblemDetail handleAuthentication(AuthenticationException ex) {
        // Mismo mensaje para usuario inexistente y contraseña incorrecta: no revela qué usuarios existen
        log.warn("Intento de login fallido: {}", ex.getMessage());
        return problem(HttpStatus.UNAUTHORIZED, "No autenticado", "Usuario o contraseña incorrectos");
    }

    @ExceptionHandler(WeatherDomainException.class)
    public ProblemDetail handleExternalServiceError(WeatherDomainException ex) {
        // No se expone ex.getMessage(): el detalle técnico ya está en el log
        return problem(HttpStatus.BAD_GATEWAY, "Error en el servicio de AEMET",
                "No se pudo obtener la información de AEMET. Inténtalo de nuevo en unos minutos.");
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex) {
        log.error("Error no controlado", ex);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno", "Error interno del servidor");
    }

    private static ProblemDetail problem(HttpStatus status, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        return problem;
    }
}
