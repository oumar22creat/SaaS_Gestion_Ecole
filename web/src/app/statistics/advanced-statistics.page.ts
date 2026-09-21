import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatTableModule } from '@angular/material/table';
import { MatIconModule } from '@angular/material/icon';
import { SchoolClass } from '../schoolclass/school-class.model';
import { SchoolClassService } from '../schoolclass/school-class.service';
import { AdvancedStatisticsService, EvolutionPoint } from './advanced-statistics.service';

@Component({
  selector: 'app-advanced-statistics-page',
  imports: [FormsModule, MatFormFieldModule, MatSelectModule, MatButtonModule, MatTableModule, MatIconModule],
  template: `
    <div class="page-header">
      <h1><mat-icon class="page-icon" aria-hidden="true">monitoring</mat-icon>Évolution des résultats</h1>
      <button mat-stroked-button (click)="csv()" [disabled]="schoolClassId === null">Export CSV</button>
    </div>
    <div class="filters-row">
      <mat-form-field appearance="outline">
        <mat-label>Classe</mat-label>
        <mat-select [(ngModel)]="schoolClassId" (ngModelChange)="load()">
          @for (schoolClass of classes(); track schoolClass.id) {
            <mat-option [value]="schoolClass.id">{{ schoolClass.name }}</mat-option>
          }
        </mat-select>
      </mat-form-field>
    </div>
    <div class="table-scroll">
<table mat-table [dataSource]="points()" class="data-table">
      <ng-container matColumnDef="period">
        <th mat-header-cell *matHeaderCellDef>Mois</th>
        <td mat-cell *matCellDef="let point">{{ point.period }}</td>
      </ng-container>
      <ng-container matColumnDef="average">
        <th mat-header-cell *matHeaderCellDef>Moyenne /20</th>
        <td mat-cell *matCellDef="let point">{{ point.average }}</td>
      </ng-container>
      <ng-container matColumnDef="count">
        <th mat-header-cell *matHeaderCellDef>Notes</th>
        <td mat-cell *matCellDef="let point">{{ point.gradeCount }}</td>
      </ng-container>
      <tr mat-header-row *matHeaderRowDef="columns"></tr>
      <tr mat-row *matRowDef="let row; columns: columns"></tr>
    </table>
    </div>
  `,
})
export class AdvancedStatisticsPage {
  private readonly statisticsService = inject(AdvancedStatisticsService);
  private readonly schoolClassService = inject(SchoolClassService);

  protected readonly classes = signal<SchoolClass[]>([]);
  protected readonly points = signal<EvolutionPoint[]>([]);
  protected readonly columns = ['period', 'average', 'count'];
  protected schoolClassId: number | null = null;

  constructor() {
    void this.schoolClassService.list().then((classes) => this.classes.set(classes));
  }

  async load(): Promise<void> {
    if (this.schoolClassId === null) {
      return;
    }
    const result = await this.statisticsService.resultsEvolution(this.schoolClassId);
    this.points.set(result.points);
  }

  async csv(): Promise<void> {
    if (this.schoolClassId === null) {
      return;
    }
    await this.statisticsService.downloadCsv(this.schoolClassId);
  }
}
