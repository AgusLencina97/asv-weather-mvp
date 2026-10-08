import { ComponentFixture, TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';
import { MatAutocompleteSelectedEvent } from '@angular/material/autocomplete';
import { vi } from 'vitest';
import { WeatherComponent } from './weather.component';
import { WeatherStore } from '../../stores/weather.store';
import { Municipio } from '../../interfaces/models/municipio';
import { WeatherPrediction } from '../../interfaces/models/weather-prediction';

describe('WeatherComponent', () => {
  let component: WeatherComponent;
  let fixture: ComponentFixture<WeatherComponent>;
  let mockStore: {
    municipalities: ReturnType<typeof signal<Municipio[]>>;
    unit: ReturnType<typeof signal<string>>;
    isLoading: ReturnType<typeof signal<boolean>>;
    prediction: ReturnType<typeof signal<WeatherPrediction | null>>;
    selectedMunicipio: ReturnType<typeof signal<Municipio | null>>;
    searchMunicipalities: ReturnType<typeof vi.fn>;
    setSelectedMunicipio: ReturnType<typeof vi.fn>;
    updateUnit: ReturnType<typeof vi.fn>;
  };

  const agost: Municipio = { codigo: '03002', nombre: 'Agost' };
  const el = () => fixture.nativeElement as HTMLElement;

  beforeEach(async () => {
    mockStore = {
      municipalities: signal<Municipio[]>([agost]),
      unit: signal('G_CEL'),
      isLoading: signal(false),
      prediction: signal<WeatherPrediction | null>(null),
      selectedMunicipio: signal<Municipio | null>(null),
      searchMunicipalities: vi.fn(),
      setSelectedMunicipio: vi.fn(),
      updateUnit: vi.fn(),
    };

    await TestBed.configureTestingModule({
      imports: [WeatherComponent],
      providers: [{ provide: WeatherStore, useValue: mockStore }],
    }).compileComponents();

    fixture = TestBed.createComponent(WeatherComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('debería crearse correctamente', () => {
    expect(component).toBeTruthy();
  });

  describe('búsqueda', () => {
    it('debería buscar municipios en el store cuando el usuario escribe texto', () => {
      component.searchControl.setValue('Ago');

      expect(mockStore.searchMunicipalities).toHaveBeenCalledWith('Ago');
    });

    it('debería buscar con cadena vacía al limpiar el campo', () => {
      component.searchControl.setValue('');

      expect(mockStore.searchMunicipalities).toHaveBeenCalledWith('');
    });

    it('no debería buscar cuando el valor no es texto (municipio seleccionado o null)', () => {
      component.searchControl.setValue(agost as unknown as string);
      component.searchControl.setValue(null);

      expect(mockStore.searchMunicipalities).not.toHaveBeenCalled();
    });
  });

  describe('displayFn', () => {
    it('debería formatear correctamente el nombre del municipio en el Autocomplete', () => {
      expect(component.displayFn({ codigo: '1', nombre: 'Valencia' })).toBe('Valencia');
    });

    it.each([
      ['null', null],
      ['undefined', undefined],
      ['sin nombre', { codigo: '1', nombre: '' }],
    ])('debería devolver cadena vacía para un municipio %s', (_caso, value) => {
      expect(component.displayFn(value as unknown as Municipio)).toBe('');
    });
  });

  describe('eventos', () => {
    it('debería llamar a updateUnit del store al cambiar la unidad', () => {
      component.onUnitChange('G_FAH');

      expect(mockStore.updateUnit).toHaveBeenCalledWith('G_FAH');
    });

    it('debería informar al store del municipio elegido en el autocomplete', () => {
      component.onMunicipioSelected({ option: { value: agost } } as MatAutocompleteSelectedEvent);

      expect(mockStore.setSelectedMunicipio).toHaveBeenCalledWith(agost);
    });
  });

  describe('plantilla', () => {
    const prediction: WeatherPrediction = {
      mediaTemperatura: 21,
      unidadTemperatura: 'G_CEL',
      probPrecipitacion: [
        { probabilidad: 10, periodo: '00-12' },
        { probabilidad: 80, periodo: '12-24' },
      ],
    };

    it('no debería mostrar spinner ni tarjeta del clima por defecto', () => {
      expect(el().querySelector('mat-spinner')).toBeNull();
      expect(el().querySelector('mat-card')).toBeNull();
    });

    it('debería mostrar el spinner mientras carga', () => {
      mockStore.isLoading.set(true);
      fixture.detectChanges();

      expect(el().querySelector('mat-spinner')).not.toBeNull();
    });

    it('no debería mostrar la tarjeta si hay predicción pero no municipio seleccionado', () => {
      mockStore.prediction.set(prediction);
      fixture.detectChanges();

      expect(el().querySelector('mat-card')).toBeNull();
    });

    it('debería mostrar la tarjeta con municipio, temperatura en °C y probabilidades', () => {
      mockStore.selectedMunicipio.set(agost);
      mockStore.prediction.set(prediction);
      fixture.detectChanges();

      expect(el().querySelector('mat-card-title')?.textContent).toContain('Agost');
      expect(el().querySelector('.temp-value')?.textContent).toContain('21');
      expect(el().querySelector('.temp-unit')?.textContent).toContain('°C');

      const items = el().querySelectorAll('.prob-item');
      expect(items.length).toBe(2);
      expect(items[1].textContent).toContain('80%');
      expect(items[1].textContent).toContain('12-24');
    });

    it('debería mostrar °F cuando la unidad de la predicción es G_FAH', () => {
      mockStore.selectedMunicipio.set(agost);
      mockStore.prediction.set({ ...prediction, unidadTemperatura: 'G_FAH', mediaTemperatura: 70 });
      fixture.detectChanges();

      expect(el().querySelector('.temp-unit')?.textContent).toContain('°F');
    });
  });
});
