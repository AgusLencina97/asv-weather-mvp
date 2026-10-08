package com.grupoasv.weather.infrastructure.adapter.in.web.dto;

import com.grupoasv.weather.domain.model.TemperatureUnit;
import com.grupoasv.weather.domain.model.WeatherPrediction;

import java.util.List;

/** Contrato público de la API para la predicción del día siguiente. */
public record PredictionResponse(
        Double mediaTemperatura,
        TemperatureUnit unidadTemperatura,
        List<PrecipitationResponse> probPrecipitacion
) {

    public record PrecipitationResponse(Integer probabilidad, String periodo) {}

    public static PredictionResponse from(WeatherPrediction prediction) {
        List<PrecipitationResponse> precipitaciones = prediction.probPrecipitacion().stream()
                .map(p -> new PrecipitationResponse(p.probabilidad(), p.periodo()))
                .toList();
        return new PredictionResponse(prediction.mediaTemperatura(), prediction.unidadTemperatura(), precipitaciones);
    }
}
