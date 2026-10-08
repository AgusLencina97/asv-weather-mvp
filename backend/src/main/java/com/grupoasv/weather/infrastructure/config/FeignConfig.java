package com.grupoasv.weather.infrastructure.config;

import com.grupoasv.weather.infrastructure.adapter.out.aemet.AemetFeignClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

/**
 * Fuera de la clase principal para que los tests de la capa web (@WebMvcTest) no levanten los clientes HTTP.
 */
@Configuration
@EnableFeignClients(clients = AemetFeignClient.class)
public class FeignConfig {
}
