package com.grupoasv.weather.domain.model;

/**
 * Unidades de temperatura soportadas. AEMET siempre entrega grados Celsius,
 * por lo que cada unidad sabe convertir desde Celsius (regla de negocio del dominio).
 */
public enum TemperatureUnit {
    G_CEL {
        @Override
        public double fromCelsius(double celsius) {
            return celsius;
        }
    },
    G_FAH {
        @Override
        public double fromCelsius(double celsius) {
            return celsius * 9 / 5 + 32;
        }
    };

    public static final TemperatureUnit DEFAULT = G_CEL;

    public abstract double fromCelsius(double celsius);
}
