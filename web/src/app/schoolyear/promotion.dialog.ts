import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { MatTableModule } from '@angular/material/table';
import { extractErrorMessage } from '../core/http-error.util';
import { SchoolClass } from '../schoolclass/school-class.model';
import { SchoolClassService } from '../schoolclass/school-class.service';
import { Student } from '../student/student.model';
import { StudentService } from '../student/student.service';
import {
  EnrollmentOutcome,
  PromotionDecision,
  PromotionResult,
  SchoolYear,
  SchoolYearService,
} from './school-year.service';

export interface PromotionDialogData {
  sourceYear: SchoolYear;
  targetYear: SchoolYear;
}

interface DecisionRow {
  studentId: number;
  studentName: string;
  studentNumber: string;
  currentClassId: number | null;
  currentClassName: string;
  outcome: EnrollmentOutcome;
  targetClassId: number | null;
}

const OUTCOMES: { value: EnrollmentOutcome; label: string }[] = [
  { value: 'PROMOTED', label: 'Passe' },
  { value: 'REPEATING', label: 'Redouble' },
  { value: 'TRANSFERRED', label: 'Transféré' },
  { value: 'GRADUATED', label: 'Fin de cycle' },
  { value: 'WITHDRAWN', label: 'A quitté' },
];

/**
 * Rentrée : réinscrire une promotion entière sur l'année suivante.
 *
 * <p>Le travail se fait d'abord par classe — « toute la 6ème A passe en 5ème A » — puis se
 * corrige élève par élève. C'est l'ordre dans lequel un conseil de classe décide, et cela
 * évite de choisir 500 fois la même destination.
 *
 * <p>Aucune décision n'est déduite d'une moyenne : le conseil tranche des cas que le logiciel
 * ne connaît pas (dossier, absence longue, décision des parents).
 */
@Component({
  selector: 'app-promotion-dialog',
  imports: [
    FormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatSelectModule,
    MatButtonModule,
    MatIconModule,
    MatTableModule,
    MatProgressSpinnerModule,
  ],
  template: `
    <h2 mat-dialog-title>Rentrée {{ data.targetYear.label }}</h2>
    <mat-dialog-content>
      @if (result(); as done) {
        <div class="promotion-done">
          <p class="promotion-counts">
            <strong>{{ done.promotedCount }}</strong> passage(s) ·
            <strong>{{ done.repeatingCount }}</strong> redoublement(s) ·
            <strong>{{ done.leftCount }}</strong> départ(s)
          </p>
          @if (done.skipped.length > 0) {
            <p class="promotion-skipped-title">Non réinscrits :</p>
            <ul class="promotion-skipped">
              @for (line of done.skipped; track line) {
                <li>{{ line }}</li>
              }
            </ul>
          } @else {
            <p class="promotion-note">Tous les élèves de {{ data.sourceYear.label }} ont été traités.</p>
          }
        </div>
      } @else {
        <p class="promotion-lead">
          Les élèves inscrits en <strong>{{ data.sourceYear.label }}</strong> vont être
          réinscrits en <strong>{{ data.targetYear.label }}</strong>. Choisissez d'abord la
          classe d'arrivée de chaque classe, puis corrigez les cas particuliers.
        </p>

        <div class="class-mapping">
          @for (source of sourceClasses(); track source.id) {
            <mat-form-field appearance="outline">
              <mat-label>{{ source.name }} →</mat-label>
              <mat-select [ngModel]="classMapping()[source.id] ?? null" (ngModelChange)="mapClass(source.id, $event)">
                <mat-option [value]="null">Choisir…</mat-option>
                @for (target of classes(); track target.id) {
                  <mat-option [value]="target.id">{{ target.name }}</mat-option>
                }
              </mat-select>
            </mat-form-field>
          }
        </div>

        @if (loading()) {
          <p class="loading-state" role="status" aria-label="Chargement des élèves"></p>
        } @else if (rows().length === 0) {
          <p class="empty-state">Aucun élève inscrit sur {{ data.sourceYear.label }}.</p>
        } @else {
          <div class="table-scroll">
            <table mat-table [dataSource]="rows()" class="data-table">
              <ng-container matColumnDef="student">
                <th mat-header-cell *matHeaderCellDef>Élève</th>
                <td mat-cell *matCellDef="let row">
                  <span class="cell-strong">{{ row.studentName }}</span>
                  <span class="cell-muted">{{ row.currentClassName }}</span>
                </td>
              </ng-container>
              <ng-container matColumnDef="outcome">
                <th mat-header-cell *matHeaderCellDef>Décision</th>
                <td mat-cell *matCellDef="let row">
                  <mat-select [(ngModel)]="row.outcome" class="inline-select">
                    @for (outcome of outcomes; track outcome.value) {
                      <mat-option [value]="outcome.value">{{ outcome.label }}</mat-option>
                    }
                  </mat-select>
                </td>
              </ng-container>
              <ng-container matColumnDef="targetClass">
                <th mat-header-cell *matHeaderCellDef>Classe d'arrivée</th>
                <td mat-cell *matCellDef="let row">
                  @if (row.outcome === 'PROMOTED') {
                    <mat-select [(ngModel)]="row.targetClassId" class="inline-select">
                      <mat-option [value]="null">Choisir…</mat-option>
                      @for (target of classes(); track target.id) {
                        <mat-option [value]="target.id">{{ target.name }}</mat-option>
                      }
                    </mat-select>
                  } @else if (row.outcome === 'REPEATING') {
                    <span class="cell-muted">{{ row.currentClassName }}</span>
                  } @else {
                    <span class="cell-muted">—</span>
                  }
                </td>
              </ng-container>
              <tr mat-header-row *matHeaderRowDef="columns"></tr>
              <tr mat-row *matRowDef="let row; columns: columns"></tr>
            </table>
          </div>
        }

        @if (errorMessage()) {
          <p class="error-message">{{ errorMessage() }}</p>
        }
      }
    </mat-dialog-content>

    <mat-dialog-actions align="end">
      @if (result()) {
        <button mat-flat-button (click)="close()">Fermer</button>
      } @else {
        <button mat-button type="button" (click)="cancel()">Annuler</button>
        <button mat-flat-button [disabled]="submitting() || rows().length === 0" (click)="submit()">
          @if (submitting()) {
            <mat-spinner diameter="20" />
          } @else {
            Réinscrire {{ rows().length }} élève(s)
          }
        </button>
      }
    </mat-dialog-actions>
  `,
  styles: `
    .promotion-lead {
      margin: 0 0 var(--space-4);
      line-height: 1.6;
    }

    .class-mapping {
      display: flex;
      flex-wrap: wrap;
      gap: var(--space-3);
      margin-bottom: var(--space-4);
    }

    .class-mapping mat-form-field {
      width: 220px;
    }

    .inline-select {
      min-width: 140px;
    }

    .promotion-counts {
      font-size: var(--font-size-body);
    }

    .promotion-skipped-title {
      margin-bottom: var(--space-1);
      font-weight: var(--font-weight-semibold);
    }

    /* Ce qui n'a pas été réinscrit est listé et non résumé : à la rentrée, un élève oublié
     * est un élève perdu. */
    .promotion-skipped {
      margin: 0;
      padding-inline-start: var(--space-5);
      color: var(--color-text-secondary);
    }

    .promotion-note {
      color: var(--color-text-secondary);
    }
  `,
})
export class PromotionDialog {
  private readonly schoolYearService = inject(SchoolYearService);
  private readonly studentService = inject(StudentService);
  private readonly schoolClassService = inject(SchoolClassService);
  private readonly dialogRef = inject(MatDialogRef<PromotionDialog, boolean>);
  protected readonly data = inject<PromotionDialogData>(MAT_DIALOG_DATA);

