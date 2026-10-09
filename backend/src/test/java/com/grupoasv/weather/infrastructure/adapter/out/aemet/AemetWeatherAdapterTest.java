package com.grupoasv.weather.infrastructure.adapter.out.aemet;

import com.grupoasv.weather.domain.exception.ResourceNotFoundException;
import com.grupoasv.weather.domain.exception.WeatherDomainException;
import com.grupoasv.weather.domain.model.DailyForecast;
import com.grupoasv.weather.domain.model.Municipio;
import com.grupoasv.weather.domain.model.PrecipitationProbability;
import feign.FeignException;
import feign.Request;
import feign.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AemetWeatherAdapterTest {

    private static final String DATOS_URL = "https://opendata.aemet.es/opendata/sh/mock";
    private static final AemetResponseWrapper RESPUESTA_OK = new AemetResponseWrapper("exito", 200, DATOS_URL, "ok");

    @Mock
    private AemetFeignClient feignClient;

    private AemetWeatherAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new AemetWeatherAdapter(feignClient, JsonMapper.builder().build());

        ReflectionTestUtils.setField(adapter, "apiKey", "test-key");
    }

    @Test
    @DisplayName("Fetch all municipalities - should parse and clean IDs correctly")
    void fetchAllMunicipalities_ShouldParseAndCleanIdsCorrectly() {
        String rawJsonData = """
                [
                  {"id": "id03002", "nombre": "Agost"},
                  {"id": "id40001", "nombre": "Abades"},
                  {"nombre": "Sin id"}
                ]
                """;

        when(feignClient.getMunicipiosUrl("test-key")).thenReturn(RESPUESTA_OK);
        when(feignClient.getDataFromUrl(URI.create(DATOS_URL))).thenReturn(rawJsonData);

        List<Municipio> result = adapter.fetchAllMunicipalities();

        assertEquals(List.of(new Municipio("03002", "Agost"), new Municipio("40001", "Abades")), result);
    }

    @Test
    @DisplayName("Fetch all municipalities - should throw DomainException on Feign error")
    void fetchAllMunicipalities_ShouldThrowDomainExceptionOnFeignError() {
        when(feignClient.getMunicipiosUrl(anyString())).thenThrow(feignException(429));

        WeatherDomainException exception = assertThrows(WeatherDomainException.class,
                () -> adapter.fetchAllMunicipalities());

        assertTrue(exception.getMessage().contains("Error obteniendo el listado de municipios"));
    }

    @Test
    @DisplayName("Fetch all municipalities - should throw DomainException when AEMET 'estado' is not OK")
    void fetchAllMunicipalities_ShouldThrowDomainExceptionWhenEstadoIsNotOk() {
        when(feignClient.getMunicipiosUrl(anyString()))
                .thenReturn(new AemetResponseWrapper("Límite de peticiones", 429, null, null));

        assertThrows(WeatherDomainException.class, () -> adapter.fetchAllMunicipalities());
        verify(feignClient, never()).getDataFromUrl(any());
    }

    @Test
    @DisplayName("Fetch daily forecast - should pick the requested date and keep only 6-hour periods")
    void fetchDailyForecast_ShouldPickRequestedDateAndKeepOnlySixHourPeriods() throws IOException {
        // El fixture tiene los días 08 (hoy), 09 (mañana) y 10, con la estructura real de AEMET
        when(feignClient.getPrediccionUrl("03002", "test-key")).thenReturn(RESPUESTA_OK);
        when(feignClient.getDataFromUrl(URI.create(DATOS_URL))).thenReturn(fixture("aemet/prediccion-diaria.json"));

        DailyForecast result = adapter.fetchDailyForecast("03002", LocalDate.of(2026, 10, 9));

        assertEquals(24.0, result.temperaturaMaximaCelsius());
        assertEquals(13.0, result.temperaturaMinimaCelsius());
        assertEquals(List.of(
                new PrecipitationProbability(5, "00-06"),
                new PrecipitationProbability(10, "06-12"),
                new PrecipitationProbability(15, "18-24")
        ), result.probPrecipitacion());
    }

    @Test
    @DisplayName("Fetch daily forecast - should fall back to 12-hour periods when 6-hour ones are not available")
    void fetchDailyForecast_ShouldFallBackToTwelveHourPeriods() throws IOException {
        // Caso real: pasada la medianoche en España, "mañana" es el tercer día del fichero y solo trae tramos de 12h
        when(feignClient.getPrediccionUrl("03002", "test-key")).thenReturn(RESPUESTA_OK);
        when(feignClient.getDataFromUrl(URI.create(DATOS_URL))).thenReturn(fixture("aemet/prediccion-diaria.json"));

        DailyForecast result = adapter.fetchDailyForecast("03002", LocalDate.of(2026, 10, 10));

        assertEquals(List.of(
                new PrecipitationProbability(0, "00-12"),
                new PrecipitationProbability(20, "12-24")
        ), result.probPrecipitacion());
    }

    @Test
    @DisplayName("Fetch daily forecast - should throw NotFound when AEMET 'estado' is 404")
    void fetchDailyForecast_ShouldThrowNotFoundWhenMunicipioDoesNotExist() {
        // AEMET responde HTTP 200 pero con "estado": 404 en el cuerpo
        when(feignClient.getPrediccionUrl("99999", "test-key"))
                .thenReturn(new AemetResponseWrapper("Error al obtener los datos", 404, null, null));

        assertThrows(ResourceNotFoundException.class, () -> adapter.fetchDailyForecast("99999", LocalDate.of(2026, 10, 9)));
    }

    @Test
    @DisplayName("Fetch daily forecast - should throw NotFound when the date is not in the forecast")
    void fetchDailyForecast_ShouldThrowNotFoundWhenDateIsMissing() throws IOException {
        when(feignClient.getPrediccionUrl("03002", "test-key")).thenReturn(RESPUESTA_OK);
        when(feignClient.getDataFromUrl(URI.create(DATOS_URL))).thenReturn(fixture("aemet/prediccion-diaria.json"));

        assertThrows(ResourceNotFoundException.class, () -> adapter.fetchDailyForecast("03002", LocalDate.of(2026, 12, 25)));
    }

    @Test
    @DisplayName("Fetch daily forecast - should throw DomainException on malformed JSON")
    void fetchDailyForecast_ShouldThrowDomainExceptionOnMalformedJson() {
        when(feignClient.getPrediccionUrl("03002", "test-key")).thenReturn(RESPUESTA_OK);
        when(feignClient.getDataFromUrl(URI.create(DATOS_URL))).thenReturn("<html>no es JSON</html>");

        assertThrows(WeatherDomainException.class, () -> adapter.fetchDailyForecast("03002", LocalDate.of(2026, 10, 9)));
    }

    private static FeignException feignException(int status) {
        Request request = Request.create(Request.HttpMethod.GET, "https://opendata.aemet.es",
                Map.of(), null, StandardCharsets.UTF_8, null);
        return FeignException.errorStatus("getMunicipiosUrl",
                Response.builder().status(status).reason("error").request(request).headers(Map.of()).build());
    }

    private String fixture(String path) throws IOException {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(path)) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
