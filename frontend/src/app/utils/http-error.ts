import { HttpErrorResponse } from '@angular/common/http';

/**
 * Traduce un error HTTP a un mensaje para el usuario. El backend responde con ProblemDetail (RFC 9457),
 * cuyo campo "detail" ya es un texto apto para mostrar.
 */
export function toUserMessage(error: unknown, fallback: string): string {
  if (error instanceof HttpErrorResponse) {
    if (error.status === 0) {
      return 'No se pudo conectar con el servidor. Comprueba tu conexión.';
    }
    const detail = (error.error as { detail?: unknown } | null)?.detail;
    if (typeof detail === 'string' && detail.trim()) {
      return detail;
    }
  }
  return fallback;
}
