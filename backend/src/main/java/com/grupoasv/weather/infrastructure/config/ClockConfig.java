package com.grupoasv.weather.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class ClockConfig {

    // "Mañana" se calcula en la zona horaria de AEMET (España), no en la del servidor
    @Bean
    public Clock clock(@Value("${weather.time-zone}") ZoneId zoneId) {
        return Clock.system(zoneId);
    }
}
