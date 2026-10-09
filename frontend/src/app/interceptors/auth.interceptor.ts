import { HttpErrorResponse, HttpInterceptorFn, HttpStatusCode } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';
import { API_BASE_URL, LOGIN_URL } from '../config/api.config';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  // El token solo se adjunta a peticiones a nuestra API, nunca a dominios de terceros
  if (!req.url.startsWith(API_BASE_URL)) {
    return next(req);
  }

  const authService = inject(AuthService);
  const token = authService.getToken();
  const authReq = token ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } }) : req;

  return next(authReq).pipe(
    catchError((error: unknown) => {
      // Un 401 fuera del login significa token caducado o inválido: se cierra la sesión
      if (error instanceof HttpErrorResponse && error.status === HttpStatusCode.Unauthorized && req.url !== LOGIN_URL) {
        authService.logout();
      }
      return throwError(() => error);
    }),
  );
};
