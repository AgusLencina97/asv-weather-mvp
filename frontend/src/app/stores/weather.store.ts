import { computed, inject } from '@angular/core';
import { patchState, signalStore, withComputed, withHooks, withMethods, withState } from '@ngrx/signals';
import { rxMethod } from '@ngrx/signals/rxjs-interop';
import { EMPTY, debounceTime, distinctUntilChanged, map, pipe, switchMap, tap } from 'rxjs';
import { tapResponse } from '@ngrx/operators';
import { WeatherService } from '../services/weather.service';
import { LastSelectionStorage } from '../services/last-selection.storage';
import { Municipio } from '../interfaces/models/municipio';
import { TemperatureUnit } from '../interfaces/models/temperature-unit';
import { WeatherPrediction } from '../interfaces/models/weather-prediction';
import { toUserMessage } from '../utils/http-error';

export const MIN_SEARCH_LENGTH = 2;

interface WeatherState {
  municipalities: Municipio[];
  /** Prefijo de la última búsqueda completada (null si no se ha buscado) */
  searchedPrefix: string | null;
  isSearching: boolean;
  searchError: string | null;
  selectedMunicipio: Municipio | null;
  /** null = sin elegir: el backend usa grados Celsius por defecto */
  unit: TemperatureUnit | null;
  prediction: WeatherPrediction | null;
  isLoadingPrediction: boolean;
  predictionError: string | null;
}

const initialState: WeatherState = {
  municipalities: [],
  searchedPrefix: null,
  isSearching: false,
  searchError: null,
  selectedMunicipio: null,
  unit: null,
  prediction: null,
  isLoadingPrediction: false,
  predictionError: null,
};

export const WeatherStore = signalStore(
  { providedIn: 'root' },
  withState(initialState),
  withComputed(({ searchedPrefix, municipalities, isSearching, searchError }) => ({
    noResults: computed(
      () => searchedPrefix() !== null && !isSearching() && !searchError() && municipalities().length === 0,
    ),
  })),
  withMethods((store, weatherService = inject(WeatherService), lastSelection = inject(LastSelectionStorage)) => {
    // switchMap cancela la petición anterior si el usuario cambia de municipio o unidad antes de que responda
    const loadPrediction = rxMethod<{ municipio: Municipio; unit: TemperatureUnit | null }>(
      pipe(
        tap(() => patchState(store, { isLoadingPrediction: true, predictionError: null })),
        switchMap(({ municipio, unit }) =>
          weatherService.getPrediction(municipio.codigo, unit).pipe(
            tapResponse({
              next: (prediction: WeatherPrediction) => patchState(store, { prediction, isLoadingPrediction: false }),
              error: (error: unknown) =>
                patchState(store, {
                  prediction: null,
                  isLoadingPrediction: false,
                  predictionError: toUserMessage(error, 'No se pudo obtener la predicción. Inténtalo de nuevo.'),
                }),
            }),
          ),
        ),
      ),
    );

    const refreshPrediction = (): void => {
      const municipio = store.selectedMunicipio();
      if (municipio) {
        const unit = store.unit();
        lastSelection.save({ municipio, unit });
        loadPrediction({ municipio, unit });
      }
    };

    return {
      searchMunicipalities: rxMethod<string>(
        pipe(
          debounceTime(300),
          map((prefix) => prefix.trim()),
          distinctUntilChanged(),
          switchMap((prefix) => {
            if (prefix.length < MIN_SEARCH_LENGTH) {
              patchState(store, { municipalities: [], searchedPrefix: null, isSearching: false, searchError: null });
              return EMPTY;
            }
            patchState(store, { isSearching: true, searchError: null });
            return weatherService.searchMunicipalities(prefix).pipe(
              tapResponse({
                next: (municipalities: Municipio[]) =>
                  patchState(store, { municipalities, searchedPrefix: prefix, isSearching: false }),
                error: (error: unknown) =>
                  patchState(store, {
                    municipalities: [],
                    searchedPrefix: prefix,
                    isSearching: false,
                    searchError: toUserMessage(error, 'No se pudieron buscar municipios. Inténtalo de nuevo.'),
                  }),
              }),
            );
          }),
        ),
      ),

      setSelectedMunicipio(municipio: Municipio): void {
        patchState(store, { selectedMunicipio: municipio });
        refreshPrediction();
      },

      updateUnit(unit: TemperatureUnit): void {
        patchState(store, { unit });
        refreshPrediction();
      },
    };
  }),
  withHooks((store, lastSelection = inject(LastSelectionStorage)) => ({
    // Al abrir la app se recupera la última selección y se carga su predicción automáticamente
    onInit(): void {
      const saved = lastSelection.load();
      if (saved) {
        patchState(store, { unit: saved.unit });
        store.setSelectedMunicipio(saved.municipio);
      }
    },
  })),
);