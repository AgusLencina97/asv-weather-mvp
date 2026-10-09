package com.grupoasv.weather.application.service;

import com.grupoasv.weather.domain.model.DailyForecast;
import com.grupoasv.weather.domain.model.Municipio;
import com.grupoasv.weather.domain.model.TemperatureUnit;
import com.grupoasv.weather.domain.model.WeatherPrediction;
import com.grupoasv.weather.domain.port.in.FindMunicipalitiesUseCase;
import com.grupoasv.weather.domain.port.in.GetNextDayPredictionUseCase;
import com.grupoasv.weather.domain.port.out.WeatherExternalPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class WeatherService implements FindMunicipalitiesUseCase, GetNextDayPredictionUseCase {

    // AEMET publica algunos nombres con el artículo al final: "Coruña, A", "Rozas de Madrid, Las"
    private static final Pattern ARTICULO_POSPUESTO = Pattern.compile("^(.+), (\\S+)$");

    private final WeatherExternalPort weatherExternalPort;
    // Zona Europe/Madrid: "mañana" es el de España, no el del servidor
    private final Clock clock;

    @Override
    public List<Municipio> findByNamePrefix(String prefix) {
        String normalizedPrefix = normalize(prefix);
        log.debug("Delegando búsqueda de municipios al puerto de salida externo");
        return weatherExternalPort.fetchAllMunicipalities().stream()
                .filter(m -> matchesPrefix(m.nombre(), normalizedPrefix))
                .sorted(Comparator.comparing(m -> normalize(m.nombre())))
                .toList();
    }

    @Override
    public WeatherPrediction getPrediction(String municipioId, TemperatureUnit unit) {
        TemperatureUnit finalUnit = (unit != null) ? unit : TemperatureUnit.DEFAULT;
        LocalDate tomorrow = LocalDate.now(clock).plusDays(1);
        log.debug("Obteniendo predicción para municipio {} del día {} en {}", municipioId, tomorrow, finalUnit);

        DailyForecast forecast = weatherExternalPort.fetchDailyForecast(municipioId, tomorrow);
        double temperature = roundToOneDecimal(finalUnit.fromCelsius(forecast.temperaturaMediaCelsius()));

        return new WeatherPrediction(forecast.fecha(), temperature, finalUnit, forecast.probPrecipitacion());
    }

    private static boolean matchesPrefix(String nombre, String normalizedPrefix) {
        String normalizedName = normalize(nombre);
        if (normalizedName.startsWith(normalizedPrefix)) {
            return true;
        }
        Matcher matcher = ARTICULO_POSPUESTO.matcher(normalizedName);
        if (matcher.matches()) {
            String articulo = matcher.group(2);
            String separador = articulo.endsWith("'") ? "" : " ";
            return (articulo + separador + matcher.group(1)).startsWith(normalizedPrefix);
        }
        return false;
    }

    // "Alcalá" -> "alcala"
    private static String normalize(String text) {
        return Normalizer.normalize(text.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
    }

    private static double roundToOneDecimal(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
