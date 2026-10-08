package com.grupoasv.weather.infrastructure.adapter.out.aemet;

/**
 * Primera respuesta de AEMET. El campo "estado" es el resultado real de la petición:
 * AEMET contesta HTTP 200 incluso cuando el recurso no existe (estado 404).
 */
public record AemetResponseWrapper(String descripcion, int estado, String datos, String metadatos) {}
