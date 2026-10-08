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
import { WeatherStore } from '../../stores/weather.store';
import { Municipio } from '../../interfaces/models/municipio';

@Component({
  selector: 'app-weather',
  standalone: true,
  imports: [
    CommonModule, ReactiveFormsModule, MatFormFieldModule, MatInputModule,
    MatAutocompleteModule, MatSelectModule, MatCardModule, MatIconModule, MatProgressSpinnerModule, DatePipe
  ],
  templateUrl: './weather.component.html',
  styleUrls: ['./weather.component.scss']
})
export class WeatherComponent implements OnInit {
  store = inject(WeatherStore);
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

  onUnitChange(unit: string) {
    this.store.updateUnit(unit);
  }
}