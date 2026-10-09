import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Router } from '@angular/router';
import { vi } from 'vitest';
import { AuthService } from './auth.service';
import { LOGIN_URL } from '../config/api.config';

describe('AuthService', () => {
  let service: AuthService;
  let httpMock: HttpTestingController;
  let routerSpy: { navigate: ReturnType<typeof vi.fn> };

  const storeSession = (token: string, expiresAt: number) => {
    sessionStorage.setItem('accessToken', token);
    sessionStorage.setItem('accessTokenExpiresAt', String(expiresAt));
  };

  beforeEach(() => {
    sessionStorage.clear();
    routerSpy = { navigate: vi.fn() };

    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: Router, useValue: routerSpy },
      ],
    });
    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
    sessionStorage.clear();
    vi.useRealTimers();
  });

  describe('estado inicial', () => {
    it('debería no estar autenticado si no hay token guardado', () => {
      expect(service.isAuthenticated()).toBe(false);
    });

    it('debería estar autenticado si hay un token vigente', () => {
      storeSession('jwt-vigente', Date.now() + 60_000);

      expect(service.isAuthenticated()).toBe(true);
    });

    it('debería descartar un token caducado', () => {
      storeSession('jwt-caducado', Date.now() - 1);

      expect(service.isAuthenticated()).toBe(false);
      expect(sessionStorage.getItem('accessToken')).toBeNull();
    });
  });

  describe('login', () => {
    it('debería enviar las credenciales al backend y guardar el token recibido', () => {
      let completed = false;

      service.login('admin', 'admin123').subscribe(() => (completed = true));

      const req = httpMock.expectOne(LOGIN_URL);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual({ username: 'admin', password: 'admin123' });
      req.flush({ accessToken: 'jwt-token', tokenType: 'Bearer', expiresIn: 3600 });

      expect(completed).toBe(true);
      expect(service.getToken()).toBe('jwt-token');
      expect(service.isAuthenticated()).toBe(true);
    });

    it('debería guardar la fecha de caducidad a partir de expiresIn', () => {
      vi.useFakeTimers();
      vi.setSystemTime(new Date('2026-10-09T10:00:00Z'));

      service.login('admin', 'admin123').subscribe();
      httpMock.expectOne(LOGIN_URL).flush({ accessToken: 'jwt-token', tokenType: 'Bearer', expiresIn: 3600 });

      vi.setSystemTime(new Date('2026-10-09T10:59:59Z'));
      expect(service.getToken()).toBe('jwt-token');
      vi.setSystemTime(new Date('2026-10-09T11:00:00Z'));
      expect(service.getToken()).toBeNull();
    });

    it('no debería guardar nada si el backend rechaza las credenciales', () => {
      let error: unknown;

      service.login('admin', 'mala').subscribe({ error: (e) => (error = e) });
      httpMock.expectOne(LOGIN_URL).flush(null, { status: 401, statusText: 'Unauthorized' });

      expect(error).toBeTruthy();
      expect(service.isAuthenticated()).toBe(false);
    });
  });

  describe('logout', () => {
    it('debería limpiar el token y redirigir a /login', () => {
      storeSession('jwt-vigente', Date.now() + 60_000);

      service.logout();

      expect(sessionStorage.getItem('accessToken')).toBeNull();
      expect(service.isAuthenticated()).toBe(false);
      expect(routerSpy.navigate).toHaveBeenCalledWith(['/login']);
    });
  });
});
