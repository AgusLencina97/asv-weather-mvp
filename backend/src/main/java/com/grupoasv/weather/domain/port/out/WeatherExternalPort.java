package com.grupoasv.weather.domain.port.out;

import com.grupoasv.weather.domain.model.DailyForecast;
import com.grupoasv.weather.domain.model.Municipio;
import java.time.LocalDate;
import java.util.List;

public interface WeatherExternalPort {
    List<Municipio> fetchAllMunicipalities();

    /**
     * Devuelve la predicción del municipio para la fecha indicada, con temperaturas en grados Celsius.
     *
     * @throws com.grupoasv.weather.domain.exception.ResourceNotFoundException si el municipio no existe
     *         o el proveedor no tiene predicción para esa fecha
     */
    DailyForecast fetchDailyForecast(String municipioId, LocalDate fecha);
}
