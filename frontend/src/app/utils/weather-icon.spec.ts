import { weatherIconFor } from './weather-icon';
import { WeatherPrediction } from '../interfaces/models/weather-prediction';

describe('weatherIconFor', () => {
  const withProbabilities = (...values: number[]): WeatherPrediction => ({
    fecha: '2026-10-10',
    mediaTemperatura: 20,
    unidadTemperatura: 'G_CEL',
    probPrecipitacion: values.map((probabilidad, i) => ({ probabilidad, periodo: `periodo-${i}` })),
  });

  it.each([
    [[0, 5, 10], 'sunny'],
    [[0, 20, 5], 'partly_cloudy_day'],
    [[10, 50], 'rainy'],
    [[], 'sunny'],
  ])('con probabilidades %j debería devolver "%s"', (values, icon) => {
    expect(weatherIconFor(withProbabilities(...values))).toBe(icon);
  });
});
