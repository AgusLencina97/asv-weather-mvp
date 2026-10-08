package com.grupoasv.weather.application.service;

import com.grupoasv.weather.domain.model.Municipio;
import com.grupoasv.weather.domain.model.TemperatureUnit;
import com.grupoasv.weather.domain.model.WeatherPrediction;
import com.grupoasv.weather.domain.port.out.WeatherExternalPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WeatherServiceTest {

    @Mock
    private WeatherExternalPort weatherExternalPort;

    @InjectMocks
    private WeatherService weatherService;

    @Test
    @DisplayName("Find by name prefix - should return filtered and case sensitive list")
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

        // Assert: Verificamos que filtra correctamente
        assertEquals(3, result.size());
        assertTrue(result.stream().anyMatch(m -> m.nombre().equals("Agost")));
        assertTrue(result.stream().anyMatch(m -> m.nombre().equals("Agres")));
        assertTrue(result.stream().anyMatch(m -> m.nombre().equals("Agaete")));
        assertFalse(result.stream().anyMatch(m -> m.nombre().equals("Abades")));

        verify(weatherExternalPort, times(1)).fetchAllMunicipalities();
    }

    @Test
    @DisplayName("Get prediction - should use Celsius as default when unit is null")
    void getPrediction_ShouldUseCelsiusAsDefaultWhenUnitIsNull() {
        // Arrange
        String municipioId = "40001";
        WeatherPrediction mockPrediction = new WeatherPrediction(11.0, TemperatureUnit.G_CEL, List.of());
        when(weatherExternalPort.fetchNextDayPrediction(municipioId, TemperatureUnit.G_CEL))
                .thenReturn(mockPrediction);

        // Act: Pasamos null para verificar el uso de Celsius como default
        WeatherPrediction result = weatherService.getPrediction(municipioId, null);

        // Assert
        assertNotNull(result);
        assertEquals(TemperatureUnit.G_CEL, result.unidadTemperatura());
        verify(weatherExternalPort, times(1)).fetchNextDayPrediction(municipioId, TemperatureUnit.G_CEL);
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
}