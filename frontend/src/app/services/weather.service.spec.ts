import { TestBed } from '@angular/core/testing';
import { WeatherService } from './weather.service';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Municipio } from '../interfaces/models/municipio';
import { WeatherPrediction } from '../interfaces/models/weather-prediction';

describe('WeatherService', () => {
  let service: WeatherService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [WeatherService, provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(WeatherService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('debería obtener la lista de municipios por prefijo correctamente', () => {
    const mockMunicipios: Municipio[] = [
      { codigo: '03002', nombre: 'Agost' },
      { codigo: '40001', nombre: 'Abades' },
    ];
    let result: Municipio[] | undefined;

    service.searchMunicipalities('Aba').subscribe((res) => (result = res));

    const req = httpMock.expectOne('/api/v1/weather/municipalities?prefix=Aba');
    expect(req.request.method).toBe('GET');
    req.flush(mockMunicipios);

    expect(result).toEqual(mockMunicipios);
  });

  it('debería obtener la predicción del clima pasando la unidad de temperatura', () => {
    const mockPrediction: WeatherPrediction = {
      fecha: '2026-10-10',
      mediaTemperatura: 59.0,
      unidadTemperatura: 'G_FAH',
      probPrecipitacion: [{ probabilidad: 10, periodo: '00-24' }],
    };
    let result: WeatherPrediction | undefined;

    service.getPrediction('40001', 'G_FAH').subscribe((res) => (result = res));

    const req = httpMock.expectOne('/api/v1/weather/prediction/40001?unit=G_FAH');
    expect(req.request.method).toBe('GET');
    req.flush(mockPrediction);

    expect(result).toEqual(mockPrediction);
  });

  it('no debería enviar el parámetro unit si no se indica la unidad', () => {
    service.getPrediction('40001').subscribe();

    const req = httpMock.expectOne('/api/v1/weather/prediction/40001');
    expect(req.request.params.has('unit')).toBe(false);
    req.flush({ fecha: '2026-10-10', mediaTemperatura: 20, unidadTemperatura: 'G_CEL', probPrecipitacion: [] });
  });

  it('debería propagar el error HTTP al buscar municipios', () => {
    let status: number | undefined;

    service.searchMunicipalities('Aba').subscribe({ error: (err) => (status = err.status) });

    httpMock
      .expectOne('/api/v1/weather/municipalities?prefix=Aba')
      .flush('error', { status: 500, statusText: 'Server Error' });

    expect(status).toBe(500);
  });

  it('debería propagar el error HTTP al obtener la predicción', () => {
    let status: number | undefined;

    service.getPrediction('99999').subscribe({ error: (err) => (status = err.status) });

    httpMock
      .expectOne('/api/v1/weather/prediction/99999')
      .flush('not found', { status: 404, statusText: 'Not Found' });

    expect(status).toBe(404);
  });
});
