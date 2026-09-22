import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { IonButton, IonButtons, IonContent, IonHeader, IonIcon, IonTitle, IonToolbar } from '@ionic/angular';
import { extractErrorMessage } from '../core/http-error.util';
import { Student } from '../student/student.model';
import { StudentService } from '../student/student.service';
import { Exam } from './grade.model';
import { ExamService } from './exam.service';
import { GradeService } from './grade.service';

interface GradeRow {
  studentId: number;
  studentLabel: string;
  score: number | null;
  absent: boolean;
}

/**
 * Saisie des notes d'une évaluation (mobile-first) — docs/DESIGN.md §5 : un enseignant
 * saisit une classe entière d'affilée, donc une ligne = un champ numérique large plus un
 * bouton « absent » qui neutralise la note.
 */
@Component({
  selector: 'app-grade-entry-page',
  imports: [FormsModule,
    RouterLink,
    IonHeader,
    IonToolbar,
    IonTitle,
    IonButtons,
    IonButton,
    IonContent, IonIcon],
  template: `
    <ion-header>
      <ion-toolbar>
        <ion-buttons slot="start">
          <ion-button routerLink="/exams"><ion-icon aria-hidden="true" name="create-outline"></ion-icon> Notes</ion-button>
        </ion-buttons>
        <ion-title>Saisie</ion-title>
      </ion-toolbar>
    </ion-header>
    <ion-content class="ion-padding">
      <div class="screen-header">
        <ion-icon class="screen-icon" aria-hidden="true" name="calculator-outline"></ion-icon>
        <h1 class="screen-title">{{ exam()?.label ?? 'Saisie des notes' }}</h1>
      </div>

      @if (exam()) {
        <div class="tally">
          <span class="tally-item">Barème /{{ exam()!.maxScore }}</span>
          <span class="tally-item">Saisies : {{ filledCount() }}/{{ rows().length }}</span>
          <span class="tally-item">Absents : {{ absentCount() }}</span>
        </div>
      }

      @if (errorMessage()) {
        <p class="flash-error">{{ errorMessage() }}</p>
      }
      @if (success()) {
        <p class="flash-success">Notes enregistrées.</p>
      }

      @if (rows().length > 0) {
        <div class="roster">
          @for (row of rows(); track row.studentId) {
            <div class="student" [class.absent]="row.absent">
              <p class="student-name">{{ row.studentLabel }}</p>
              <div class="score-row">
                <input
                  class="score-input"
                  type="number"
                  inputmode="decimal"
                  min="0"
                  [max]="exam()?.maxScore ?? 20"
                  [attr.aria-label]="'Note de ' + row.studentLabel"
                  [disabled]="row.absent"
                  [ngModel]="row.score"
                  (ngModelChange)="setScore(row.studentId, $event)"
                />
                <span class="score-scale">/ {{ exam()?.maxScore ?? 20 }}</span>
                <button
                  type="button"
                  class="absent-button"
                  [class.selected]="row.absent"
                  [attr.aria-pressed]="row.absent"
                  (click)="toggleAbsent(row.studentId)"
                >
                  <ion-icon aria-hidden="true" name="close-circle-outline"></ion-icon>
                  Absent
                </button>
              </div>
            </div>
          }
        </div>

        <ion-button expand="block" class="save" (click)="submit()"><ion-icon aria-hidden="true" name="save-outline"></ion-icon> Enregistrer les notes ({{ rows().length }})
        </ion-button>
      } @else {
        <p class="empty-state">Aucun élève actif dans la classe de cette évaluation.</p>
      }
    </ion-content>
  `,
  styles: `
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
    .student.absent {
      background: var(--color-surface-muted);
    }
    .student-name {
      margin: 0 0 var(--space-3);
      font-size: var(--font-size-body);
      font-weight: var(--font-weight-semibold);
    }
    .score-row {
      display: flex;
      align-items: center;
      gap: var(--space-3);
    }
    /* Cible tactile >= 44px (docs/DESIGN.md §1) : saisie au pouce, classe après classe. */
    .score-input {
      width: 88px;
      min-height: 48px;
      padding: 0 var(--space-3);
      border: 1px solid var(--color-border-strong);
      border-radius: var(--radius-sm);
      background: var(--color-surface);
      color: var(--color-text);
      font-family: var(--font-family-display);
      font-size: var(--font-size-title-3);
      font-weight: var(--font-weight-semibold);
      text-align: center;
    }
    .score-input:focus {
      outline: 2px solid var(--tenant-primary);
      outline-offset: 1px;
    }
    .score-input:disabled {
      background: var(--color-surface-muted);
      color: var(--color-text-secondary);
    }
    .score-scale {
      flex: 1;
      color: var(--color-text-secondary);
      font-size: var(--font-size-caption);
    }
    .absent-button {
      display: flex;
      align-items: center;
      gap: var(--space-1);
      min-height: 44px;
      padding: 0 var(--space-3);
      border: 1px solid var(--color-border);
      border-radius: var(--radius-pill);
      background: var(--color-surface-muted);
      color: var(--color-text-secondary);
      font-family: inherit;
      font-size: var(--font-size-caption);
      font-weight: var(--font-weight-semibold);
    }
    .absent-button.selected {
      background: var(--tenant-primary-soft);
      border-color: var(--tenant-primary);
      color: var(--tenant-primary);
    }
    .save {
      margin-top: var(--space-5);
    }
  `,
})
export class GradeEntryPage {
  private readonly route = inject(ActivatedRoute);
  private readonly examService = inject(ExamService);
  private readonly gradeService = inject(GradeService);
  private readonly studentService = inject(StudentService);

