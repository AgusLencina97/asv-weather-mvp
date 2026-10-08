package com.grupoasv.weather.infrastructure.adapter.out.aemet;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.grupoasv.weather.domain.exception.WeatherDomainException;
import com.grupoasv.weather.domain.model.Municipio;
import com.grupoasv.weather.domain.model.PrecipitationProbability;
import com.grupoasv.weather.domain.model.TemperatureUnit;
import com.grupoasv.weather.domain.model.WeatherPrediction;
import com.grupoasv.weather.domain.port.out.WeatherExternalPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AemetWeatherAdapter implements WeatherExternalPort {

    private final AemetFeignClient feignClient;
    private final ObjectMapper objectMapper;

    @Value("${aemet.api.key}")
    private String apiKey;

    @Override
    @Cacheable(value = "municipiosCache") // Mitiga la lentitud de la API
    public List<Municipio> fetchAllMunicipalities() {
        try {
            AemetResponseWrapper wrapper = feignClient.getMunicipiosUrl(apiKey);
            String rawJson = feignClient.getDataFromUrl(URI.create(wrapper.datos()));
            JsonNode rootNode = objectMapper.readTree(rawJson);

            List<Municipio> municipios = new ArrayList<>();
            for (JsonNode node : rootNode) {
                // Parseo defensivo: Solo procesa si el nodo realmente tiene "id" y "nombre"
                if (node.hasNonNull("id") && node.hasNonNull("nombre")) {
                    String rawId = node.get("id").asText();
                    String cleanId = rawId.replaceFirst("^id", "");
                    municipios.add(new Municipio(cleanId, node.get("nombre").asText()));
                }
            }
            return municipios;
        } catch (Exception e) {
            log.error("Error al obtener la lista de municipios de AEMET: {}", e.getMessage(), e);
            // El detalle técnico se queda en el log; al cliente solo le llega un mensaje genérico
            throw new WeatherDomainException("Error obteniendo municipios de AEMET", e);
        }
    }

    @Override
    public WeatherPrediction fetchNextDayPrediction(String municipioId, TemperatureUnit unit) {
        try {
            AemetResponseWrapper wrapper = feignClient.getPrediccionUrl(municipioId, apiKey);
            String rawJson = feignClient.getDataFromUrl(URI.create(wrapper.datos()));
            JsonNode rootNode = objectMapper.readTree(rawJson);

            // AEMET devuelve un array, tomamos la predicción del día 1 (mañana)
            JsonNode tomorrowNode = rootNode.get(0).get("prediccion").get("dia").get(1);

            // Extraer probabilidad precipitación
            List<PrecipitationProbability> probs = new ArrayList<>();
            for (JsonNode probNode : tomorrowNode.get("probPrecipitacion")) {
                if (probNode.has("periodo") && !probNode.get("value").asText().isEmpty()) {
                    probs.add(new PrecipitationProbability(
                            probNode.get("value").asInt(),
                            probNode.get("periodo").asText()
                    ));
                }
            }

            // Extraer y calcular temperatura media
            JsonNode tempNode = tomorrowNode.get("temperatura");
            double max = tempNode.get("maxima").asDouble();
            double min = tempNode.get("minima").asDouble();
            double tempCelsius = (max + min) / 2.0;

            double finalTemp = (unit == TemperatureUnit.G_FAH)
                    ? (tempCelsius * 9 / 5) + 32
                    : tempCelsius;

            // Redondeo a 1 decimal
            finalTemp = Math.round(finalTemp * 10.0) / 10.0;

            return new WeatherPrediction(finalTemp, unit, probs);
        } catch (Exception e) {
            log.error("Error al obtener la predicción del tiempo para el municipio {}: {}", municipioId, e.getMessage(), e);
            throw new WeatherDomainException("Error obteniendo la predicción de AEMET", e);
        }
    }
}