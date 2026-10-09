import { Component, computed, inject } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { filter } from 'rxjs';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatAutocompleteModule, MatAutocompleteSelectedEvent } from '@angular/material/autocomplete';
import { MatSelectModule } from '@angular/material/select';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatButtonModule } from '@angular/material/button';
import { MatTooltipModule } from '@angular/material/tooltip';
import { WeatherStore } from '../../stores/weather.store';
import { AuthService } from '../../services/auth.service';
import { Municipio } from '../../interfaces/models/municipio';
import { TemperatureUnit } from '../../interfaces/models/temperature-unit';
import { weatherIconFor } from '../../utils/weather-icon';

@Component({
  selector: 'app-weather',
  imports: [
    ReactiveFormsModule, DatePipe, DecimalPipe, MatFormFieldModule, MatInputModule, MatAutocompleteModule,
    MatSelectModule, MatCardModule, MatIconModule, MatProgressSpinnerModule, MatButtonModule, MatTooltipModule
  ],
  templateUrl: './weather.component.html',
  styleUrls: ['./weather.component.scss']
})
export class WeatherComponent {
  readonly store = inject(WeatherStore);
  private readonly authService = inject(AuthService);

  // Si se restauró la última selección, el campo arranca mostrando ese municipio
  readonly searchControl = new FormControl<string | Municipio>(this.store.selectedMunicipio() ?? '', { nonNullable: true });

  readonly weatherIcon = computed(() => {
    const prediction = this.store.prediction();
    return prediction ? weatherIconFor(prediction) : null;
  });

  constructor() {
    // Al elegir una opción el valor es un Municipio: solo el texto escrito dispara búsquedas
    this.store.searchMunicipalities(
      this.searchControl.valueChanges.pipe(filter((value): value is string => typeof value === 'string'))
    );
  }

  displayMunicipio(value: string | Municipio | null): string {
    return typeof value === 'string' ? value : value?.nombre ?? '';
  }

  onMunicipioSelected(event: MatAutocompleteSelectedEvent): void {
    this.store.setSelectedMunicipio(event.option.value as Municipio);
  }

  onUnitChange(unit: TemperatureUnit): void {
    this.store.updateUnit(unit);
  }

  logout(): void {
    this.authService.logout();
  }
}