  protected readonly outcomes = OUTCOMES;
  protected readonly columns = ['student', 'outcome', 'targetClass'];

  protected readonly classes = signal<SchoolClass[]>([]);
  protected readonly rows = signal<DecisionRow[]>([]);
  protected readonly classMapping = signal<Record<number, number | null>>({});
  protected readonly loading = signal(true);
  protected readonly submitting = signal(false);
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly result = signal<PromotionResult | null>(null);

  /** Classes réellement représentées dans la promotion : inutile d'en proposer d'autres. */
  protected readonly sourceClasses = computed(() => {
    const ids = new Set(this.rows().map((row) => row.currentClassId));
    return this.classes().filter((schoolClass) => ids.has(schoolClass.id));
  });

  constructor() {
    void this.load();
  }

  private async load(): Promise<void> {
    try {
      const [classes, students] = await Promise.all([
        this.schoolClassService.list(),
        this.studentService.list(),
      ]);
      this.classes.set(classes);
      const byId = new Map(classes.map((schoolClass) => [schoolClass.id, schoolClass.name]));
      this.rows.set(
        students
          .filter((student: Student) => student.active && student.schoolClassId !== null)
          .map((student) => ({
            studentId: student.id,
            studentName: `${student.firstName} ${student.lastName}`,
            studentNumber: student.studentNumber,
            currentClassId: student.schoolClassId,
            currentClassName: byId.get(student.schoolClassId!) ?? '—',
            outcome: 'PROMOTED' as EnrollmentOutcome,
            targetClassId: null,
          })),
      );
    } catch (error) {
      this.errorMessage.set(extractErrorMessage(error));
    } finally {
      this.loading.set(false);
    }
  }

  /** Applique une destination à toute une classe d'un coup, puis laisse corriger au cas par cas. */
  protected mapClass(sourceClassId: number, targetClassId: number | null): void {
    this.classMapping.update((mapping) => ({ ...mapping, [sourceClassId]: targetClassId }));
    this.rows.update((rows) =>
      rows.map((row) =>
        row.currentClassId === sourceClassId && row.outcome === 'PROMOTED'
          ? { ...row, targetClassId }
          : row,
      ),
    );
  }

  async submit(): Promise<void> {
    this.submitting.set(true);
    this.errorMessage.set(null);
    try {
      const decisions: PromotionDecision[] = this.rows().map((row) => ({
        studentId: row.studentId,
        outcome: row.outcome,
        targetClassId: row.outcome === 'PROMOTED' ? row.targetClassId : null,
      }));
      this.result.set(
        await this.schoolYearService.promote(
          this.data.targetYear.id,
          this.data.sourceYear.id,
          decisions,
        ),
      );
    } catch (error) {
      this.errorMessage.set(extractErrorMessage(error));
    } finally {
      this.submitting.set(false);
    }
  }

  close(): void {
    this.dialogRef.close(true);
  }

  cancel(): void {
    this.dialogRef.close(false);
  }
}
