package com.grupoasv.weather.domain.model;

import java.time.LocalDate;
import java.util.List;

public record WeatherPrediction(
        LocalDate fecha,
        Double mediaTemperatura,
        TemperatureUnit unidadTemperatura,
        List<PrecipitationProbability> probPrecipitacion
) {}
