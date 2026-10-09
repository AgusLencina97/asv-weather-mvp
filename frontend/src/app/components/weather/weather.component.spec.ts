import { ComponentFixture, TestBed } from '@angular/core/testing';
import { LOCALE_ID, signal } from '@angular/core';
import { registerLocaleData } from '@angular/common';
import localeEs from '@angular/common/locales/es';
import { MatAutocompleteSelectedEvent } from '@angular/material/autocomplete';
import { Observable } from 'rxjs';
import { vi } from 'vitest';
import { WeatherComponent } from './weather.component';
import { WeatherStore } from '../../stores/weather.store';
import { AuthService } from '../../services/auth.service';
import { Municipio } from '../../interfaces/models/municipio';
import { TemperatureUnit } from '../../interfaces/models/temperature-unit';
import { WeatherPrediction } from '../../interfaces/models/weather-prediction';

// Mismo locale que la app (app.config.ts) para comprobar el formato español de fechas y números
registerLocaleData(localeEs, 'es');

describe('WeatherComponent', () => {
  let component: WeatherComponent;
  let fixture: ComponentFixture<WeatherComponent>;
  let searchedValues: string[];
  let mockAuthService: { logout: ReturnType<typeof vi.fn> };

  const agost: Municipio = { codigo: '03002', nombre: 'Agost' };
  const prediction: WeatherPrediction = {
    fecha: '2026-10-10',
    mediaTemperatura: 21.5,
    unidadTemperatura: 'G_CEL',
    probPrecipitacion: [
      { probabilidad: 10, periodo: '00-12' },
      { probabilidad: 80, periodo: '12-24' },
    ],
  };

  // Simula el store: searchMunicipalities recibe el observable del input, igual que un rxMethod
  const createMockStore = () => ({
    municipalities: signal<Municipio[]>([agost]),
    noResults: signal(false),
    isSearching: signal(false),
    searchError: signal<string | null>(null),
    unit: signal<TemperatureUnit | null>(null),
    selectedMunicipio: signal<Municipio | null>(null),
    prediction: signal<WeatherPrediction | null>(null),
    isLoadingPrediction: signal(false),
    predictionError: signal<string | null>(null),
    searchMunicipalities: vi.fn((source$: Observable<string>) => source$.subscribe((v) => searchedValues.push(v))),
    setSelectedMunicipio: vi.fn(),
    updateUnit: vi.fn(),
  });
  let mockStore: ReturnType<typeof createMockStore>;

  const el = () => fixture.nativeElement as HTMLElement;

  const createComponent = () => {
    fixture = TestBed.createComponent(WeatherComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  };

  beforeEach(async () => {
    searchedValues = [];
    mockStore = createMockStore();
    mockAuthService = { logout: vi.fn() };

    await TestBed.configureTestingModule({
      imports: [WeatherComponent],
      providers: [
        { provide: WeatherStore, useValue: mockStore },
        { provide: AuthService, useValue: mockAuthService },
        { provide: LOCALE_ID, useValue: 'es' },
      ],
    }).compileComponents();
  });

  it('debería crearse correctamente', () => {
    createComponent();

    expect(component).toBeTruthy();
  });

  describe('búsqueda', () => {
    beforeEach(() => createComponent());

    it('debería enviar al store el texto que escribe el usuario', () => {
      component.searchControl.setValue('Ago');
      component.searchControl.setValue('');

      expect(searchedValues).toEqual(['Ago', '']);
    });

    it('no debería buscar cuando se elige una opción (el valor es un Municipio)', () => {
      component.searchControl.setValue(agost);

      expect(searchedValues).toEqual([]);
    });

    it('debería mostrar "sin resultados" en el autocompletado', () => {
      mockStore.municipalities.set([]);
      mockStore.noResults.set(true);
      fixture.detectChanges();

      el().querySelector('input')!.dispatchEvent(new Event('focusin'));
      fixture.detectChanges();

      expect(document.body.textContent).toContain('No se encontraron municipios');
    });

    it('debería mostrar el error de búsqueda', () => {
      mockStore.searchError.set('No se pudo conectar con el servidor.');
      fixture.detectChanges();

      expect(el().querySelector('[role="alert"]')?.textContent).toContain('No se pudo conectar');
    });
  });

  describe('displayMunicipio', () => {
    beforeEach(() => createComponent());

    it.each([
      ['un municipio', agost, 'Agost'],
      ['un texto', 'Ago', 'Ago'],
      ['null', null, ''],
    ])('debería mostrar correctamente %s en el input', (_caso, value, expected) => {
      expect(component.displayMunicipio(value)).toBe(expected);
    });
  });

  describe('eventos', () => {
    beforeEach(() => createComponent());

    it('debería llamar a updateUnit del store al cambiar la unidad', () => {
      component.onUnitChange('G_FAH');

      expect(mockStore.updateUnit).toHaveBeenCalledWith('G_FAH');
    });

    it('debería informar al store del municipio elegido en el autocomplete', () => {
      component.onMunicipioSelected({ option: { value: agost } } as MatAutocompleteSelectedEvent);

      expect(mockStore.setSelectedMunicipio).toHaveBeenCalledWith(agost);
    });

    it('debería cerrar la sesión al pulsar el botón de logout', () => {
      (el().querySelector('button[aria-label="Cerrar sesión"]') as HTMLButtonElement).click();

      expect(mockAuthService.logout).toHaveBeenCalledTimes(1);
    });
  });

  describe('último municipio recordado', () => {
    it('debería mostrar en el input el municipio restaurado por el store', async () => {
      mockStore.selectedMunicipio.set(agost);

      createComponent();
      await fixture.whenStable();

      expect(el().querySelector('input')!.value).toBe('Agost');
    });
  });

  describe('plantilla', () => {
    beforeEach(() => createComponent());

    it('debería mostrar el mensaje inicial cuando no hay predicción', () => {
      expect(el().querySelector('.empty-state')?.textContent).toContain('Busca un municipio');
      expect(el().querySelector('mat-card')).toBeNull();
    });

    it('debería mostrar el spinner mientras carga la predicción', () => {
      mockStore.isLoadingPrediction.set(true);
      fixture.detectChanges();

      expect(el().querySelector('.spinner-container mat-spinner')).not.toBeNull();
    });

    it('debería mostrar el spinner pequeño del buscador mientras busca', () => {
      mockStore.isSearching.set(true);
      fixture.detectChanges();

      expect(el().querySelector('.search-spinner')).not.toBeNull();
      expect(el().querySelector('.spinner-container')).toBeNull();
    });

    it('debería mostrar el error de la predicción', () => {
      mockStore.predictionError.set('AEMET no tiene datos para el municipio');
      fixture.detectChanges();

      expect(el().querySelector('[role="alert"]')?.textContent).toContain('AEMET no tiene datos');
      expect(el().querySelector('mat-card')).toBeNull();
    });

    it('debería mostrar la tarjeta con la fecha de la predicción, temperatura en °C y probabilidades', () => {
      mockStore.selectedMunicipio.set(agost);
      mockStore.prediction.set(prediction);
      fixture.detectChanges();

      expect(el().querySelector('mat-card-title')?.textContent).toContain('Agost');
      expect(el().querySelector('mat-card-subtitle')?.textContent).toContain('10 octubre 2026');
      expect(el().querySelector('.temp-value')?.textContent).toContain('21,5');
      expect(el().querySelector('.temp-unit')?.textContent).toContain('°C');

      const items = el().querySelectorAll('.prob-item');
      expect(items.length).toBe(2);
      expect(items[1].textContent).toContain('80%');
      expect(items[1].textContent).toContain('12-24');
    });

    it('debería elegir el icono según la probabilidad de lluvia', () => {
      mockStore.selectedMunicipio.set(agost);
      mockStore.prediction.set(prediction);
      fixture.detectChanges();

      expect(el().querySelector('.weather-icon')?.textContent?.trim()).toBe('rainy');
      expect(el().querySelector('.weather-icon')?.classList).not.toContain('weather-icon--sunny');
    });

    it('debería marcar el icono de sol para mostrarlo en otro color', () => {
      mockStore.selectedMunicipio.set(agost);
      mockStore.prediction.set({ ...prediction, probPrecipitacion: [{ probabilidad: 0, periodo: '00-24' }] });
      fixture.detectChanges();

      expect(el().querySelector('.weather-icon')?.textContent?.trim()).toBe('sunny');
      expect(el().querySelector('.weather-icon')?.classList).toContain('weather-icon--sunny');
    });

    it('debería mostrar °F cuando la unidad de la predicción es G_FAH', () => {
      mockStore.selectedMunicipio.set(agost);
      mockStore.prediction.set({ ...prediction, unidadTemperatura: 'G_FAH', mediaTemperatura: 70.7 });
      fixture.detectChanges();

      expect(el().querySelector('.temp-unit')?.textContent).toContain('°F');
    });
  });
});
