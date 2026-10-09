package com.grupoasv.weather.infrastructure.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
@EnableCaching
public class CacheConfig {

    public static final String MUNICIPIOS = "municipios";
    public static final String PREDICCIONES = "predicciones";

    @Bean
    public CacheManager cacheManager(
            @Value("${weather.cache.municipios-ttl}") Duration municipiosTtl,
            @Value("${weather.cache.predicciones-ttl}") Duration prediccionesTtl,
            @Value("${weather.cache.predicciones-max-size}") long prediccionesMaxSize) {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.registerCustomCache(MUNICIPIOS, Caffeine.newBuilder()
                .expireAfterWrite(municipiosTtl)
                .build());
        cacheManager.registerCustomCache(PREDICCIONES, Caffeine.newBuilder()
                .expireAfterWrite(prediccionesTtl)
                .maximumSize(prediccionesMaxSize)
                .build());
        return cacheManager;
    }
}
