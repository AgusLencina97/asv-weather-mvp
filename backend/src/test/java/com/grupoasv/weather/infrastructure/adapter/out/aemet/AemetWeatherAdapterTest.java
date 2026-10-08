package com.grupoasv.weather.infrastructure.adapter.out.aemet;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.grupoasv.weather.domain.exception.WeatherDomainException;
import com.grupoasv.weather.domain.model.Municipio;
import com.grupoasv.weather.domain.model.TemperatureUnit;
import com.grupoasv.weather.domain.model.WeatherPrediction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.net.URI;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AemetWeatherAdapterTest {

    @Mock
    private AemetFeignClient feignClient;

    private AemetWeatherAdapter adapter;

    @BeforeEach
    void setUp() {
        // Usamos un ObjectMapper real para probar el parseo JSON real
        ObjectMapper realMapper = new ObjectMapper();
        adapter = new AemetWeatherAdapter(feignClient, realMapper);

        // Inyectamos la API key simulada (como lo haría @Value en ejecución)
        ReflectionTestUtils.setField(adapter, "apiKey", "test-key");
    }

    @Test
    @DisplayName("Fetch all municipalities - should parse and clean IDs correctly")
    void fetchAllMunicipalities_ShouldParseAndCleanIdsCorrectly() {
        // Arrange: Simulamos la respuesta en dos pasos de AEMET
        String jsonUrl = "https://datos.aemet.es/mock";
        AemetResponseWrapper mockWrapper = new AemetResponseWrapper(200, jsonUrl, "ok");

        String rawJsonData = """
                [
                  {"id": "id03002", "nombre": "Agost"},
                  {"id": "id40001", "nombre": "Abades"}
                ]
                """;

        when(feignClient.getMunicipiosUrl(anyString())).thenReturn(mockWrapper);
        when(feignClient.getDataFromUrl(URI.create(jsonUrl))).thenReturn(rawJsonData);

        // Act
        List<Municipio> result = adapter.fetchAllMunicipalities();

        // Assert
        assertEquals(2, result.size());
        assertEquals("03002", result.get(0).codigo()); // Verificamos que quita el prefijo "id"
        assertEquals("Abades", result.get(1).nombre());
    }

    @Test
    @DisplayName("Fetch next day prediction - should calculate Fahrenheit correctly")
    void fetchNextDayPrediction_ShouldCalculateFahrenheitCorrectly() {
        // Arrange
        String jsonUrl = "https://datos.aemet.es/mock-prediccion";
        AemetResponseWrapper mockWrapper = new AemetResponseWrapper(200, jsonUrl, "ok");

        // JSON simplificado simulando la estructura profunda de AEMET para mañana (día index 1)
        String rawJsonData = """
                [{
                  "prediccion": {
                    "dia": [
                      {},
                      {
                        "probPrecipitacion": [{"value": "10", "periodo": "00-24"}],
                        "temperatura": {"maxima": 20, "minima": 10}
                      }
                    ]
                  }
                }]
                """;

        when(feignClient.getPrediccionUrl(anyString(), anyString())).thenReturn(mockWrapper);
        when(feignClient.getDataFromUrl(URI.create(jsonUrl))).thenReturn(rawJsonData);

        // Act: Pedimos Fahrenheit. Media de (20+10)/2 = 15ºC -> (15 * 9/5) + 32 = 59ºF
        WeatherPrediction result = adapter.fetchNextDayPrediction("40001", TemperatureUnit.G_FAH);

        // Assert
        assertNotNull(result);
        assertEquals(59.0, result.mediaTemperatura());
        assertEquals(TemperatureUnit.G_FAH, result.unidadTemperatura());
        assertEquals(1, result.probPrecipitacion().size());
        assertEquals(10, result.probPrecipitacion().getFirst().probabilidad());
    }

    @Test
    @DisplayName("Fetch all municipalities - should throw DomainException on Feign error")
    void fetchAllMunicipalities_ShouldThrowDomainExceptionOnFeignError() {
        // Arrange: Simulamos una caída de la API
        when(feignClient.getMunicipiosUrl(anyString())).thenThrow(new RuntimeException("API Timeout"));

        // Act & Assert
        WeatherDomainException exception = assertThrows(WeatherDomainException.class, () -> {
            adapter.fetchAllMunicipalities();
        });

        assertTrue(exception.getMessage().contains("Error obteniendo municipios"));
    }
}