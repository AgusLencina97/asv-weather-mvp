import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, map, tap } from 'rxjs';
import { LOGIN_URL } from '../config/api.config';
import { TokenResponse } from '../interfaces/models/token-response';

const TOKEN_KEY = 'accessToken';
const EXPIRES_AT_KEY = 'accessTokenExpiresAt';

/**
 * Gestiona el JWT de acceso. Se guarda en sessionStorage: sobrevive a recargar la página
 * pero se borra al cerrar la pestaña. En producción, la opción más robusta frente a XSS sería
 * una cookie HttpOnly emitida por el backend.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);

  login(username: string, password: string): Observable<void> {
    return this.http.post<TokenResponse>(LOGIN_URL, { username, password }).pipe(
      tap((response) => this.storeToken(response)),
      map(() => undefined),
    );
  }

  logout(): void {
    this.clearToken();
    this.router.navigate(['/login']);
  }

  isAuthenticated(): boolean {
    return this.getToken() !== null;
  }

  /** Devuelve el token solo si sigue vigente; si ya caducó lo descarta. */
  getToken(): string | null {
    const token = sessionStorage.getItem(TOKEN_KEY);
    const expiresAt = Number(sessionStorage.getItem(EXPIRES_AT_KEY));
    if (!token || !expiresAt || Date.now() >= expiresAt) {
      this.clearToken();
      return null;
    }
    return token;
  }

  private storeToken({ accessToken, expiresIn }: TokenResponse): void {
    sessionStorage.setItem(TOKEN_KEY, accessToken);
    sessionStorage.setItem(EXPIRES_AT_KEY, String(Date.now() + expiresIn * 1000));
  }

  private clearToken(): void {
    sessionStorage.removeItem(TOKEN_KEY);
    sessionStorage.removeItem(EXPIRES_AT_KEY);
  }
}
