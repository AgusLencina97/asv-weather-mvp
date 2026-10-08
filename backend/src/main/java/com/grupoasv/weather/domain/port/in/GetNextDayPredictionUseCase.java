package com.grupoasv.weather.domain.port.in;

import com.grupoasv.weather.domain.model.TemperatureUnit;
import com.grupoasv.weather.domain.model.WeatherPrediction;

public interface GetNextDayPredictionUseCase {
    WeatherPrediction getPrediction(String municipioId, TemperatureUnit unit);
}