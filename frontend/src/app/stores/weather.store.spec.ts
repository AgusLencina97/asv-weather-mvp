import { TestBed } from '@angular/core/testing';
import { HttpErrorResponse } from '@angular/common/http';
import { Subject, of, throwError } from 'rxjs';
import { vi } from 'vitest';
import { WeatherStore } from './weather.store';
import { WeatherService } from '../services/weather.service';
import { LastSelection, LastSelectionStorage } from '../services/last-selection.storage';
import { Municipio } from '../interfaces/models/municipio';
import { WeatherPrediction } from '../interfaces/models/weather-prediction';

describe('WeatherStore', () => {
  let store: InstanceType<typeof WeatherStore>;
  let mockWeatherService: {
    searchMunicipalities: ReturnType<typeof vi.fn>;
    getPrediction: ReturnType<typeof vi.fn>;
  };
  let mockStorage: { load: ReturnType<typeof vi.fn>; save: ReturnType<typeof vi.fn> };

  const agost: Municipio = { codigo: '03002', nombre: 'Agost' };
  const paterna: Municipio = { codigo: '46190', nombre: 'Paterna' };
  const prediction: WeatherPrediction = {
    fecha: '2026-10-10',
    mediaTemperatura: 20,
    unidadTemperatura: 'G_CEL',
    probPrecipitacion: [],
  };

  const createStore = (saved: LastSelection | null = null) => {
    mockStorage.load.mockReturnValue(saved);
    store = TestBed.inject(WeatherStore);
  };

  beforeEach(() => {
    vi.useFakeTimers();

    mockWeatherService = {
      searchMunicipalities: vi.fn().mockReturnValue(of([agost])),
      getPrediction: vi.fn().mockReturnValue(of(prediction)),
    };
    mockStorage = { load: vi.fn(), save: vi.fn() };

    TestBed.configureTestingModule({
      providers: [
        WeatherStore,
        { provide: WeatherService, useValue: mockWeatherService },
        { provide: LastSelectionStorage, useValue: mockStorage },
      ],
    });
  });

  afterEach(() => {
    vi.useRealTimers();
    vi.restoreAllMocks();
  });

  it('debería inicializar con los valores por defecto (unidad vacía)', () => {
    createStore();

    expect(store.unit()).toBeNull();
    expect(store.isSearching()).toBe(false);
    expect(store.isLoadingPrediction()).toBe(false);
    expect(store.municipalities()).toEqual([]);
    expect(store.selectedMunicipio()).toBeNull();
    expect(store.prediction()).toBeNull();
    expect(mockWeatherService.getPrediction).not.toHaveBeenCalled();
  });

  describe('último municipio recordado', () => {
    it('debería restaurar la última selección y cargar su predicción al iniciar', () => {
      createStore({ municipio: paterna, unit: 'G_FAH' });

      expect(store.selectedMunicipio()).toEqual(paterna);
      expect(store.unit()).toBe('G_FAH');
      expect(mockWeatherService.getPrediction).toHaveBeenCalledWith('46190', 'G_FAH');
      expect(store.prediction()).toEqual(prediction);
    });

    it('debería guardar el municipio y la unidad cada vez que cambian', () => {
      createStore();

      store.setSelectedMunicipio(agost);
      store.updateUnit('G_FAH');

      expect(mockStorage.save).toHaveBeenNthCalledWith(1, { municipio: agost, unit: null });
      expect(mockStorage.save).toHaveBeenNthCalledWith(2, { municipio: agost, unit: 'G_FAH' });
    });
  });

  describe('updateUnit', () => {
    beforeEach(() => createStore());

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
    beforeEach(() => createStore());

    it('debería pedir la predicción sin unidad (por defecto del backend) si no se eligió ninguna', () => {
      store.setSelectedMunicipio(agost);

      expect(store.selectedMunicipio()).toEqual(agost);
      expect(mockWeatherService.getPrediction).toHaveBeenCalledWith('03002', null);
      expect(store.prediction()).toEqual(prediction);
      expect(store.isLoadingPrediction()).toBe(false);
    });

    it('debería marcar la carga mientras la predicción está en curso', () => {
      const pending = new Subject<WeatherPrediction>();
      mockWeatherService.getPrediction.mockReturnValue(pending);

      store.setSelectedMunicipio(agost);
      expect(store.isLoadingPrediction()).toBe(true);

      pending.next(prediction);
      expect(store.isLoadingPrediction()).toBe(false);
    });

    it('debería mostrar el mensaje del backend si la predicción falla', () => {
      mockWeatherService.getPrediction.mockReturnValue(throwError(() => new HttpErrorResponse({
        status: 502,
        error: { detail: 'No se pudo obtener la información de AEMET. Inténtalo de nuevo en unos minutos.' },
      })));

      store.setSelectedMunicipio(agost);

      expect(store.prediction()).toBeNull();
      expect(store.isLoadingPrediction()).toBe(false);
      expect(store.predictionError()).toContain('AEMET');
    });

    it('debería limpiar el error anterior al pedir una nueva predicción', () => {
      mockWeatherService.getPrediction.mockReturnValueOnce(throwError(() => new Error('boom')));
      store.setSelectedMunicipio(agost);
      expect(store.predictionError()).not.toBeNull();

      store.setSelectedMunicipio(paterna);

      expect(store.predictionError()).toBeNull();
      expect(store.prediction()).toEqual(prediction);
    });
  });

  describe('searchMunicipalities', () => {
    beforeEach(() => createStore());

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
      expect(store.isSearching()).toBe(false);
      expect(store.noResults()).toBe(false);
    });

    it('debería indicar "sin resultados" cuando la búsqueda no encuentra municipios', () => {
      mockWeatherService.searchMunicipalities.mockReturnValue(of([]));

      store.searchMunicipalities('Xyz');
      vi.advanceTimersByTime(300);

      expect(store.noResults()).toBe(true);
    });

    it('no debería afectar a la carga de la predicción (indicadores independientes)', () => {
      const pendingSearch = new Subject<Municipio[]>();
      mockWeatherService.searchMunicipalities.mockReturnValue(pendingSearch);

      store.searchMunicipalities('Ago');
      vi.advanceTimersByTime(300);
      store.setSelectedMunicipio(agost);

      expect(store.isSearching()).toBe(true);
      expect(store.isLoadingPrediction()).toBe(false);
    });

    it.each([['vacío', ''], ['de un solo carácter', 'A'], ['solo con espacios', '   ']])(
      'debería vaciar la lista sin llamar al servicio si el prefijo es %s',
      (_caso, prefix) => {
        store.searchMunicipalities('Ago');
        vi.advanceTimersByTime(300);
        mockWeatherService.searchMunicipalities.mockClear();

        store.searchMunicipalities(prefix);
        vi.advanceTimersByTime(300);

        expect(store.municipalities()).toEqual([]);
        expect(store.noResults()).toBe(false);
        expect(mockWeatherService.searchMunicipalities).not.toHaveBeenCalled();
      },
    );

    it('debería mostrar un error y vaciar la lista si el servicio falla', () => {
      mockWeatherService.searchMunicipalities.mockReturnValue(
        throwError(() => new HttpErrorResponse({ status: 0 })),
      );

      store.searchMunicipalities('Ago');
      vi.advanceTimersByTime(300);

      expect(store.municipalities()).toEqual([]);
      expect(store.isSearching()).toBe(false);
      expect(store.searchError()).toContain('No se pudo conectar');
      expect(store.noResults()).toBe(false);
    });

    it('no debería repetir la consulta si el prefijo no cambia (ignorando espacios)', () => {
      store.searchMunicipalities('Ago');
      vi.advanceTimersByTime(300);
      store.searchMunicipalities('Ago ');
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
