import { TestBed } from '@angular/core/testing';
import { AuthService } from './auth.service';
import { Router } from '@angular/router';
import { vi } from 'vitest';

describe('AuthService', () => {
  let routerSpy: { navigate: ReturnType<typeof vi.fn> };

  const createService = () => TestBed.inject(AuthService);

  beforeEach(() => {
    localStorage.clear();
    // Espiamos el enrutador para verificar las redirecciones sin ejecutarlas realmente
    routerSpy = { navigate: vi.fn() };

    TestBed.configureTestingModule({
      providers: [AuthService, { provide: Router, useValue: routerSpy }],
    });
  });

  afterEach(() => localStorage.clear());

  describe('estado inicial', () => {
    it('debería iniciar como no autenticado si no hay token guardado', () => {
      expect(createService().isAuthenticated()).toBe(false);
    });

    it('debería iniciar como autenticado si ya hay un token guardado', () => {
      localStorage.setItem('authCredentials', 'dummyToken');

      expect(createService().isAuthenticated()).toBe(true);
    });
  });

  describe('login', () => {
    it('debería guardar el token, actualizar el signal y redirigir a /weather al hacer login', () => {
      const service = createService();

      service.login('admin', 'asv2026');

      expect(localStorage.getItem('authCredentials')).toBeTruthy();
      expect(service.isAuthenticated()).toBe(true);
      expect(routerSpy.navigate).toHaveBeenCalledWith(['/weather']);
    });

    it('debería codificar las credenciales en Base64 (usuario:contraseña)', () => {
      const service = createService();

      service.login('admin', 'asv2026');

      expect(localStorage.getItem('authCredentials')).toBe(btoa('admin:asv2026'));
    });
  });

  describe('logout', () => {
    it('debería limpiar el token, actualizar el signal y redirigir a /login al hacer logout', () => {
      localStorage.setItem('authCredentials', 'dummyToken');
      const service = createService();
      expect(service.isAuthenticated()).toBe(true);

      service.logout();

      expect(localStorage.getItem('authCredentials')).toBeNull();
      expect(service.isAuthenticated()).toBe(false);
      expect(routerSpy.navigate).toHaveBeenCalledWith(['/login']);
    });
  });

  describe('getToken', () => {
    it('debería devolver null si no hay sesión', () => {
      expect(createService().getToken()).toBeNull();
    });

    it('debería devolver el token guardado tras el login', () => {
      const service = createService();

      service.login('user', 'pass');

      expect(service.getToken()).toBe(btoa('user:pass'));
    });
  });
});
