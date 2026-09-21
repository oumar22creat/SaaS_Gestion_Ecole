import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { IonButton, IonButtons, IonContent, IonHeader, IonIcon, IonItem, IonLabel, IonList, IonSelect, IonSelectOption, IonTitle, IonToolbar } from '@ionic/angular';
import { extractErrorMessage } from '../core/http-error.util';
import { SchoolClass } from '../schoolclass/school-class.model';
import { SchoolClassService } from '../schoolclass/school-class.service';
import { Student } from '../student/student.model';
import { StudentService } from '../student/student.service';
import { ATTENDANCE_STATUSES, AttendanceStatus } from './attendance.model';
import { AttendanceService } from './attendance.service';

interface RollCallRow {
  studentId: number;
  studentLabel: string;
  status: AttendanceStatus;
}

/**
 * Feuille d'appel enseignant (mobile-first) — docs/DESIGN.md §5 : l'appel d'une classe
 * doit tenir en quelques secondes, d'où un statut posé en un seul appui plutôt qu'une
 * liste déroulante par élève.
 */
@Component({
  selector: 'app-roll-call-page',
  imports: [FormsModule,
    RouterLink,
    IonHeader,
    IonToolbar,
    IonTitle,
    IonButtons,
    IonButton,
    IonContent,
    IonList,
    IonItem,
    IonLabel,
    IonSelect,
    IonSelectOption, IonIcon],
  template: `
    <ion-header>
      <ion-toolbar>
        <ion-buttons slot="start">
          <ion-button routerLink="/home"><ion-icon aria-hidden="true" name="school-outline"></ion-icon> Accueil</ion-button>
        </ion-buttons>
        <ion-title>Feuille d'appel</ion-title>
      </ion-toolbar>
    </ion-header>
    <ion-content class="ion-padding">
      <ion-list class="filters">
        <ion-item class="card-item">
          <ion-select
            label="Classe"
            labelPlacement="stacked"
            [(ngModel)]="schoolClassId"
            (ionChange)="load()"
          >
            @for (schoolClass of classes(); track schoolClass.id) {
              <ion-select-option [value]="schoolClass.id">{{ schoolClass.name }}</ion-select-option>
            }
          </ion-select>
        </ion-item>
        <ion-item class="card-item">
          <ion-label>
            <p>Date</p>
            <input type="date" [(ngModel)]="date" (change)="load()" />
          </ion-label>
        </ion-item>
      </ion-list>

      @if (errorMessage()) {
        <p class="flash-error">{{ errorMessage() }}</p>
      }
      @if (success()) {
        <p class="flash-success">Appel enregistré.</p>
      }

      @if (rows().length > 0) {
        <div class="tally">
          @for (status of statuses; track status.value) {
            <span class="tally-item">
              <ion-icon aria-hidden="true" [name]="status.icon"></ion-icon>
              {{ countFor(status.value) }}
            </span>
          }
        </div>

        <div class="roster">
          @for (row of rows(); track row.studentId) {
            <div class="student">
              <p class="student-name">{{ row.studentLabel }}</p>
              <div class="status-row">
                @for (status of statuses; track status.value) {
                  <button
                    type="button"
                    class="status-button"
                    [class.selected]="row.status === status.value"
                    [attr.aria-pressed]="row.status === status.value"
                    (click)="setStatus(row.studentId, status.value)"
                  >
                    <ion-icon aria-hidden="true" [name]="status.icon"></ion-icon>
                    {{ status.label }}
                  </button>
                }
              </div>
            </div>
          }
        </div>

        <ion-button expand="block" class="save" (click)="submit()"><ion-icon aria-hidden="true" name="save-outline"></ion-icon> Enregistrer l'appel ({{ rows().length }})
        </ion-button>
      } @else {
        <p class="empty-state">Choisissez une classe et une date pour démarrer l'appel.</p>
      }
    </ion-content>
  `,
  styles: `
    .filters {
      margin-bottom: var(--space-4);
    }
    .tally {
      display: flex;
      flex-wrap: wrap;
      gap: var(--space-2);
      margin-bottom: var(--space-3);
    }
    .tally-item {
      display: inline-flex;
      align-items: center;
      gap: var(--space-1);
      padding: var(--space-1) var(--space-3);
      border-radius: var(--radius-pill);
      background: var(--color-surface);
      border: 1px solid var(--color-border);
      color: var(--color-text-secondary);
      font-size: var(--font-size-caption);
      font-weight: var(--font-weight-semibold);
    }
    .roster {
      display: flex;
      flex-direction: column;
      gap: var(--space-2);
    }
    .student {
      padding: var(--space-3) var(--space-4) var(--space-4);
      background: var(--color-surface);
      border: 1px solid var(--color-border);
      border-radius: var(--radius);
      box-shadow: var(--shadow-1);
    }
    .student-name {
      margin: 0 0 var(--space-3);
      font-size: var(--font-size-body);
      font-weight: var(--font-weight-semibold);
    }
    .status-row {
      display: grid;
      grid-template-columns: repeat(4, 1fr);
      gap: var(--space-2);
    }
    /* Cible tactile >= 44px (docs/DESIGN.md §1) : l'appel se fait debout, au pouce. */
    .status-button {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 2px;
      min-height: 52px;
      padding: var(--space-2) 0;
      border: 1px solid var(--color-border);
      border-radius: var(--radius-sm);
      background: var(--color-surface-muted);
      color: var(--color-text-secondary);
      font-family: inherit;
      font-size: 10.5px;
      font-weight: var(--font-weight-semibold);
      line-height: 1.15;
      text-align: center;
    }
    .status-button span {
      font-size: 17px;
    }
    .status-button.selected {
      background: var(--tenant-primary-soft);
      border-color: var(--tenant-primary);
      color: var(--tenant-primary);
    }
    .save {
      margin-top: var(--space-5);
    }
  `,
})
export class RollCallPage {
  private readonly schoolClassService = inject(SchoolClassService);
  private readonly studentService = inject(StudentService);
  private readonly attendanceService = inject(AttendanceService);

