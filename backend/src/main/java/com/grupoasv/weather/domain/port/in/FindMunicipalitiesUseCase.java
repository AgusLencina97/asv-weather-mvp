package com.grupoasv.weather.domain.port.in;

import com.grupoasv.weather.domain.model.Municipio;
import java.util.List;

public interface FindMunicipalitiesUseCase {
    List<Municipio> findByNamePrefix(String prefix);
}