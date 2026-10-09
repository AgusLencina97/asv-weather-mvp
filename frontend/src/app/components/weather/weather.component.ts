import { Component, inject, OnInit } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
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

@Component({
  selector: 'app-weather',
  standalone: true,
  imports: [
    CommonModule, ReactiveFormsModule, MatFormFieldModule, MatInputModule,
    MatAutocompleteModule, MatSelectModule, MatCardModule, MatIconModule, MatProgressSpinnerModule, MatButtonModule,
    MatTooltipModule, DatePipe
  ],
  templateUrl: './weather.component.html',
  styleUrls: ['./weather.component.scss']
})
export class WeatherComponent implements OnInit {
  store = inject(WeatherStore);
  private readonly authService = inject(AuthService);
  searchControl = new FormControl('');
  today = new Date();

  ngOnInit() {
    this.searchControl.valueChanges.subscribe(value => {
      if (typeof value === 'string') {
        this.store.searchMunicipalities(value);
      }
    });
  }

  displayFn(municipio: Municipio): string {
    return municipio && municipio.nombre ? municipio.nombre : '';
  }

  onMunicipioSelected(event: MatAutocompleteSelectedEvent) {
    this.store.setSelectedMunicipio(event.option.value);
  }

  logout() {
    this.authService.logout();
  }

  onUnitChange(unit: string) {
    this.store.updateUnit(unit);
  }
}