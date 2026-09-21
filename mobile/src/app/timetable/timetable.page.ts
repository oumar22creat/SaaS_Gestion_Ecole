import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { IonButton, IonButtons, IonContent, IonHeader, IonIcon, IonItem, IonSelect, IonSelectOption, IonTitle, IonToolbar } from '@ionic/angular';
import { SchoolClass } from '../schoolclass/school-class.model';
import { SchoolClassService } from '../schoolclass/school-class.service';
import { Subject } from '../subject/subject.model';
import { SubjectService } from '../subject/subject.service';
import { DAYS_OF_WEEK, DayOfWeek, TimetableEntry } from './timetable-entry.model';
import { TimetableEntryService } from './timetable-entry.service';

/**
 * Emploi du temps consultable sur le terrain — docs/DESIGN.md §5 : lecture rapide
 * jour par jour, le créneau horaire étant l'information cherchée en premier.
 */
@Component({
  selector: 'app-timetable-page',
  imports: [FormsModule,
    RouterLink,
    IonHeader,
    IonToolbar,
    IonTitle,
    IonButtons,
    IonButton,
    IonContent,
    IonItem,
    IonSelect,
    IonSelectOption, IonIcon],
  template: `
    <ion-header>
      <ion-toolbar>
        <ion-buttons slot="start">
          <ion-button routerLink="/home"><ion-icon aria-hidden="true" name="school-outline"></ion-icon> Accueil</ion-button>
        </ion-buttons>
        <ion-title>Emploi du temps</ion-title>
      </ion-toolbar>
    </ion-header>
    <ion-content class="ion-padding">
      <div class="screen-header">
        <ion-icon class="screen-icon" aria-hidden="true" name="calendar-outline"></ion-icon>
        <h1 class="screen-title">Emploi du temps</h1>
      </div>

      <ion-item class="card-item">
        <ion-select label="Classe" labelPlacement="stacked" [(ngModel)]="schoolClassId">
          <ion-select-option [value]="null">Toutes les classes</ion-select-option>
          @for (schoolClass of classes(); track schoolClass.id) {
            <ion-select-option [value]="schoolClass.id">{{ schoolClass.name }}</ion-select-option>
          }
        </ion-select>
      </ion-item>

      @if (totalShown() > 0) {
        @for (day of days; track day.value) {
          @if (entriesFor(day.value).length > 0) {
            <p class="section-label">{{ day.label }}</p>
            <div class="slots">
              @for (entry of entriesFor(day.value); track entry.id) {
                <div class="slot">
                  <span class="slot-time">
                    <span class="slot-start">{{ entry.startTime }}</span>
                    <span class="slot-end">{{ entry.endTime }}</span>
                  </span>
                  <span class="slot-body">
                    <span class="slot-subject">
                      <ion-icon aria-hidden="true" name="calendar-outline"></ion-icon>
                      {{ subjectName(entry.subjectId) }}
                    </span>
                    <span class="slot-meta"><ion-icon aria-hidden="true" name="log-out-outline"></ion-icon> Salle #{{ entry.roomId }}</span>
                  </span>
                </div>
              }
            </div>
          }
        }
      } @else {
        <p class="empty-state">Aucun créneau planifié pour cette sélection.</p>
      }
    </ion-content>
  `,
  styles: `
    ion-item.card-item {
      margin-bottom: var(--space-4);
    }
    .slots {
      display: flex;
      flex-direction: column;
      gap: var(--space-2);
    }
    .slot {
      display: flex;
      align-items: stretch;
      gap: var(--space-3);
      padding: var(--space-3) var(--space-4);
      background: var(--color-surface);
      border: 1px solid var(--color-border);
      border-radius: var(--radius);
      box-shadow: var(--shadow-1);
    }
    /* Le créneau sert de repère de lecture : colonne fixe, séparée par un filet vertical. */
    .slot-time {
      display: flex;
      flex: 0 0 auto;
      flex-direction: column;
      justify-content: center;
      gap: 2px;
      width: 56px;
      padding-inline-end: var(--space-3);
      border-inline-end: 1px solid var(--color-border);
      font-variant-numeric: tabular-nums;
    }
    .slot-start {
      font-family: var(--font-family-display);
      font-size: var(--font-size-body);
      font-weight: var(--font-weight-semibold);
    }
    .slot-end {
      color: var(--color-text-secondary);
      font-size: var(--font-size-caption);
    }
    .slot-body {
      display: flex;
      flex-direction: column;
      justify-content: center;
      gap: 2px;
      min-width: 0;
    }
    .slot-subject {
      display: flex;
      align-items: center;
      gap: var(--space-1);
      font-weight: var(--font-weight-semibold);
    }
    .slot-meta {
      color: var(--color-text-secondary);
      font-size: var(--font-size-caption);
    }
  `,
})
export class TimetablePage {
  private readonly timetableService = inject(TimetableEntryService);
  private readonly schoolClassService = inject(SchoolClassService);
  private readonly subjectService = inject(SubjectService);

  protected readonly classes = signal<SchoolClass[]>([]);
  protected readonly entries = signal<TimetableEntry[]>([]);
  protected readonly subjects = signal<Subject[]>([]);
  protected readonly days = DAYS_OF_WEEK;
  protected schoolClassId: number | null = null;
  private readonly byId = computed(() => new Map(this.subjects().map((subject) => [subject.id, subject.name])));

  constructor() {
    void this.schoolClassService.list().then((classes) => this.classes.set(classes));
    void this.timetableService.list().then((entries) => this.entries.set(entries));
    void this.subjectService.list().then((subjects) => this.subjects.set(subjects));
  }

  protected entriesFor(day: DayOfWeek): TimetableEntry[] {
    return this.entries()
      .filter((entry) => entry.dayOfWeek === day && (this.schoolClassId === null || entry.schoolClassId === this.schoolClassId))
      .sort((a, b) => a.startTime.localeCompare(b.startTime));
  }

  protected totalShown(): number {
    return this.entries().filter(
      (entry) => this.schoolClassId === null || entry.schoolClassId === this.schoolClassId,
    ).length;
  }

  protected subjectName(subjectId: number): string {
    return this.byId().get(subjectId) ?? `Matière #${subjectId}`;
  }
}
