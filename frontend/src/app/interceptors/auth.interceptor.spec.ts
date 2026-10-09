import { TestBed } from '@angular/core/testing';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { vi } from 'vitest';
import { authInterceptor } from './auth.interceptor';
import { AuthService } from '../services/auth.service';
import { LOGIN_URL } from '../config/api.config';

describe('authInterceptor', () => {
  const API_URL = '/api/v1/weather/municipalities';
  let http: HttpClient;
  let httpMock: HttpTestingController;
  let token: string | null;
  let mockAuthService: { getToken: () => string | null; logout: ReturnType<typeof vi.fn> };

  beforeEach(() => {
    token = null;
    mockAuthService = { getToken: () => token, logout: vi.fn() };
    TestBed.configureTestingModule({
      providers: [
        { provide: AuthService, useValue: mockAuthService },
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    http = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('debería añadir la cabecera Authorization Bearer cuando hay token', () => {
    token = 'jwt-token';

    http.get(API_URL).subscribe();

    const req = httpMock.expectOne(API_URL);
    expect(req.request.headers.get('Authorization')).toBe('Bearer jwt-token');
    req.flush([]);
  });

  it('no debería modificar la petición cuando no hay token', () => {
    http.get(API_URL).subscribe();

    const req = httpMock.expectOne(API_URL);
    expect(req.request.headers.has('Authorization')).toBe(false);
    req.flush([]);
  });

  it('no debería enviar el token a dominios externos', () => {
    token = 'jwt-token';

    http.get('https://otro-dominio.example/datos').subscribe();

    const req = httpMock.expectOne('https://otro-dominio.example/datos');
    expect(req.request.headers.has('Authorization')).toBe(false);
    req.flush({});
  });

  it('debería cerrar la sesión cuando la API responde 401', () => {
    token = 'jwt-caducado';
    let error: unknown;

    http.get(API_URL).subscribe({ error: (e) => (error = e) });
    httpMock.expectOne(API_URL).flush(null, { status: 401, statusText: 'Unauthorized' });

    expect(mockAuthService.logout).toHaveBeenCalledTimes(1);
    expect(error).toBeTruthy();
  });

  it('no debería cerrar la sesión por un 401 del propio login (credenciales incorrectas)', () => {
    http.post(LOGIN_URL, {}).subscribe({ error: () => undefined });
    httpMock.expectOne(LOGIN_URL).flush(null, { status: 401, statusText: 'Unauthorized' });

    expect(mockAuthService.logout).not.toHaveBeenCalled();
  });

  it('no debería cerrar la sesión por otros errores', () => {
    token = 'jwt-token';

    http.get(API_URL).subscribe({ error: () => undefined });
    httpMock.expectOne(API_URL).flush(null, { status: 502, statusText: 'Bad Gateway' });

    expect(mockAuthService.logout).not.toHaveBeenCalled();
  });
});
