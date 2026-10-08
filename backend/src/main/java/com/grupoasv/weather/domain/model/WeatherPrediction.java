package com.grupoasv.weather.domain.model;

import java.util.List;

public record WeatherPrediction(
        Double mediaTemperatura,
        TemperatureUnit unidadTemperatura,
        List<PrecipitationProbability> probPrecipitacion
) {}