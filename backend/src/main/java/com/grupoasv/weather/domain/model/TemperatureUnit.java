package com.grupoasv.weather.domain.model;

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
