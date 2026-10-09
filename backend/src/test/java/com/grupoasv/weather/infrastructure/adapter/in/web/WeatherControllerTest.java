package com.grupoasv.weather.infrastructure.adapter.in.web;

import com.grupoasv.weather.domain.exception.ResourceNotFoundException;
import com.grupoasv.weather.domain.exception.WeatherDomainException;
import com.grupoasv.weather.domain.model.Municipio;
import com.grupoasv.weather.domain.model.PrecipitationProbability;
import com.grupoasv.weather.domain.model.TemperatureUnit;
import com.grupoasv.weather.domain.model.WeatherPrediction;
import com.grupoasv.weather.domain.port.in.FindMunicipalitiesUseCase;
import com.grupoasv.weather.domain.port.in.GetNextDayPredictionUseCase;
import com.grupoasv.weather.infrastructure.config.JwtConfig;
import com.grupoasv.weather.infrastructure.config.SecurityConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test de la capa web aislada: controlador, validaciones, manejo de errores y seguridad,
 * con los casos de uso simulados.
 */
@WebMvcTest(WeatherController.class)
@Import({SecurityConfig.class, JwtConfig.class})
@TestPropertySource(locations = "classpath:test-secrets.properties")
@WithMockUser
class WeatherControllerTest {

    private static final String BASE_URL = "/api/v1/weather";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FindMunicipalitiesUseCase findMunicipalitiesUseCase;

    @MockitoBean
    private GetNextDayPredictionUseCase getNextDayPredictionUseCase;

    @Test
    @DisplayName("GET municipalities - should return codigo and nombre")
    void searchMunicipalities_ShouldReturnCodigoAndNombre() throws Exception {
        when(findMunicipalitiesUseCase.findByNamePrefix("Ag")).thenReturn(List.of(new Municipio("03002", "Agost")));

        mockMvc.perform(get(BASE_URL + "/municipalities").param("prefix", "Ag"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].codigo").value("03002"))
                .andExpect(jsonPath("$[0].nombre").value("Agost"));
    }

    @Test
    @DisplayName("GET municipalities - should return 400 when prefix is blank")
    void searchMunicipalities_ShouldReturnBadRequestWhenPrefixIsBlank() throws Exception {
        mockMvc.perform(get(BASE_URL + "/municipalities").param("prefix", " "))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(findMunicipalitiesUseCase);
    }

    @Test
    @DisplayName("GET prediction - should return the contract defined in the specification")
    void getPrediction_ShouldReturnExpectedContract() throws Exception {
        when(getNextDayPredictionUseCase.getPrediction("03002", TemperatureUnit.G_FAH)).thenReturn(
                new WeatherPrediction(66.7, TemperatureUnit.G_FAH, List.of(new PrecipitationProbability(5, "00-06"))));

        mockMvc.perform(get(BASE_URL + "/prediction/03002").param("unit", "G_FAH"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mediaTemperatura").value(66.7))
                .andExpect(jsonPath("$.unidadTemperatura").value("G_FAH"))
                .andExpect(jsonPath("$.probPrecipitacion[0].probabilidad").value(5))
                .andExpect(jsonPath("$.probPrecipitacion[0].periodo").value("00-06"));
    }

    @Test
    @DisplayName("GET prediction - should return 400 when municipio code is not 5 digits")
    void getPrediction_ShouldReturnBadRequestWhenCodeIsInvalid() throws Exception {
        mockMvc.perform(get(BASE_URL + "/prediction/12ab"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(getNextDayPredictionUseCase);
    }

    @Test
    @DisplayName("GET prediction - should return 400 when unit is not supported")
    void getPrediction_ShouldReturnBadRequestWhenUnitIsInvalid() throws Exception {
        mockMvc.perform(get(BASE_URL + "/prediction/03002").param("unit", "KELVIN"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET prediction - should return 404 when the municipio does not exist")
    void getPrediction_ShouldReturnNotFound() throws Exception {
        when(getNextDayPredictionUseCase.getPrediction(any(), any()))
                .thenThrow(new ResourceNotFoundException("AEMET no tiene datos para el municipio 99999"));

        mockMvc.perform(get(BASE_URL + "/prediction/99999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Recurso no encontrado"));
    }

    @Test
    @DisplayName("GET prediction - should return 502 without exposing internal details when AEMET fails")
    void getPrediction_ShouldReturnBadGatewayWithoutInternalDetails() throws Exception {
        when(getNextDayPredictionUseCase.getPrediction(any(), any()))
                .thenThrow(new WeatherDomainException("detalle interno con datos sensibles"));

        mockMvc.perform(get(BASE_URL + "/prediction/03002"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.detail", not(containsString("detalle interno"))));
    }

    @Test
    @WithAnonymousUser
    @DisplayName("Security - should return 401 when the request is not authenticated")
    void shouldReturnUnauthorizedWhenNotAuthenticated() throws Exception {
        mockMvc.perform(get(BASE_URL + "/municipalities").param("prefix", "Ag"))
                .andExpect(status().isUnauthorized());
    }
}
