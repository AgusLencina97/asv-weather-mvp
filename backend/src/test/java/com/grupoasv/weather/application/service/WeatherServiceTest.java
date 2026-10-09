package com.grupoasv.weather.application.service;

import com.grupoasv.weather.domain.model.DailyForecast;
import com.grupoasv.weather.domain.model.Municipio;
import com.grupoasv.weather.domain.model.PrecipitationProbability;
import com.grupoasv.weather.domain.model.TemperatureUnit;
import com.grupoasv.weather.domain.model.WeatherPrediction;
import com.grupoasv.weather.domain.port.out.WeatherExternalPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WeatherServiceTest {

    private static final ZoneId MADRID = ZoneId.of("Europe/Madrid");
    // 8 de octubre a las 20:00 en España
    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-10-08T18:00:00Z"), MADRID);
    private static final LocalDate TOMORROW = LocalDate.of(2026, 10, 9);

    @Mock
    private WeatherExternalPort weatherExternalPort;

    private WeatherService weatherService;

    @BeforeEach
    void setUp() {
        weatherService = new WeatherService(weatherExternalPort, FIXED_CLOCK);
    }

    @Test
    @DisplayName("Find by name prefix - should return filtered and case insensitive list")
    void findByNamePrefix_ShouldReturnFilteredAndCaseInsensitiveList() {
        // Arrange: Simulamos lo que devolvería AEMET
        when(weatherExternalPort.fetchAllMunicipalities()).thenReturn(List.of(
                new Municipio("03002", "Agost"),
                new Municipio("03003", "Agres"),
                new Municipio("40001", "Abades"),
                new Municipio("35001", "Agaete")
        ));

        // Act: Probamos nuestro caso de uso en minúsculas para verificar el ignore-case
        List<Municipio> result = weatherService.findByNamePrefix("ag");

        // Assert: Verificamos que filtra correctamente y ordena alfabéticamente
        assertEquals(List.of("Agaete", "Agost", "Agres"), result.stream().map(Municipio::nombre).toList());
        verify(weatherExternalPort, times(1)).fetchAllMunicipalities();
    }

    @Test
    @DisplayName("Find by name prefix - should ignore accents")
    void findByNamePrefix_ShouldIgnoreAccents() {
        when(weatherExternalPort.fetchAllMunicipalities()).thenReturn(List.of(
                new Municipio("28005", "Alcalá de Henares"),
                new Municipio("03006", "Alcalalí"),
                new Municipio("03002", "Agost")
        ));

        List<Municipio> result = weatherService.findByNamePrefix("alcala");

        assertEquals(List.of("Alcalá de Henares", "Alcalalí"), result.stream().map(Municipio::nombre).toList());
    }

    @Test
    @DisplayName("Find by name prefix - should match names with trailing article (\"Coruña, A\")")
    void findByNamePrefix_ShouldMatchNamesWithTrailingArticle() {
        when(weatherExternalPort.fetchAllMunicipalities()).thenReturn(List.of(
                new Municipio("15030", "Coruña, A"),
                new Municipio("28127", "Rozas de Madrid, Las"),
                new Municipio("46901", "Alqueria de la Comtessa, l'")
        ));

        assertEquals("Coruña, A", weatherService.findByNamePrefix("A Coru").getFirst().nombre());
        assertEquals("Coruña, A", weatherService.findByNamePrefix("coruna").getFirst().nombre());
        assertEquals("Rozas de Madrid, Las", weatherService.findByNamePrefix("las rozas").getFirst().nombre());
        assertEquals("Alqueria de la Comtessa, l'", weatherService.findByNamePrefix("l'alqueria").getFirst().nombre());
    }

    @Test
    @DisplayName("Find by name prefix - should return empty list when no match")
    void findByNamePrefix_ShouldReturnEmptyListWhenNoMatch() {
        // Arrange
        when(weatherExternalPort.fetchAllMunicipalities()).thenReturn(List.of(
                new Municipio("03002", "Agost"),
                new Municipio("40001", "Abades")
        ));

        // Act
        List<Municipio> result = weatherService.findByNamePrefix("Madrid");

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(weatherExternalPort, times(1)).fetchAllMunicipalities();
    }

    @Test
    @DisplayName("Get prediction - should use Celsius as default and ask for tomorrow's forecast")
    void getPrediction_ShouldUseCelsiusAsDefaultAndAskForTomorrow() {
        // Arrange: máxima 24 y mínima 13 -> media 18.5 ºC
        when(weatherExternalPort.fetchDailyForecast("03002", TOMORROW))
                .thenReturn(forecast(24, 13));

        // Act: Pasamos null para verificar el uso de Celsius como default
        WeatherPrediction result = weatherService.getPrediction("03002", null);

        // Assert
        assertEquals(TOMORROW, result.fecha());
        assertEquals(TemperatureUnit.G_CEL, result.unidadTemperatura());
        assertEquals(18.5, result.mediaTemperatura());
        assertEquals(2, result.probPrecipitacion().size());
        verify(weatherExternalPort).fetchDailyForecast("03002", TOMORROW);
    }

    @Test
    @DisplayName("Get prediction - should convert to Fahrenheit rounding to one decimal")
    void getPrediction_ShouldConvertToFahrenheitRoundingToOneDecimal() {
        // Media 21.5 ºC -> 70.7 ºF
        when(weatherExternalPort.fetchDailyForecast("03002", TOMORROW))
                .thenReturn(forecast(26, 17));

        WeatherPrediction result = weatherService.getPrediction("03002", TemperatureUnit.G_FAH);

        assertEquals(TemperatureUnit.G_FAH, result.unidadTemperatura());
        assertEquals(70.7, result.mediaTemperatura());
    }

    @Test
    @DisplayName("Get prediction - should compute tomorrow in Spanish time, not in server time")
    void getPrediction_ShouldComputeTomorrowInSpanishTime() {
        // 8 de octubre 22:30 UTC = 9 de octubre 00:30 en España -> "mañana" es el día 10
        Clock lateNightClock = Clock.fixed(Instant.parse("2026-10-08T22:30:00Z"), MADRID);
        weatherService = new WeatherService(weatherExternalPort, lateNightClock);
        LocalDate expectedDate = LocalDate.of(2026, 10, 10);
        when(weatherExternalPort.fetchDailyForecast("03002", expectedDate)).thenReturn(forecast(20, 10));

        weatherService.getPrediction("03002", TemperatureUnit.G_CEL);

        verify(weatherExternalPort).fetchDailyForecast("03002", expectedDate);
    }

    private static DailyForecast forecast(double max, double min) {
        return new DailyForecast(TOMORROW, max, min, List.of(
                new PrecipitationProbability(5, "00-06"),
                new PrecipitationProbability(10, "06-12")
        ));
    }
}
