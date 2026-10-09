package com.grupoasv.weather.infrastructure.adapter.in.web.dto;

import com.grupoasv.weather.domain.model.TemperatureUnit;
import com.grupoasv.weather.domain.model.WeatherPrediction;

import java.time.LocalDate;
import java.util.List;

/**
 * Contrato público de la API para la predicción del día siguiente. Respecto al enunciado se añade "fecha"
 * (día al que corresponde la predicción) para que el cliente no tenga que calcularlo con su propio reloj.
 */
public record PredictionResponse(
        LocalDate fecha,
        Double mediaTemperatura,
        TemperatureUnit unidadTemperatura,
        List<PrecipitationResponse> probPrecipitacion
) {

    public record PrecipitationResponse(Integer probabilidad, String periodo) {}

    public static PredictionResponse from(WeatherPrediction prediction) {
        List<PrecipitationResponse> precipitaciones = prediction.probPrecipitacion().stream()
                .map(p -> new PrecipitationResponse(p.probabilidad(), p.periodo()))
                .toList();
        return new PredictionResponse(prediction.fecha(), prediction.mediaTemperatura(),
                prediction.unidadTemperatura(), precipitaciones);
    }
}
