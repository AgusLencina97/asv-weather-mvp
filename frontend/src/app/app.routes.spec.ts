import { routes } from './app.routes';
import { authGuard } from './guards/auth.guard';
import { LoginComponent } from './components/login/login.component';
import { WeatherComponent } from './components/weather/weather.component';

describe('routes', () => {
  const find = (path: string) => routes.find((r) => r.path === path)!;

  it('debería cargar LoginComponent de forma diferida en /login sin guard', async () => {
    const route = find('login');

    expect(await (route.loadComponent as () => Promise<unknown>)()).toBe(LoginComponent);
    expect(route.canActivate).toBeUndefined();
  });

  it('debería cargar WeatherComponent en /weather protegido por authGuard', async () => {
    const route = find('weather');

    expect(await (route.loadComponent as () => Promise<unknown>)()).toBe(WeatherComponent);
    expect(route.canActivate).toEqual([authGuard]);
  });

  it('debería redirigir la ruta vacía y las desconocidas a /weather', () => {
    expect(find('').redirectTo).toBe('/weather');
    expect(find('**').redirectTo).toBe('/weather');
  });
});
