import { WeatherPrediction } from '../interfaces/models/weather-prediction';

/**
 * Icono orientativo (Material Symbols) según la probabilidad de precipitación más alta del día.
 * Si en el futuro se expone el estado del cielo de AEMET, bastaría con cambiar esta función.
 */
export function weatherIconFor(prediction: WeatherPrediction): string {
  const maxProbability = Math.max(0, ...prediction.probPrecipitacion.map((p) => p.probabilidad));
  if (maxProbability >= 50) {
    return 'rainy';
  }
  if (maxProbability >= 20) {
    return 'partly_cloudy_day';
  }
  return 'sunny';
}
