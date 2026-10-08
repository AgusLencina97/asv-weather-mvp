package com.grupoasv.weather.domain.port.out;

import com.grupoasv.weather.domain.model.Municipio;
import com.grupoasv.weather.domain.model.TemperatureUnit;
import com.grupoasv.weather.domain.model.WeatherPrediction;
import java.util.List;

public interface WeatherExternalPort {
    List<Municipio> fetchAllMunicipalities();
    WeatherPrediction fetchNextDayPrediction(String municipioId, TemperatureUnit unit);
}