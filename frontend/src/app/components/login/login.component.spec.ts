import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpErrorResponse } from '@angular/common/http';
import { Router } from '@angular/router';
import { Subject, of, throwError } from 'rxjs';
import { vi } from 'vitest';
import { LoginComponent } from './login.component';
import { AuthService } from '../../services/auth.service';

describe('LoginComponent', () => {
  let component: LoginComponent;
  let fixture: ComponentFixture<LoginComponent>;
  let mockAuthService: { login: ReturnType<typeof vi.fn> };
  let routerSpy: { navigate: ReturnType<typeof vi.fn> };

  const el = () => fixture.nativeElement as HTMLElement;
  const submitButton = () => el().querySelector('button[type="submit"]') as HTMLButtonElement;
  const fillForm = () => component.loginForm.setValue({ username: 'admin', password: 'admin123' });

  beforeEach(async () => {
    mockAuthService = { login: vi.fn().mockReturnValue(of(undefined)) };
    routerSpy = { navigate: vi.fn() };

    await TestBed.configureTestingModule({
      imports: [LoginComponent],
      providers: [
        { provide: AuthService, useValue: mockAuthService },
        { provide: Router, useValue: routerSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(LoginComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('debería tener el formulario inválido al iniciar (campos vacíos)', () => {
    expect(component.loginForm.valid).toBeFalsy();
  });

  it.each([
    ['usuario vacío', '', '1234'],
    ['contraseña vacía', 'admin', ''],
  ])('debería ser inválido con %s', (_caso, username, password) => {
    component.loginForm.setValue({ username, password });

    expect(component.loginForm.valid).toBe(false);
  });

  it('debería hacer login y navegar a /weather si las credenciales son correctas', () => {
    fillForm();

    component.onSubmit();

    expect(mockAuthService.login).toHaveBeenCalledWith('admin', 'admin123');
    expect(routerSpy.navigate).toHaveBeenCalledWith(['/weather']);
    expect(component.errorMessage()).toBeNull();
  });

  it('no debería llamar a AuthService.login si el formulario es inválido', () => {
    component.onSubmit();

    expect(mockAuthService.login).not.toHaveBeenCalled();
  });

  it('debería mostrar un error y no navegar si las credenciales son incorrectas', () => {
    mockAuthService.login.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 401 })));
    fillForm();

    component.onSubmit();
    fixture.detectChanges();

    expect(routerSpy.navigate).not.toHaveBeenCalled();
    expect(el().querySelector('[role="alert"]')?.textContent).toContain('Usuario o contraseña incorrectos');
  });

  it('debería mostrar un error de conexión si el backend no responde', () => {
    mockAuthService.login.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 0 })));
    fillForm();

    component.onSubmit();

    expect(component.errorMessage()).toContain('No se pudo conectar');
  });

  it('debería deshabilitar el botón y evitar envíos duplicados mientras se procesa el login', () => {
    const pendingLogin = new Subject<void>();
    mockAuthService.login.mockReturnValue(pendingLogin);
    fillForm();

    component.onSubmit();
    component.onSubmit();
    fixture.detectChanges();

    expect(mockAuthService.login).toHaveBeenCalledTimes(1);
    expect(submitButton().disabled).toBe(true);

    pendingLogin.complete();
    fixture.detectChanges();
    expect(component.isSubmitting()).toBe(false);
  });

  it('debería deshabilitar el botón mientras el formulario es inválido y habilitarlo al completarlo', () => {
    expect(submitButton().disabled).toBe(true);

    fillForm();
    fixture.detectChanges();

    expect(submitButton().disabled).toBe(false);
  });

  it('debería enviar el formulario desde el DOM con las credenciales ingresadas', () => {
    const inputs = el().querySelectorAll('input') as NodeListOf<HTMLInputElement>;
    inputs[0].value = 'admin';
    inputs[0].dispatchEvent(new Event('input'));
    inputs[1].value = 'admin123';
    inputs[1].dispatchEvent(new Event('input'));
    fixture.detectChanges();

    el().querySelector('form')!.dispatchEvent(new Event('submit'));

    expect(mockAuthService.login).toHaveBeenCalledWith('admin', 'admin123');
  });
});
