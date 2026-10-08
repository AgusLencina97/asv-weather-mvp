package com.grupoasv.weather.application.service;

import com.grupoasv.weather.domain.model.Municipio;
import com.grupoasv.weather.domain.model.TemperatureUnit;
import com.grupoasv.weather.domain.model.WeatherPrediction;
import com.grupoasv.weather.domain.port.in.FindMunicipalitiesUseCase;
import com.grupoasv.weather.domain.port.in.GetNextDayPredictionUseCase;
import com.grupoasv.weather.domain.port.out.WeatherExternalPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class WeatherService implements FindMunicipalitiesUseCase, GetNextDayPredictionUseCase {

    private final WeatherExternalPort weatherExternalPort;

    @Override
    public List<Municipio> findByNamePrefix(String prefix) {
        String normalizedPrefix = prefix.toLowerCase();
        log.debug("Delegando búsqueda de municipios al puerto de salida externo");
        return weatherExternalPort.fetchAllMunicipalities().stream()
                .filter(m -> m.nombre().toLowerCase().startsWith(normalizedPrefix))
                .toList();
    }

    @Override
    public WeatherPrediction getPrediction(String municipioId, TemperatureUnit unit) {
        TemperatureUnit finalUnit = (unit != null) ? unit : TemperatureUnit.G_CEL;
        log.debug("Obteniendo predicción para municipio {}, unidad calculada: {}", municipioId, finalUnit);
        return weatherExternalPort.fetchNextDayPrediction(municipioId, finalUnit);
    }
}