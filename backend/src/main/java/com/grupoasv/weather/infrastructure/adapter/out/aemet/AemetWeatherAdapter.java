package com.grupoasv.weather.infrastructure.adapter.out.aemet;

import com.grupoasv.weather.domain.exception.ResourceNotFoundException;
import com.grupoasv.weather.domain.exception.WeatherDomainException;
import com.grupoasv.weather.domain.model.DailyForecast;
import com.grupoasv.weather.domain.model.Municipio;
import com.grupoasv.weather.domain.model.PrecipitationProbability;
import com.grupoasv.weather.domain.port.out.WeatherExternalPort;
import com.grupoasv.weather.infrastructure.config.CacheConfig;
import feign.FeignException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Adaptador de salida hacia AEMET OpenData. Traduce el formato de AEMET al modelo de dominio
 * (capa anticorrupción) y no contiene reglas de negocio: devuelve siempre grados Celsius.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AemetWeatherAdapter implements WeatherExternalPort {

    private static final int ESTADO_OK = 200;
    private static final int ESTADO_NOT_FOUND = 404;
    // AEMET mezcla tramos solapados de 24, 12 y 6 horas, y no todos los días los traen todos
    // (justo después de medianoche "mañana" solo tiene tramos de 12h). Se usa el más fino disponible.
    private static final List<Set<String>> GRANULARIDADES = List.of(
            Set.of("00-06", "06-12", "12-18", "18-24"),
            Set.of("00-12", "12-24"),
            Set.of("00-24"));

    private final AemetFeignClient feignClient;
    private final JsonMapper jsonMapper;

    @Value("${aemet.api.key}")
    private String apiKey;

    // Una key vacía (p. ej. .env copiado sin rellenar) se detecta al arrancar, no en la primera búsqueda
    @PostConstruct
    void validateApiKey() {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("AEMET_API_KEY está vacía: configúrala en backend/.env (ver .env.example)");
        }
    }

    @Override
    // sync = true: si varias peticiones encuentran la caché vacía a la vez, solo una llama a AEMET y el resto espera su resultado
    @Cacheable(value = CacheConfig.MUNICIPIOS, sync = true) // El listado es lento y casi nunca cambia
    public List<Municipio> fetchAllMunicipalities() {
        JsonNode root = fetchDatos(() -> feignClient.getMunicipiosUrl(apiKey), "el listado de municipios");

        List<Municipio> municipios = new ArrayList<>();
        for (JsonNode node : root) {
            // Parseo defensivo: solo procesa si el nodo realmente tiene "id" y "nombre"
            if (node.hasNonNull("id") && node.hasNonNull("nombre")) {
                String cleanId = node.get("id").asString().replaceFirst("^id", "");
                municipios.add(new Municipio(cleanId, node.get("nombre").asString()));
            }
        }
        return municipios;
    }

    @Override
    @Cacheable(value = CacheConfig.PREDICCIONES, sync = true) // Clave = municipio + fecha: no depende de la unidad y caduca sola al cambiar de día
    public DailyForecast fetchDailyForecast(String municipioId, LocalDate fecha) {
        JsonNode root = fetchDatos(() -> feignClient.getPrediccionUrl(municipioId, apiKey),
                "la predicción del municipio " + municipioId);
        JsonNode dia = findDay(root, fecha, municipioId);

        List<PrecipitationProbability> probs = parsePrecipitation(dia.path("probPrecipitacion"));

        JsonNode temperatura = dia.path("temperatura");
        JsonNode maxima = temperatura.path("maxima");
        JsonNode minima = temperatura.path("minima");
        if (!maxima.isNumber() || !minima.isNumber()) {
            throw new WeatherDomainException("AEMET no incluye temperaturas para el " + fecha);
        }
        return new DailyForecast(fecha, maxima.asDouble(), minima.asDouble(), probs);
    }

    /**
     * AEMET responde en dos pasos: la primera llamada devuelve un "estado" y una URL temporal ("datos")
     * donde descargar el contenido real. Este método resuelve ambos pasos y traduce los errores al dominio.
     */
    private JsonNode fetchDatos(Supplier<AemetResponseWrapper> peticion, String recurso) {
        try {
            AemetResponseWrapper respuesta = peticion.get();
            if (respuesta.estado() == ESTADO_NOT_FOUND) {
                throw new ResourceNotFoundException("AEMET no tiene datos para " + recurso);
            }
            if (respuesta.estado() != ESTADO_OK || respuesta.datos() == null) {
                log.error("AEMET respondió estado {} al pedir {}: {}", respuesta.estado(), recurso, respuesta.descripcion());
                throw new WeatherDomainException("AEMET no pudo devolver " + recurso);
            }
            return jsonMapper.readTree(feignClient.getDataFromUrl(URI.create(respuesta.datos())));
        } catch (FeignException | JacksonException e) {
            // El detalle técnico se queda en el log; al cliente solo le llega un mensaje genérico
            log.error("Error al obtener {} de AEMET: {}", recurso, e.getMessage(), e);
            throw new WeatherDomainException("Error obteniendo " + recurso + " de AEMET", e);
        }
    }

    private static List<PrecipitationProbability> parsePrecipitation(JsonNode probPrecipitacion) {
        List<PrecipitationProbability> todas = new ArrayList<>();
        for (JsonNode probNode : probPrecipitacion) {
            JsonNode value = probNode.path("value");
            if (probNode.hasNonNull("periodo") && value.isNumber()) {
                todas.add(new PrecipitationProbability(value.asInt(), probNode.get("periodo").asString()));
            }
        }
        for (Set<String> periodos : GRANULARIDADES) {
            List<PrecipitationProbability> seleccion = todas.stream()
                    .filter(p -> periodos.contains(p.periodo()))
                    .toList();
            if (!seleccion.isEmpty()) {
                return seleccion;
            }
        }
        return List.of();
    }

    private JsonNode findDay(JsonNode root, LocalDate fecha, String municipioId) {
        for (JsonNode dia : root.path(0).path("prediccion").path("dia")) {
            // Se busca por fecha y no por posición: el primer día del array no siempre es "hoy"
            if (dia.path("fecha").asString("").startsWith(fecha.toString())) {
                return dia;
            }
        }
        throw new ResourceNotFoundException("AEMET no tiene predicción del " + fecha + " para el municipio " + municipioId);
    }
}
