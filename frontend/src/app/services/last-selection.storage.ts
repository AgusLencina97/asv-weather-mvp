import { Injectable } from '@angular/core';
import { Municipio } from '../interfaces/models/municipio';
import { TemperatureUnit } from '../interfaces/models/temperature-unit';

export interface LastSelection {
  municipio: Municipio;
  unit: TemperatureUnit | null;
}

const STORAGE_KEY = 'asv-weather.last-selection';

@Injectable({ providedIn: 'root' })
export class LastSelectionStorage {
  load(): LastSelection | null {
    try {
      const raw = localStorage.getItem(STORAGE_KEY);
      const parsed: unknown = raw ? JSON.parse(raw) : null;
      return isLastSelection(parsed) ? parsed : null;
    } catch {
      // JSON corrupto o storage bloqueado: se empieza sin selección
      return null;
    }
  }

  save(selection: LastSelection): void {
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(selection));
    } catch {
      // Recordar la selección es opcional: si el storage falla, la app sigue funcionando
    }
  }
}

function isLastSelection(value: unknown): value is LastSelection {
  const candidate = value as Partial<LastSelection> | null;
  return (
    typeof candidate?.municipio?.codigo === 'string' &&
    typeof candidate.municipio.nombre === 'string' &&
    (candidate.unit === null || candidate.unit === 'G_CEL' || candidate.unit === 'G_FAH')
  );
}
