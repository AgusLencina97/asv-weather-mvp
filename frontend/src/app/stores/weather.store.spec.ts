import { TestBed } from '@angular/core/testing';
import { WeatherStore } from './weather.store';
import { WeatherService } from '../services/weather.service';
import { of, throwError } from 'rxjs';
import { vi } from 'vitest';

describe('WeatherStore', () => {
  let store: InstanceType<typeof WeatherStore>;
  let mockWeatherService: {
    searchMunicipalities: ReturnType<typeof vi.fn>;
    getPrediction: ReturnType<typeof vi.fn>;
  };

  const agost = { codigo: '03002', nombre: 'Agost' };
  const prediction = { mediaTemperatura: 20, unidadTemperatura: 'G_CEL', probPrecipitacion: [] };

  beforeEach(() => {
    vi.useFakeTimers();
    vi.spyOn(console, 'error').mockImplementation(() => undefined);

    mockWeatherService = {
      searchMunicipalities: vi.fn().mockReturnValue(of([agost])),
      getPrediction: vi.fn().mockReturnValue(of(prediction)),
    };

    TestBed.configureTestingModule({
      providers: [WeatherStore, { provide: WeatherService, useValue: mockWeatherService }],
    });

    store = TestBed.inject(WeatherStore);
  });

  afterEach(() => {
    vi.useRealTimers();
    vi.restoreAllMocks();
  });

  it('debería inicializar con los valores por defecto', () => {
    expect(store.unit()).toBe('G_CEL');
    expect(store.isLoading()).toBe(false);
    expect(store.municipalities()).toEqual([]);
    expect(store.selectedMunicipio()).toBeNull();
    expect(store.prediction()).toBeNull();
  });

  describe('updateUnit', () => {
    it('debería actualizar la unidad sin pedir predicción si no hay municipio seleccionado', () => {
      store.updateUnit('G_FAH');

      expect(store.unit()).toBe('G_FAH');
      expect(mockWeatherService.getPrediction).not.toHaveBeenCalled();
    });

    it('debería recargar la predicción con la nueva unidad si hay municipio seleccionado', () => {
      store.setSelectedMunicipio(agost);
      mockWeatherService.getPrediction.mockClear();

      store.updateUnit('G_FAH');

      expect(mockWeatherService.getPrediction).toHaveBeenCalledWith('03002', 'G_FAH');
    });
  });

  describe('setSelectedMunicipio', () => {
    it('debería actualizar el municipio seleccionado y pedir la predicción', () => {
      store.setSelectedMunicipio(agost);

      expect(store.selectedMunicipio()).toEqual(agost);
      expect(mockWeatherService.getPrediction).toHaveBeenCalledWith('03002', 'G_CEL');
      expect(store.prediction()).toEqual(prediction);
      expect(store.isLoading()).toBe(false);
    });

    it('debería limpiar la predicción al deseleccionar el municipio', () => {
      store.setSelectedMunicipio(agost);

      store.setSelectedMunicipio(null);

      expect(store.selectedMunicipio()).toBeNull();
      expect(store.prediction()).toBeNull();
    });
  });

  describe('loadPrediction', () => {
    it('debería dejar la predicción en null y apagar isLoading si el servicio falla', () => {
      mockWeatherService.getPrediction.mockReturnValue(throwError(() => new Error('boom')));
      store.setSelectedMunicipio(agost);

      expect(store.prediction()).toBeNull();
      expect(store.isLoading()).toBe(false);
      expect(console.error).toHaveBeenCalled();
    });
  });

  describe('searchMunicipalities', () => {
    it('debería esperar el debounce antes de consultar al servicio', () => {
      store.searchMunicipalities('Ag');

      vi.advanceTimersByTime(299);
      expect(mockWeatherService.searchMunicipalities).not.toHaveBeenCalled();

      vi.advanceTimersByTime(1);
      expect(mockWeatherService.searchMunicipalities).toHaveBeenCalledWith('Ag');
    });

    it('debería cargar los municipios devueltos por el servicio', () => {
      store.searchMunicipalities('Ago');
      vi.advanceTimersByTime(300);

      expect(store.municipalities()).toEqual([agost]);
      expect(store.isLoading()).toBe(false);
    });

    it.each([['vacío', ''], ['de un solo carácter', 'A']])(
      'debería vaciar la lista sin llamar al servicio si el prefijo es %s',
      (_caso, prefix) => {
        store.searchMunicipalities('Ago');
        vi.advanceTimersByTime(300);
        expect(store.municipalities()).toEqual([agost]);
        mockWeatherService.searchMunicipalities.mockClear();

        store.searchMunicipalities(prefix);
        vi.advanceTimersByTime(300);

        expect(store.municipalities()).toEqual([]);
        expect(store.isLoading()).toBe(false);
        expect(mockWeatherService.searchMunicipalities).not.toHaveBeenCalled();
      },
    );

    it('debería vaciar la lista y apagar isLoading si el servicio falla', () => {
      mockWeatherService.searchMunicipalities.mockReturnValue(throwError(() => new Error('boom')));

      store.searchMunicipalities('Ago');
      vi.advanceTimersByTime(300);

      expect(store.municipalities()).toEqual([]);
      expect(store.isLoading()).toBe(false);
      expect(console.error).toHaveBeenCalled();
    });

    it('no debería repetir la consulta si el prefijo no cambia', () => {
      store.searchMunicipalities('Ago');
      vi.advanceTimersByTime(300);
      store.searchMunicipalities('Ago');
      vi.advanceTimersByTime(300);

      expect(mockWeatherService.searchMunicipalities).toHaveBeenCalledTimes(1);
    });

    it('debería consultar solo el último prefijo si se escribe dentro del debounce', () => {
      store.searchMunicipalities('Ag');
      vi.advanceTimersByTime(100);
      store.searchMunicipalities('Ago');
      vi.advanceTimersByTime(300);

      expect(mockWeatherService.searchMunicipalities).toHaveBeenCalledTimes(1);
      expect(mockWeatherService.searchMunicipalities).toHaveBeenCalledWith('Ago');
    });
  });
});
