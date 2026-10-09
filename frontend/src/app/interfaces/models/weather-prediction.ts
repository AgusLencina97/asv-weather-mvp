import { Precipitation } from './precipitation';
import { TemperatureUnit } from './temperature-unit';

export interface WeatherPrediction {
  /** ISO yyyy-MM-dd */
  fecha: string;
  mediaTemperatura: number;
  unidadTemperatura: TemperatureUnit;
  probPrecipitacion: Precipitation[];
}