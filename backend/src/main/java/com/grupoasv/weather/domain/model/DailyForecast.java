package com.grupoasv.weather.domain.model;

import java.time.LocalDate;
import java.util.List;

public record DailyForecast(
        LocalDate fecha,
        double temperaturaMaximaCelsius,
        double temperaturaMinimaCelsius,
        List<PrecipitationProbability> probPrecipitacion
) {
    public double temperaturaMediaCelsius() {
        return (temperaturaMaximaCelsius + temperaturaMinimaCelsius) / 2.0;
    }
}
