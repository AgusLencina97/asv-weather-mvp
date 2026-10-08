package com.grupoasv.weather.domain.exception;

public class ResourceNotFoundException extends WeatherDomainException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}