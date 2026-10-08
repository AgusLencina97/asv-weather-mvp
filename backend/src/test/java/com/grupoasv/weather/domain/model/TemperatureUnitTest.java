package com.grupoasv.weather.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TemperatureUnitTest {

    @ParameterizedTest(name = "{0} ºC = {1} ºF")
    @CsvSource({"0, 32", "100, 212", "-40, -40", "15, 59"})
    @DisplayName("G_FAH - should convert from Celsius")
    void fahrenheit_ShouldConvertFromCelsius(double celsius, double expectedFahrenheit) {
        assertEquals(expectedFahrenheit, TemperatureUnit.G_FAH.fromCelsius(celsius), 0.0001);
    }

    @ParameterizedTest
    @CsvSource({"0", "18.5", "-3.2"})
    @DisplayName("G_CEL - should keep the Celsius value")
    void celsius_ShouldKeepValue(double celsius) {
        assertEquals(celsius, TemperatureUnit.G_CEL.fromCelsius(celsius));
    }
}
