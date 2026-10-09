import { WeatherPrediction } from '../interfaces/models/weather-prediction';

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
