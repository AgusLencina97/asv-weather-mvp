package com.grupoasv.weather.infrastructure.adapter.in.web;

import com.grupoasv.weather.domain.model.Municipio;
import com.grupoasv.weather.domain.model.TemperatureUnit;
import com.grupoasv.weather.domain.model.WeatherPrediction;
import com.grupoasv.weather.domain.port.in.FindMunicipalitiesUseCase;
import com.grupoasv.weather.domain.port.in.GetNextDayPredictionUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
    @Operation(summary = "Busca municipios por prefijo")
    public List<Municipio> searchMunicipalities(@RequestParam String prefix) {
        log.info("Petición recibida para buscar municipios con prefijo: '{}'", prefix);
        List<Municipio> result = findMunicipalitiesUseCase.findByNamePrefix(prefix);
        log.info("Se encontraron {} municipios para el prefijo '{}'", result.size(), prefix);
        return result;
    }

    @GetMapping("/prediction/{municipioId}")
    @Operation(summary = "Obtiene predicción del día siguiente para un municipio")
    public WeatherPrediction getPrediction(
            @PathVariable String municipioId,
            @RequestParam(required = false) TemperatureUnit unit) {
        log.info("Petición recibida para predicción de clima. Municipio ID: {}, Unidad: {}", municipioId, unit);
        return getNextDayPredictionUseCase.getPrediction(municipioId, unit);
    }
}