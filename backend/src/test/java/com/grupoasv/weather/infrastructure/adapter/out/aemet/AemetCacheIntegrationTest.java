package com.grupoasv.weather.infrastructure.adapter.out.aemet;

import com.grupoasv.weather.domain.model.Municipio;
import com.grupoasv.weather.domain.port.out.WeatherExternalPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Comprueba la caché real (Caffeine + proxy de Spring) delante del adaptador: el objetivo es
 * llamar a AEMET lo menos posible para no superar su límite de uso.
 */
@SpringBootTest
@TestPropertySource(locations = "classpath:test-secrets.properties")
class AemetCacheIntegrationTest {

    private static final String DATOS_URL = "https://opendata.aemet.es/opendata/sh/mock";
    private static final String MUNICIPIOS_JSON = """
            [{"id": "id03002", "nombre": "Agost"}]
            """;

    @Autowired
    private WeatherExternalPort weatherExternalPort;

    @Autowired
    private CacheManager cacheManager;

    @MockitoBean
    private AemetFeignClient feignClient;

    @BeforeEach
    void setUp() {
        cacheManager.getCacheNames().forEach(name -> Objects.requireNonNull(cacheManager.getCache(name)).clear());
        when(feignClient.getDataFromUrl(any(URI.class))).thenReturn(MUNICIPIOS_JSON);
    }

    @Test
    @DisplayName("Municipalities - should call AEMET only once for consecutive requests")
    void fetchAllMunicipalities_ShouldCallAemetOnceForConsecutiveRequests() {
        when(feignClient.getMunicipiosUrl(anyString())).thenReturn(new AemetResponseWrapper("exito", 200, DATOS_URL, null));

        weatherExternalPort.fetchAllMunicipalities();
        weatherExternalPort.fetchAllMunicipalities();

        verify(feignClient, times(1)).getMunicipiosUrl(anyString());
    }

    @Test
    @DisplayName("Municipalities - should call AEMET only once for concurrent requests (sync = true)")
    void fetchAllMunicipalities_ShouldCallAemetOnceForConcurrentRequests() throws Exception {
        // AEMET tarda en responder: mientras tanto llegan otras 4 peticiones con la caché todavía vacía
        when(feignClient.getMunicipiosUrl(anyString())).thenAnswer(invocation -> {
            Thread.sleep(300);
            return new AemetResponseWrapper("exito", 200, DATOS_URL, null);
        });
        Callable<List<Municipio>> request = weatherExternalPort::fetchAllMunicipalities;

        List<Future<List<Municipio>>> results;
        try (ExecutorService pool = Executors.newFixedThreadPool(5)) {
            results = pool.invokeAll(List.of(request, request, request, request, request));
        }

        for (Future<List<Municipio>> result : results) {
            assertEquals(List.of(new Municipio("03002", "Agost")), result.get());
        }
        verify(feignClient, times(1)).getMunicipiosUrl(anyString());
    }
}
