import { HttpErrorResponse } from '@angular/common/http';
import { toUserMessage } from './http-error';

describe('toUserMessage', () => {
  const FALLBACK = 'Mensaje por defecto';

  it('debería usar el "detail" del ProblemDetail del backend', () => {
    const error = new HttpErrorResponse({ status: 404, error: { title: 'Recurso no encontrado', detail: 'No existe' } });

    expect(toUserMessage(error, FALLBACK)).toBe('No existe');
  });

  it('debería indicar un problema de conexión si el backend no responde (status 0)', () => {
    expect(toUserMessage(new HttpErrorResponse({ status: 0 }), FALLBACK)).toContain('No se pudo conectar');
  });

  it.each([
    ['un error HTTP sin detail', new HttpErrorResponse({ status: 500, error: 'texto plano' })],
    ['un error que no es HTTP', new Error('boom')],
  ])('debería usar el mensaje por defecto con %s', (_caso, error) => {
    expect(toUserMessage(error, FALLBACK)).toBe(FALLBACK);
  });
});
