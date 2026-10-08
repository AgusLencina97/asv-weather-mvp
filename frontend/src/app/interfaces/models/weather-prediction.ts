import { Precipitation } from './precipitation';

export interface WeatherPrediction {
  mediaTemperatura: number;
  unidadTemperatura: string;
  probPrecipitacion: Precipitation[];
}