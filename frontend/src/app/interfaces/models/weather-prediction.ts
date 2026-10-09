import { Precipitation } from './precipitation';
import { TemperatureUnit } from './temperature-unit';

export interface WeatherPrediction {
  /** Día de la predicción (ISO yyyy-MM-dd), calculado por el backend en hora de España */
  fecha: string;
  mediaTemperatura: number;
  unidadTemperatura: TemperatureUnit;
  probPrecipitacion: Precipitation[];
}