import { inject } from '@angular/core';
import { patchState, signalStore, withMethods, withState } from '@ngrx/signals';
import { rxMethod } from '@ngrx/signals/rxjs-interop';
import { debounceTime, distinctUntilChanged, pipe, switchMap, tap } from 'rxjs';
import { tapResponse } from '@ngrx/operators';
import { WeatherService } from '../services/weather.service';
import { Municipio } from '../interfaces/models/municipio';
import { WeatherPrediction } from '../interfaces/models/weather-prediction';
import { HttpErrorResponse } from '@angular/common/http';

interface WeatherState {
  municipalities: Municipio[];
  selectedMunicipio: Municipio | null;
  prediction: WeatherPrediction | null;
  unit: string;
  isLoading: boolean;
}

const initialState: WeatherState = {
  municipalities: [],
  selectedMunicipio: null,
  prediction: null,
  unit: 'G_CEL',
  isLoading: false,
};

export const WeatherStore = signalStore(
  { providedIn: 'root' },
  withState(initialState),
  withMethods((store, weatherService = inject(WeatherService)) => ({
    
    updateUnit(unit: string) {
      patchState(store, { unit });
      const currentMunicipio = store.selectedMunicipio();
      if (currentMunicipio) {
        this.loadPrediction({ id: currentMunicipio.codigo, unit });
      }
    },

    setSelectedMunicipio(municipio: Municipio | null) {
      patchState(store, { selectedMunicipio: municipio });
      if (municipio) {
        this.loadPrediction({ id: municipio.codigo, unit: store.unit() });
      } else {
        patchState(store, { prediction: null });
      }
    },

    searchMunicipalities: rxMethod<string>(
      pipe(
        debounceTime(300),
        distinctUntilChanged(),
        tap(() => patchState(store, { isLoading: true })),
        switchMap((prefix) => {
          if (!prefix || prefix.length < 2) {
            patchState(store, { municipalities: [], isLoading: false });
            return [];
          }
          return weatherService.searchMunicipalities(prefix).pipe(
            // Tipado explícito para evitar el error 'implicit any'
            tapResponse<Municipio[], HttpErrorResponse>({
              next: (municipalities) => patchState(store, { municipalities, isLoading: false }),
              error: (err) => {
                console.error(err);
                patchState(store, { municipalities: [], isLoading: false });
              },
            })
          );
        })
      )
    ),

    loadPrediction: rxMethod<{ id: string; unit: string }>(
      pipe(
        tap(() => patchState(store, { isLoading: true })),
        switchMap(({ id, unit }) =>
          weatherService.getPrediction(id, unit).pipe(
            tapResponse<WeatherPrediction, HttpErrorResponse>({
              next: (prediction) => patchState(store, { prediction, isLoading: false }),
              error: (err) => {
                console.error(err);
                patchState(store, { prediction: null, isLoading: false });
              },
            })
          )
        )
      )
    ),
  }))
);