import { ComponentFixture, TestBed } from '@angular/core/testing';
import { LoginComponent } from './login.component';
import { AuthService } from '../../services/auth.service';
import { vi } from 'vitest';

describe('LoginComponent', () => {
  let component: LoginComponent;
  let fixture: ComponentFixture<LoginComponent>;
  let mockAuthService: { login: ReturnType<typeof vi.fn> };

  const submitButton = () =>
    fixture.nativeElement.querySelector('button[type="submit"]') as HTMLButtonElement;

  beforeEach(async () => {
    mockAuthService = { login: vi.fn() };

    await TestBed.configureTestingModule({
      imports: [LoginComponent],
      providers: [{ provide: AuthService, useValue: mockAuthService }],
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

  it('debería llamar a AuthService.login si el formulario es válido y se envía', () => {
    component.loginForm.controls['username'].setValue('admin');
    component.loginForm.controls['password'].setValue('1234');

    expect(component.loginForm.valid).toBeTruthy();

    component.onSubmit();

    expect(mockAuthService.login).toHaveBeenCalledTimes(1);
    expect(mockAuthService.login).toHaveBeenCalledWith('admin', '1234');
  });

  it('no debería llamar a AuthService.login si el formulario es inválido', () => {
    component.onSubmit();

    expect(mockAuthService.login).not.toHaveBeenCalled();
  });

  it('debería deshabilitar el botón mientras el formulario es inválido y habilitarlo al completarlo', () => {
    expect(submitButton().disabled).toBe(true);

    component.loginForm.setValue({ username: 'admin', password: '1234' });
    fixture.detectChanges();

    expect(submitButton().disabled).toBe(false);
  });

  it('debería enviar el formulario desde el DOM con las credenciales ingresadas', () => {
    const inputs = fixture.nativeElement.querySelectorAll('input') as NodeListOf<HTMLInputElement>;
    inputs[0].value = 'admin';
    inputs[0].dispatchEvent(new Event('input'));
    inputs[1].value = 'asv2026';
    inputs[1].dispatchEvent(new Event('input'));
    fixture.detectChanges();

    fixture.nativeElement.querySelector('form').dispatchEvent(new Event('submit'));

    expect(mockAuthService.login).toHaveBeenCalledWith('admin', 'asv2026');
  });
});
