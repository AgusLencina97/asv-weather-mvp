package com.grupoasv.weather.infrastructure.adapter.in.web.dto;

import com.grupoasv.weather.domain.model.Municipio;

public record MunicipioResponse(String codigo, String nombre) {

    public static MunicipioResponse from(Municipio municipio) {
        return new MunicipioResponse(municipio.codigo(), municipio.nombre());
    }
}
