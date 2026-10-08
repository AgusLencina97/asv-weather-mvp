package com.grupoasv.weather;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

// Valores ficticios: los tests no deben depender de secretos reales (ni de un .env local ni del CI)
@SpringBootTest(properties = {
		"AEMET_API_KEY=test-api-key",
		"API_USERNAME=test-user",
		"API_PASSWORD=test-password"
})
class WeatherApplicationTests {

	@Test
	void contextLoads() {
	}

}
