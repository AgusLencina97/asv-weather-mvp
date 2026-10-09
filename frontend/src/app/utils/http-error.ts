import { HttpErrorResponse } from '@angular/common/http';

// El backend responde con ProblemDetail (RFC 9457): su "detail" ya es apto para mostrar
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
