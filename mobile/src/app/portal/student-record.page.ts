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
import { formatMoney } from '../core/money.util';
import {
  PortalAttendance,
  PortalFeeSummary,
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
      <!-- scrollable : à quatre onglets, « Emploi du temps » ne tient plus dans un quart d'un
           écran de 375 px. Mieux vaut faire défiler que couper un libellé en plein mot. -->
      <ion-segment scrollable [value]="tab()" (ionChange)="tab.set($any($event).detail.value)">
        <ion-segment-button value="grades">Notes</ion-segment-button>
        <ion-segment-button value="attendance">Absences</ion-segment-button>
        <ion-segment-button value="fees">Frais</ion-segment-button>
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
      } @else if (tab() === 'fees') {
        @if (fees(); as summary) {
          @if (summary.lines.length === 0) {
            <p class="empty-state">Aucun frais enregistré pour le moment.</p>
          } @else {
            <div class="fee-totals" [class.fee-alert]="summary.overdueCents > 0">
              <span class="fee-remaining">{{ money(summary.totalRemainingCents) }}</span>
              <span class="fee-caption">
                @if (summary.totalRemainingCents === 0) {
                  Tout est réglé. Merci.
                } @else if (summary.overdueCents > 0) {
                  reste à payer, dont {{ money(summary.overdueCents) }} en retard
                } @else {
                  reste à payer
                }
              </span>
              <span class="fee-caption">
                {{ money(summary.totalPaidCents) }} déjà versés sur {{ money(summary.totalDueCents) }}
              </span>
            </div>

            <div class="record-list">
              @for (line of summary.lines; track line.label + line.dueDate) {
                <div class="record-row">
                  <span class="record-text">
                    <span class="record-label">{{ line.label }}</span>
                    <span class="record-meta">
                      @if (line.dueDate) {
                        échéance {{ line.dueDate | date: 'dd/MM/yyyy' }}
                      }
                      @if (line.overdue) {
                        · en retard
                      }
                      @if (line.amountPaidCents > 0 && line.amountRemainingCents > 0) {
                        · {{ money(line.amountPaidCents) }} versés
                      }
                    </span>
                  </span>
                  <span class="record-value" [class.record-settled]="line.amountRemainingCents === 0">
                    @if (line.amountRemainingCents === 0) {
                      Réglé
                    } @else {
                      {{ money(line.amountRemainingCents) }}
                    }
                  </span>
                </div>
              }
            </div>

            <p class="fee-note">
              Le règlement se fait auprès de l'établissement. Cet écran ne sert qu'à consulter.
            </p>
          }
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

    /* En mode défilant, chaque onglet garde sa largeur naturelle : on resserre seulement les
     * marges pour qu'un maximum d'onglets soit visible sans geste. */
    ion-segment-button {
      min-width: max-content;
      --padding-start: var(--space-3);
      --padding-end: var(--space-3);
      font-size: var(--font-size-caption);
    }

    /* Le solde restant est l'information que la famille vient chercher : il passe avant le
     * détail des échéances, et vire au rouge si une échéance est dépassée. */
    .fee-totals {
      display: flex;
      flex-direction: column;
      gap: 2px;
      margin-bottom: var(--space-4);
      padding: var(--space-4);
      border-radius: var(--radius);
      background: var(--color-surface);
      border: 1px solid var(--color-border);
    }

    .fee-totals.fee-alert {
      border-color: var(--color-danger, #b3261e);
    }

    .fee-remaining {
      font-family: var(--font-family-display);
      font-size: var(--font-size-display);
      font-weight: 700;
      line-height: 1.15;
    }

    .fee-totals.fee-alert .fee-remaining {
      color: var(--color-danger, #b3261e);
    }

    .fee-caption {
      color: var(--color-text-secondary);
      font-size: var(--font-size-caption);
    }

    .record-value.record-settled {
      color: var(--color-text-secondary);
      font-weight: var(--font-weight-regular, 400);
    }

    /* Dit explicitement que l'écran n'encaisse pas : sans cela, une famille cherche le bouton
     * de paiement et croit l'application incomplète. */
    .fee-note {
      margin-top: var(--space-4);
      color: var(--color-text-secondary);
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

  protected readonly money = formatMoney;
  protected readonly tab = signal<'grades' | 'attendance' | 'fees' | 'timetable'>('grades');
  protected readonly grades = signal<PortalGrade[]>([]);
  protected readonly attendance = signal<PortalAttendance[]>([]);
  protected readonly timetable = signal<PortalTimetableSlot[]>([]);
  protected readonly fees = signal<PortalFeeSummary | null>(null);
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
      const [grades, attendance, timetable, fees] = await Promise.all([
        this.portalService.grades(studentId),
        this.portalService.attendance(studentId, range.from, range.to),
        this.portalService.timetable(studentId),
        this.portalService.fees(studentId),
      ]);
      this.grades.set(grades);
      this.timetable.set(timetable);
      this.fees.set(fees);
      // Une famille consulte d'abord les absences récentes : on présente l'ordre inverse.
      this.attendance.set([...attendance].reverse());
    } catch (error) {
      this.errorMessage.set(extractErrorMessage(error));
    } finally {
      this.loading.set(false);
    }
  }
}