  protected readonly classes = signal<SchoolClass[]>([]);
  protected readonly rows = signal<RollCallRow[]>([]);
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly success = signal(false);
  protected readonly statuses = ATTENDANCE_STATUSES;
  protected schoolClassId: number | null = null;
  protected date = new Date().toISOString().slice(0, 10);

  private readonly tally = computed(() => {
    const counts = new Map<AttendanceStatus, number>();
    for (const row of this.rows()) {
      counts.set(row.status, (counts.get(row.status) ?? 0) + 1);
    }
    return counts;
  });

  constructor() {
    void this.schoolClassService.list().then((classes) => this.classes.set(classes));
  }

  countFor(status: AttendanceStatus): number {
    return this.tally().get(status) ?? 0;
  }

  setStatus(studentId: number, status: AttendanceStatus): void {
    this.success.set(false);
    this.rows.update((rows) =>
      rows.map((row) => (row.studentId === studentId ? { ...row, status } : row)),
    );
  }

  async load(): Promise<void> {
    this.success.set(false);
    if (this.schoolClassId === null) {
      this.rows.set([]);
      return;
    }
    const [students, existing] = await Promise.all([
      this.studentService.list(),
      this.attendanceService.listForClassAndDate(this.schoolClassId, this.date),
    ]);
    const byStudent = new Map(existing.map((record) => [record.studentId, record]));
    this.rows.set(
      students
        .filter((student: Student) => student.schoolClassId === this.schoolClassId && student.active)
        .map((student) => ({
          studentId: student.id,
          studentLabel: `${student.firstName} ${student.lastName}`,
          status: byStudent.get(student.id)?.status ?? 'PRESENT',
        })),
    );
  }

  async submit(): Promise<void> {
    if (this.schoolClassId === null) {
      return;
    }
    this.errorMessage.set(null);
    try {
      await this.attendanceService.submitRollCall({
        schoolClassId: this.schoolClassId,
        date: this.date,
        entries: this.rows().map((row) => ({
          studentId: row.studentId,
          status: row.status,
          reason: null,
          justified: false,
          comment: null,
        })),
      });
      this.success.set(true);
    } catch (error) {
      this.errorMessage.set(extractErrorMessage(error));
    }
  }
}
