package com.grupoasv.weather.infrastructure.adapter.out.aemet;

// AEMET responde HTTP 200 aunque el recurso no exista: el resultado real está en "estado"
public record AemetResponseWrapper(String descripcion, int estado, String datos, String metadatos) {}
