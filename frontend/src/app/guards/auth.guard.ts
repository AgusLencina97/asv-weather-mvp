import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

export const authGuard: CanActivateFn = () => {
  const authService = inject(AuthService);

  // Devolver un UrlTree hace que el router redirija sin una navegación extra
  return authService.isAuthenticated() ? true : inject(Router).createUrlTree(['/login']);
};
