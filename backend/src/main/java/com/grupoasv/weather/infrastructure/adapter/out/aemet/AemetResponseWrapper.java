package com.grupoasv.weather.infrastructure.adapter.out.aemet;

public record AemetResponseWrapper(int estado, String datos, String metadatos) {}