package com.grupoasv.weather.domain.exception;

public class WeatherDomainException extends RuntimeException {

    public WeatherDomainException(String message) {
        super(message);
    }

    public WeatherDomainException(String message, Throwable cause) {
        super(message, cause);
    }

}