  private readonly examId = Number(this.route.snapshot.paramMap.get('examId'));
  protected readonly exam = signal<Exam | null>(null);
  protected readonly rows = signal<GradeRow[]>([]);
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly success = signal(false);

  protected readonly absentCount = computed(() => this.rows().filter((row) => row.absent).length);
  protected readonly filledCount = computed(
    () => this.rows().filter((row) => row.absent || row.score !== null).length,
  );

  constructor() {
    void this.load();
  }

  private async load(): Promise<void> {
    this.errorMessage.set(null);
    try {
      const [exam, students, grades] = await Promise.all([
        this.examService.getById(this.examId),
        this.studentService.list(),
        this.gradeService.listForExam(this.examId),
      ]);
      this.exam.set(exam);
      const byStudent = new Map(grades.map((grade) => [grade.studentId, grade]));
      this.rows.set(
        students
          .filter(
            (student: Student) => student.schoolClassId === exam.schoolClassId && student.active,
          )
          .map((student) => {
            const grade = byStudent.get(student.id);
            return {
              studentId: student.id,
              studentLabel: `${student.firstName} ${student.lastName}`,
              score: grade?.score ?? null,
              absent: grade?.absent ?? false,
            };
          }),
      );
    } catch (error) {
      // Sans ce catch, un échec de chargement (session expirée, réseau coupé, droits refusés)
      // donnait une liste d'élèves vide, impossible à distinguer d'une classe sans élève :
      // l'enseignant repartait chercher l'erreur ailleurs.
      this.rows.set([]);
      this.errorMessage.set(extractErrorMessage(error));
    }
  }

  /**
   * La note passe par `rows.update` plutôt que par `[(ngModel)]` : la liaison à deux sens
   * modifiait l'objet à l'intérieur du tableau sans remplacer le tableau, donc sans notifier
   * le signal. Les compteurs de progression (« Saisies : 0/45 ») restaient alors figés à zéro
   * pendant toute la saisie — c'est-à-dire exactement quand l'enseignant s'en sert pour savoir
   * où il en est dans sa classe.
   */
  setScore(studentId: number, score: number | null): void {
    this.success.set(false);
    const value = score === null || Number.isNaN(score) ? null : score;
    this.rows.update((rows) =>
      rows.map((row) => (row.studentId === studentId ? { ...row, score: value } : row)),
    );
  }

  toggleAbsent(studentId: number): void {
    this.success.set(false);
    this.rows.update((rows) =>
      rows.map((row) => (row.studentId === studentId ? { ...row, absent: !row.absent } : row)),
    );
  }

  async submit(): Promise<void> {
    this.errorMessage.set(null);
    try {
      await this.gradeService.submit(
        this.examId,
        this.rows().map((row) => ({
          studentId: row.studentId,
          score: row.absent ? null : row.score,
          absent: row.absent,
          comment: null,
        })),
      );
      this.success.set(true);
    } catch (error) {
      this.errorMessage.set(extractErrorMessage(error));
    }
  }
}
