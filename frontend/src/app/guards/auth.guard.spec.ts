import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, Router, RouterStateSnapshot } from '@angular/router';
import { signal } from '@angular/core';
import { vi } from 'vitest';
import { authGuard } from './auth.guard';
import { AuthService } from '../services/auth.service';

describe('authGuard', () => {
  const isAuthenticated = signal(false);
  const routerSpy = { navigate: vi.fn() };

  const runGuard = () =>
    TestBed.runInInjectionContext(() =>
      authGuard({} as ActivatedRouteSnapshot, {} as RouterStateSnapshot),
    );

  beforeEach(() => {
    routerSpy.navigate.mockClear();
    TestBed.configureTestingModule({
      providers: [
        { provide: AuthService, useValue: { isAuthenticated } },
        { provide: Router, useValue: routerSpy },
      ],
    });
  });

  it('debería permitir el acceso si el usuario está autenticado', () => {
    isAuthenticated.set(true);

    expect(runGuard()).toBe(true);
    expect(routerSpy.navigate).not.toHaveBeenCalled();
  });

  it('debería bloquear el acceso y redirigir a /login si no está autenticado', () => {
    isAuthenticated.set(false);

    expect(runGuard()).toBe(false);
    expect(routerSpy.navigate).toHaveBeenCalledWith(['/login']);
  });
});
