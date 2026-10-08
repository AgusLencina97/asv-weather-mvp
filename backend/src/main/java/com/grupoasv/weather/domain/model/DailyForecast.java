package com.grupoasv.weather.domain.model;

import java.time.LocalDate;
import java.util.List;

/**
 * Predicción "en bruto" de un día, tal como la necesita el dominio: temperaturas en grados Celsius
 * y probabilidades de precipitación por periodo. Es independiente de la unidad que pida el usuario,
 * lo que permite cachearla una sola vez y convertirla después.
 */
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
