import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../config/api.config';
import { Municipio } from '../interfaces/models/municipio';
import { TemperatureUnit } from '../interfaces/models/temperature-unit';
import { WeatherPrediction } from '../interfaces/models/weather-prediction';

@Injectable({ providedIn: 'root' })
export class WeatherService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${API_BASE_URL}/weather`;

  searchMunicipalities(prefix: string): Observable<Municipio[]> {
    return this.http.get<Municipio[]>(`${this.baseUrl}/municipalities`, { params: { prefix } });
  }

  /** Sin unidad, el backend aplica su valor por defecto (G_CEL). */
  getPrediction(municipioId: string, unit?: TemperatureUnit | null): Observable<WeatherPrediction> {
    let params = new HttpParams();
    if (unit) {
      params = params.set('unit', unit);
    }
    return this.http.get<WeatherPrediction>(`${this.baseUrl}/prediction/${municipioId}`, { params });
  }
}