import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Municipio } from '../interfaces/models/municipio';
import { WeatherPrediction } from '../interfaces/models/weather-prediction';

@Injectable({ providedIn: 'root' })
export class WeatherService {
  private http = inject(HttpClient);
  private baseUrl = '/api/v1/weather'; 

  searchMunicipalities(prefix: string): Observable<Municipio[]> {
    return this.http.get<Municipio[]>(`${this.baseUrl}/municipalities`, { params: { prefix } });
  }

  getPrediction(municipioId: string, unit?: string): Observable<WeatherPrediction> {
    let params = new HttpParams();
    if (unit) {
      params = params.set('unit', unit);
    }
    return this.http.get<WeatherPrediction>(`${this.baseUrl}/prediction/${municipioId}`, { params });
  }
}