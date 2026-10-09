package com.grupoasv.weather;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(locations = "classpath:test-secrets.properties")
class WeatherApplicationTests {

	@Test
	void contextLoads() {
	}

}
