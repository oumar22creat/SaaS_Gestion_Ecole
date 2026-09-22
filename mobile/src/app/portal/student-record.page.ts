import { DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import {
  IonButton,
  IonButtons,
  IonContent,
  IonHeader,
  IonIcon,
  IonSegment,
  IonSegmentButton,
  IonTitle,
  IonToolbar,
} from '@ionic/angular';
import { extractErrorMessage } from '../core/http-error.util';
import {
  PortalAttendance,
  PortalGrade,
  PortalService,
  PortalTimetableSlot,
} from './portal.service';

/** Fenêtre d'historique des absences : l'année scolaire en cours, pas une date arbitraire. */
function schoolYearRange(): { from: string; to: string } {
  const today = new Date();
  const startYear = today.getMonth() >= 8 ? today.getFullYear() : today.getFullYear() - 1;
  return { from: `${startYear}-09-01`, to: `${startYear + 1}-08-31` };
}

@Component({
  selector: 'app-portal-student-record-page',
  imports: [
    DatePipe,
    IonHeader,
    IonToolbar,
    IonButtons,
    IonButton,
    IonTitle,
    IonContent,
    IonIcon,
    IonSegment,
    IonSegmentButton,
    RouterLink,
  ],
  template: `
    <ion-header>
      <ion-toolbar>
        <ion-buttons slot="start">
          <ion-button routerLink="/portal">
            <ion-icon aria-hidden="true" name="home-outline"></ion-icon>
            Retour
          </ion-button>
        </ion-buttons>
        <ion-title>Suivi scolaire</ion-title>
      </ion-toolbar>
    </ion-header>

    <ion-content class="ion-padding">
      <ion-segment [value]="tab()" (ionChange)="tab.set($any($event).detail.value)">
        <ion-segment-button value="grades">Notes</ion-segment-button>
        <ion-segment-button value="attendance">Absences</ion-segment-button>
        <ion-segment-button value="timetable">Emploi du temps</ion-segment-button>
      </ion-segment>

      @if (errorMessage()) {
        <p class="error-message">{{ errorMessage() }}</p>
      } @else if (loading()) {
        <p class="empty-state">Chargement…</p>
      } @else if (tab() === 'grades') {
        @if (grades().length === 0) {
          <p class="empty-state">Aucune note enregistrée pour le moment.</p>
        } @else {
          <div class="record-list">
            @for (grade of grades(); track grade.examId) {
              <div class="record-row">
                <span class="record-text">
                  <span class="record-label">{{ grade.label }}</span>
                  <span class="record-meta">
                    {{ grade.examDate | date: 'dd/MM/yyyy' }} · coefficient
                    {{ grade.coefficient }}
                  </span>
                </span>
                <span class="record-value">
                  @if (grade.absent) {
                    Absent
                  } @else {
                    {{ grade.score }} / {{ grade.maxScore }}
                  }
                </span>
              </div>
            }
          </div>
        }
      } @else if (tab() === 'attendance') {
        @if (attendance().length === 0) {
          <p class="empty-state">Aucune absence sur l'année scolaire en cours.</p>
        } @else {
          <div class="record-list">
            @for (entry of attendance(); track entry.date) {
              <div class="record-row">
                <span class="record-text">
                  <span class="record-label">{{ statusLabel(entry.status) }}</span>
                  <span class="record-meta">
                    {{ entry.date | date: 'dd/MM/yyyy'
                    }}{{ entry.reason ? ' · ' + entry.reason : '' }}
                  </span>
                </span>
                <span class="record-value">
                  {{ entry.justified ? 'Justifiée' : 'Non justifiée' }}
                </span>
              </div>
            }
          </div>
        }
      } @else {
        @if (timetable().length === 0) {
          <p class="empty-state">Aucun cours planifié pour cette classe.</p>
        } @else {
          <div class="record-list">
            @for (slot of timetable(); track slot.dayOfWeek + slot.startTime + slot.subject) {
              <div class="record-row">
                <span class="record-text">
                  <span class="record-label">{{ slot.subject }}</span>
                  <span class="record-meta">
                    {{ dayLabel(slot.dayOfWeek) }} · {{ hourMinute(slot.startTime) }} à
                    {{ hourMinute(slot.endTime) }}{{ slot.room ? ' · ' + slot.room : '' }}
                  </span>
                </span>
                <span class="record-value">{{ slot.teacher }}</span>
              </div>
            }
          </div>
        }
      }
    </ion-content>
  `,
  styles: `
    ion-segment {
      margin-bottom: var(--space-4);
    }

    /* « Emploi du temps » ne tient pas dans un tiers d'un écran de 375 px à la taille de
     * police par défaut d'Ionic : l'onglet était coupé en plein mot. */
    ion-segment-button {
      min-width: 0;
      --padding-start: var(--space-1);
      --padding-end: var(--space-1);
      font-size: var(--font-size-caption);
    }

    .record-list {
      display: grid;
      gap: var(--space-2);
    }

    .record-row {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: var(--space-3);
      padding: var(--space-3) var(--space-4);
      border-radius: var(--radius);
      background: var(--color-surface);
      border: 1px solid var(--color-border);
    }

    .record-text {
      display: flex;
      flex-direction: column;
      min-width: 0;
    }

    .record-label {
      font-weight: 600;
    }

    .record-meta {
      color: var(--color-text-secondary);
      font-size: var(--font-size-small);
    }

    .record-value {
      flex: 0 0 auto;
      font-weight: 600;
      color: var(--tenant-primary);
    }
  `,
})
export class PortalStudentRecordPage {
  private readonly portalService = inject(PortalService);
  private readonly route = inject(ActivatedRoute);

  protected readonly tab = signal<'grades' | 'attendance' | 'timetable'>('grades');
  protected readonly grades = signal<PortalGrade[]>([]);
  protected readonly attendance = signal<PortalAttendance[]>([]);
  protected readonly timetable = signal<PortalTimetableSlot[]>([]);
  protected readonly loading = signal(true);
  protected readonly errorMessage = signal<string | null>(null);

  constructor() {
    void this.load();
  }

  /** L'API renvoie HH:mm:ss ; les secondes n'apportent rien sur un emploi du temps. */
  protected hourMinute(time: string): string {
    return time.slice(0, 5);
  }

  protected dayLabel(day: string): string {
    const days: Record<string, string> = {
      MONDAY: 'Lundi',
      TUESDAY: 'Mardi',
      WEDNESDAY: 'Mercredi',
      THURSDAY: 'Jeudi',
      FRIDAY: 'Vendredi',
      SATURDAY: 'Samedi',
      SUNDAY: 'Dimanche',
    };
    return days[day] ?? day;
  }

  protected statusLabel(status: string): string {
    const labels: Record<string, string> = {
      PRESENT: 'Présent',
      ABSENT: 'Absence',
      LATE: 'Retard',
      EARLY_DEPARTURE: 'Départ anticipé',
    };
    return labels[status] ?? status;
  }

  private async load(): Promise<void> {
    const studentId = Number(this.route.snapshot.paramMap.get('studentId'));
    const range = schoolYearRange();
    try {
      const [grades, attendance, timetable] = await Promise.all([
        this.portalService.grades(studentId),
        this.portalService.attendance(studentId, range.from, range.to),
        this.portalService.timetable(studentId),
      ]);
      this.grades.set(grades);
      this.timetable.set(timetable);
      // Une famille consulte d'abord les absences récentes : on présente l'ordre inverse.
      this.attendance.set([...attendance].reverse());
    } catch (error) {
      this.errorMessage.set(extractErrorMessage(error));
    } finally {
      this.loading.set(false);
    }
  }
}
