package com.grupoasv.weather.infrastructure.adapter.in.web;

import com.grupoasv.weather.domain.model.TemperatureUnit;
import com.grupoasv.weather.domain.port.in.FindMunicipalitiesUseCase;
import com.grupoasv.weather.domain.port.in.GetNextDayPredictionUseCase;
import com.grupoasv.weather.infrastructure.adapter.in.web.dto.MunicipioResponse;
import com.grupoasv.weather.infrastructure.adapter.in.web.dto.PredictionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/weather")
@RequiredArgsConstructor
@Tag(name = "Clima", description = "Endpoints para consulta de municipios y clima (MVP)")
public class WeatherController {

    private final FindMunicipalitiesUseCase findMunicipalitiesUseCase;
    private final GetNextDayPredictionUseCase getNextDayPredictionUseCase;

    @GetMapping("/municipalities")
    @Operation(summary = "Busca municipios por prefijo (sin distinguir mayúsculas ni tildes)")
    public List<MunicipioResponse> searchMunicipalities(
            @RequestParam @NotBlank @Size(max = 50) String prefix) {
        log.info("Petición recibida para buscar municipios con prefijo: '{}'", prefix);
        List<MunicipioResponse> result = findMunicipalitiesUseCase.findByNamePrefix(prefix).stream()
                .map(MunicipioResponse::from)
                .toList();
        log.info("Se encontraron {} municipios para el prefijo '{}'", result.size(), prefix);
        return result;
    }

    @GetMapping("/prediction/{municipioId}")
    @Operation(summary = "Obtiene predicción del día siguiente para un municipio")
    public PredictionResponse getPrediction(
            @PathVariable @Pattern(regexp = "\\d{5}", message = "debe ser un código INE de 5 dígitos") String municipioId,
            @Parameter(description = "Unidad de temperatura. Por defecto G_CEL")
            @RequestParam(required = false) TemperatureUnit unit) {
        log.info("Petición recibida para predicción de clima. Municipio ID: {}, Unidad: {}", municipioId, unit);
        return PredictionResponse.from(getNextDayPredictionUseCase.getPrediction(municipioId, unit));
    }
}
