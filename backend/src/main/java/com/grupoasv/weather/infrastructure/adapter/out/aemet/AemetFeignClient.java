package com.grupoasv.weather.infrastructure.adapter.out.aemet;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import java.net.URI;

@FeignClient(name = "aemetClient", url = "https://opendata.aemet.es/opendata/api")
public interface AemetFeignClient {

    @GetMapping("/maestro/municipios")
    AemetResponseWrapper getMunicipiosUrl(@RequestParam("api_key") String apiKey);

    @GetMapping("/prediccion/especifica/municipio/diaria/{id}")
    AemetResponseWrapper getPrediccionUrl(@PathVariable("id") String municipioId, @RequestParam("api_key") String apiKey);

    // Feign permite pasar una URI dinámica para resolver el segundo paso de AEMET
    @GetMapping
    String getDataFromUrl(URI baseUrl);
}