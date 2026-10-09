import { Component, inject, signal } from '@angular/core';
import { HttpErrorResponse, HttpStatusCode } from '@angular/common/http';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { finalize } from 'rxjs';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-login',
  imports: [ReactiveFormsModule, MatCardModule, MatFormFieldModule, MatInputModule, MatButtonModule],
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.scss']
})
export class LoginComponent {
  private readonly fb = inject(NonNullableFormBuilder);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  readonly isSubmitting = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly loginForm = this.fb.group({
    username: ['', Validators.required],
    password: ['', Validators.required]
  });

  onSubmit(): void {
    if (this.loginForm.invalid || this.isSubmitting()) {
      return;
    }
    const { username, password } = this.loginForm.getRawValue();
    this.isSubmitting.set(true);
    this.errorMessage.set(null);

    this.authService.login(username, password)
      .pipe(finalize(() => this.isSubmitting.set(false)))
      .subscribe({
        next: () => this.router.navigate(['/weather']),
        error: (error: HttpErrorResponse) => this.errorMessage.set(
          error.status === HttpStatusCode.Unauthorized
            ? 'Usuario o contraseña incorrectos'
            : 'No se pudo conectar con el servidor. Inténtalo de nuevo.'
        )
      });
  }
}
