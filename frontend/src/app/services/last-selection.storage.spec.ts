import { TestBed } from '@angular/core/testing';
import { vi } from 'vitest';
import { LastSelectionStorage } from './last-selection.storage';

describe('LastSelectionStorage', () => {
  const KEY = 'asv-weather.last-selection';
  let storage: LastSelectionStorage;

  beforeEach(() => {
    localStorage.clear();
    storage = TestBed.inject(LastSelectionStorage);
  });

  afterEach(() => {
    localStorage.clear();
    vi.restoreAllMocks();
  });

  it('debería devolver null si no hay nada guardado', () => {
    expect(storage.load()).toBeNull();
  });

  it('debería guardar y recuperar el municipio y la unidad', () => {
    const selection = { municipio: { codigo: '46190', nombre: 'Paterna' }, unit: 'G_FAH' as const };

    storage.save(selection);

    expect(storage.load()).toEqual(selection);
  });

  it.each([
    ['JSON corrupto', '{no es json'],
    ['estructura inesperada', JSON.stringify({ foo: 'bar' })],
    ['unidad no válida', JSON.stringify({ municipio: { codigo: '1', nombre: 'X' }, unit: 'KELVIN' })],
  ])('debería ignorar el valor guardado si es %s', (_caso, raw) => {
    localStorage.setItem(KEY, raw);

    expect(storage.load()).toBeNull();
  });

  it('no debería romper la app si el storage no está disponible', () => {
    vi.spyOn(Storage.prototype, 'setItem').mockImplementation(() => {
      throw new Error('QuotaExceededError');
    });

    expect(() => storage.save({ municipio: { codigo: '1', nombre: 'X' }, unit: null })).not.toThrow();
  });
});
