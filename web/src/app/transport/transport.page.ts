import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatIconModule } from '@angular/material/icon';
import { extractErrorMessage } from '../core/http-error.util';
import { Student } from '../student/student.model';
import { StudentService } from '../student/student.service';
import { BusRoute, BusStop, TransportService } from './transport.service';

@Component({
  selector: 'app-transport-page',
  imports: [
    FormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatIconModule,
  ],
  template: `
    <div class="page-header">
      <h1>
        <mat-icon class="page-icon" aria-hidden="true">directions_bus</mat-icon>Transport scolaire
      </h1>
    </div>
    <form class="stack-form" (submit)="createRoute($event)">
      <mat-form-field appearance="outline"
        ><mat-label>Nouvelle ligne</mat-label
        ><input matInput [(ngModel)]="routeLabel" name="routeLabel"
      /></mat-form-field>
      <button mat-flat-button type="submit">Créer la ligne</button>
    </form>
    <div class="filters-row">
      <mat-form-field appearance="outline">
        <mat-label>Ligne</mat-label>
        <mat-select [(ngModel)]="routeId" (ngModelChange)="loadStops()">
          @for (route of routes(); track route.id) {
            <mat-option [value]="route.id">{{ route.label }}</mat-option>
          }
        </mat-select>
      </mat-form-field>
    </div>
    <form class="stack-form" (submit)="addStop($event)">
      <mat-form-field appearance="outline"
        ><mat-label>Arrêt</mat-label><input matInput [(ngModel)]="stopName" name="stopName"
      /></mat-form-field>
      <button mat-stroked-button type="submit">Ajouter un arrêt</button>
    </form>
    <form class="stack-form" (submit)="assign($event)">
      <mat-form-field appearance="outline">
        <mat-label>Élève</mat-label>
        <mat-select [(ngModel)]="studentId" name="studentId">
          @for (student of students(); track student.id) {
            <mat-option [value]="student.id"
              >{{ student.lastName }} {{ student.firstName }}</mat-option
            >
          }
        </mat-select>
      </mat-form-field>
      <mat-form-field appearance="outline">
        <mat-label>Arrêt</mat-label>
        <mat-select [(ngModel)]="stopId" name="stopId">
          @for (stop of stops(); track stop.id) {
            <mat-option [value]="stop.id">{{ stop.name }}</mat-option>
          }
        </mat-select>
      </mat-form-field>
      <button mat-flat-button type="submit">Affecter</button>
    </form>
    @if (errorMessage()) {
      <p class="flash-error">{{ errorMessage() }}</p>
    }
  `,
})
export class TransportPage {
  private readonly transportService = inject(TransportService);
  private readonly studentService = inject(StudentService);

  protected readonly routes = signal<BusRoute[]>([]);
  protected readonly stops = signal<BusStop[]>([]);
  protected readonly students = signal<Student[]>([]);
  protected readonly errorMessage = signal<string | null>(null);
  protected routeLabel = '';
  protected routeId: number | null = null;
  protected stopName = '';
  protected studentId: number | null = null;
  protected stopId: number | null = null;

  constructor() {
    void this.refresh();
    void this.studentService.list().then((students) => this.students.set(students));
  }

  async refresh(): Promise<void> {
    this.routes.set(await this.transportService.listRoutes());
  }

  async createRoute(event: Event): Promise<void> {
    event.preventDefault();
    try {
      await this.transportService.createRoute(this.routeLabel);
      this.routeLabel = '';
      await this.refresh();
    } catch (error) {
      this.errorMessage.set(extractErrorMessage(error));
    }
  }

  async loadStops(): Promise<void> {
    if (this.routeId === null) {
      return;
    }
    this.stops.set(await this.transportService.listStops(this.routeId));
  }

  async addStop(event: Event): Promise<void> {
    event.preventDefault();
    if (this.routeId === null) {
      return;
    }
    await this.transportService.addStop(this.routeId, this.stopName, this.stops().length);
    this.stopName = '';
    await this.loadStops();
  }

  async assign(event: Event): Promise<void> {
    event.preventDefault();
    if (this.studentId === null || this.routeId === null || this.stopId === null) {
      return;
    }
    try {
      await this.transportService.assign(this.studentId, this.routeId, this.stopId);
    } catch (error) {
      this.errorMessage.set(extractErrorMessage(error));
    }
  }
}
