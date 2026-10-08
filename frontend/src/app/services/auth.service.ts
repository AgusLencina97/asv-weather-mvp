import { Injectable, signal } from '@angular/core';
import { Router } from '@angular/router';

@Injectable({ providedIn: 'root' })
export class AuthService {
  // Estado reactivo para saber si el usuario está logueado
  isAuthenticated = signal<boolean>(this.hasToken());

  constructor(private router: Router) {}

  private hasToken(): boolean {
    return !!localStorage.getItem('authCredentials');
  }

  login(username: string, pass: string): void {
    // Codificamos en Base64 tal como lo exige Basic Auth
    const token = btoa(`${username}:${pass}`);
    localStorage.setItem('authCredentials', token);
    this.isAuthenticated.set(true);
    this.router.navigate(['/weather']);
  }

  logout(): void {
    localStorage.removeItem('authCredentials');
    this.isAuthenticated.set(false);
    this.router.navigate(['/login']);
  }

  getToken(): string | null {
    return localStorage.getItem('authCredentials');
  }
}