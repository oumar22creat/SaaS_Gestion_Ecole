import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatTableModule } from '@angular/material/table';
import { MatIconModule } from '@angular/material/icon';
import { extractErrorMessage } from '../core/http-error.util';
import { toIsoDate } from '../core/iso-date.util';
import { TeachingScope, TeachingScopeService } from '../core/teaching-scope.service';
import { SchoolClass } from '../schoolclass/school-class.model';
import { SchoolClassService } from '../schoolclass/school-class.service';
import { Student } from '../student/student.model';
import { StudentService } from '../student/student.service';
import { DisciplineService, Incident } from './discipline.service';

@Component({
  selector: 'app-discipline-page',
  imports: [
    FormsModule,
    MatFormFieldModule,
    MatSelectModule,
    MatInputModule,
    MatButtonModule,
    MatTableModule,
    MatIconModule,
  ],
  template: `
    <div class="page-header">
      <h1><mat-icon class="page-icon" aria-hidden="true">gavel</mat-icon>Vie scolaire</h1>
    </div>
    <div class="filters-row">
      <mat-form-field appearance="outline">
        <mat-label>Classe</mat-label>
        <mat-select [(ngModel)]="schoolClassId" (ngModelChange)="onClassChange()">
          @for (schoolClass of classes(); track schoolClass.id) {
            <mat-option [value]="schoolClass.id">{{ schoolClass.name }}</mat-option>
          }
        </mat-select>
      </mat-form-field>
    </div>
    <form class="stack-form" (submit)="create($event)">
      <mat-form-field appearance="outline">
        <mat-label>Élèves concernés</mat-label>
        <mat-select [(ngModel)]="studentIds" name="studentIds" multiple>
          @for (student of studentOptions(); track student.id) {
            <mat-option [value]="student.id"
              >{{ student.lastName }} {{ student.firstName }}</mat-option
            >
          }
        </mat-select>
      </mat-form-field>
      <mat-form-field appearance="outline">
        <mat-label>Gravité</mat-label>
        <mat-select [(ngModel)]="severity" name="severity">
          <mat-option value="MINOR">Mineur</mat-option>
          <mat-option value="MAJOR">Majeur</mat-option>
          <mat-option value="SEVERE">Grave</mat-option>
        </mat-select>
      </mat-form-field>
      <mat-form-field appearance="outline">
        <mat-label>Description</mat-label>
        <textarea matInput [(ngModel)]="description" name="description"></textarea>
      </mat-form-field>
      <button mat-flat-button type="submit"><mat-icon>add</mat-icon> Déclarer un incident</button>
    </form>
    @if (errorMessage()) {
      <p class="flash-error">{{ errorMessage() }}</p>
    }
    <div class="table-scroll">
      <table mat-table [dataSource]="incidents()" class="data-table">
        <ng-container matColumnDef="date">
          <th mat-header-cell *matHeaderCellDef>Date</th>
          <td mat-cell *matCellDef="let incident">{{ incident.occurredAt }}</td>
        </ng-container>
        <ng-container matColumnDef="severity">
          <th mat-header-cell *matHeaderCellDef>Gravité</th>
          <td mat-cell *matCellDef="let incident">{{ incident.severity }}</td>
        </ng-container>
        <ng-container matColumnDef="description">
          <th mat-header-cell *matHeaderCellDef>Faits</th>
          <td mat-cell *matCellDef="let incident">{{ incident.description }}</td>
        </ng-container>
        <tr mat-header-row *matHeaderRowDef="columns"></tr>
        <tr mat-row *matRowDef="let row; columns: columns"></tr>
      </table>
    </div>
  `,
})
export class DisciplinePage {
  private readonly disciplineService = inject(DisciplineService);
  private readonly schoolClassService = inject(SchoolClassService);
  private readonly studentService = inject(StudentService);
  private readonly teachingScopeService = inject(TeachingScopeService);

  /** Périmètre d'affichage (ADR-030) ; fermé tant qu'il n'est pas chargé. */
  protected readonly scope = signal(new TeachingScope(null, true));

  protected readonly classes = signal<SchoolClass[]>([]);
  protected readonly students = signal<Student[]>([]);
  protected readonly incidents = signal<Incident[]>([]);
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly columns = ['date', 'severity', 'description'];
  protected schoolClassId: number | null = null;
  protected studentIds: number[] = [];
  protected severity = 'MINOR';
  protected description = '';

  constructor() {
    void this.schoolClassService.list().then((classes) => this.classes.set(classes));
    void this.studentService.list().then((students) => this.students.set(students));
    void this.teachingScopeService.current().then((scope) => this.scope.set(scope));
  }

  /**
   * Un enseignant ne déclare un incident que pour des élèves de la classe choisie (ADR-030,
   * sinon 403) : on ne lui propose que ceux-là. Les autres rôles gardent la liste complète.
   */
  protected studentOptions(): Student[] {
    if (!this.scope().restricted) {
      return this.students();
    }
    if (this.schoolClassId === null) {
      return [];
    }
    return this.students().filter((student) => student.schoolClassId === this.schoolClassId);
  }

  async onClassChange(): Promise<void> {
    const allowed = new Set(this.studentOptions().map((student) => student.id));
    this.studentIds = this.studentIds.filter((id) => allowed.has(id));
    await this.load();
  }

  async load(): Promise<void> {
    if (this.schoolClassId === null) {
      return;
    }
    this.errorMessage.set(null);
    const from = toIsoDate(new Date(Date.now() - 90 * 86400000));
    const to = toIsoDate(new Date());
    try {
      this.incidents.set(await this.disciplineService.listIncidents(this.schoolClassId, from, to));
    } catch (error) {
      this.errorMessage.set(extractErrorMessage(error));
    }
  }

  async create(event: Event): Promise<void> {
    event.preventDefault();
    if (this.schoolClassId === null) {
      return;
    }
    this.errorMessage.set(null);
    try {
      await this.disciplineService.createIncident({
        schoolClassId: this.schoolClassId,
        occurredAt: toIsoDate(new Date()),
        severity: this.severity,
        description: this.description,
        studentIds: this.studentIds,
      });
      this.description = '';
      await this.load();
    } catch (error) {
      this.errorMessage.set(extractErrorMessage(error));
    }
  }
}
