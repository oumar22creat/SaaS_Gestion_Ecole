import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { extractErrorMessage } from '../core/http-error.util';
import { formatMoney } from '../core/money.util';
import { SchoolClass } from '../schoolclass/school-class.model';
import { FeeSchedule, FeeScheduleCopyResult, SchoolFeesService } from './school-fees.service';

export interface ScheduleCopyDialogData {
  sourceClass: SchoolClass;
  schedules: FeeSchedule[];
  classes: SchoolClass[];
}

/**
 * Recopie l'échéancier d'une classe vers d'autres.
 *
 * <p>Neuf classes et quatre échéances font trente-six saisies à la rentrée, pour des lignes
 * qui ne diffèrent le plus souvent que par le montant. On recopie, puis on corrige les
 * montants classe par classe.
 *
 * <p>Le dialogue dit explicitement qu'aucune facture ne sera créée : c'est la question que
 * se pose un comptable avant de cliquer sur un bouton qui touche huit classes d'un coup.
 */
@Component({
  selector: 'app-schedule-copy-dialog',
  imports: [
    FormsModule,
    MatDialogModule,
    MatCheckboxModule,
    MatButtonModule,
    MatProgressSpinnerModule,
  ],
  template: `
    <h2 mat-dialog-title>Recopier l'échéancier</h2>
    @if (result(); as done) {
      <mat-dialog-content>
        <p>
          <strong>{{ done.created.length }}</strong> échéance(s) créée(s). Aucune facture n'a
          été émise : utilisez « Facturer les élèves » classe par classe, quand les montants
          seront ajustés.
        </p>
        @if (done.skipped.length > 0) {
          <p class="flash-error">{{ done.skipped.length }} ligne(s) non recopiée(s) :</p>
          <ul class="skipped">
            @for (line of done.skipped; track line) {
              <li>{{ line }}</li>
            }
          </ul>
        }
      </mat-dialog-content>
      <mat-dialog-actions align="end">
        <button mat-flat-button type="button" (click)="close()">Fermer</button>
      </mat-dialog-actions>
    } @else {
      <mat-dialog-content>
        <p class="copy-context">
          Depuis <strong>{{ data.sourceClass.name }}</strong> :
        </p>
        <ul class="copy-lines">
          @for (schedule of data.schedules; track schedule.id) {
            <li>{{ schedule.label }} — {{ money(schedule.amountCents) }}</li>
          }
        </ul>

        <p class="copy-context">Vers :</p>
        @if (targets().length === 0) {
          <p class="empty-state">Aucune autre classe.</p>
        }
        @for (target of targets(); track target.id) {
          <mat-checkbox [(ngModel)]="selection[target.id]">{{ target.name }}</mat-checkbox>
        }

        <p class="copy-note">
          Les montants sont repris tels quels : corrigez-les ensuite classe par classe. Une
          échéance du même intitulé déjà présente ne sera pas recopiée. Aucune facture n'est
          créée.
        </p>

        @if (errorMessage()) {
          <p class="flash-error">{{ errorMessage() }}</p>
        }
      </mat-dialog-content>
      <mat-dialog-actions align="end">
        <button mat-button type="button" (click)="close()">Annuler</button>
        <button
          mat-flat-button
          type="button"
          [disabled]="submitting() || selectedIds().length === 0"
          (click)="submit()"
        >
          Recopier vers {{ selectedIds().length }} classe(s)
        </button>
      </mat-dialog-actions>
    }
  `,
  styles: `
    mat-checkbox {
      display: block;
    }

    .copy-context {
      margin: var(--space-2) 0 var(--space-1);
    }

    .copy-lines {
      margin: 0 0 var(--space-2);
      padding-left: 1.2rem;
      font-size: 0.9rem;
      line-height: 1.6;
    }

    .copy-note {
      margin-top: var(--space-3);
      color: var(--color-text-muted, #666);
      font-size: 0.82rem;
      line-height: 1.5;
    }

    .skipped {
      margin: 0;
      padding-left: 1.2rem;
      font-size: 0.85rem;
      line-height: 1.6;
    }
  `,
})
export class ScheduleCopyDialog {
  protected readonly money = formatMoney;
  private readonly schoolFeesService = inject(SchoolFeesService);
  private readonly dialogRef = inject(MatDialogRef<ScheduleCopyDialog, boolean>);
  protected readonly data = inject<ScheduleCopyDialogData>(MAT_DIALOG_DATA);

  protected readonly submitting = signal(false);
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly result = signal<FeeScheduleCopyResult | null>(null);
  protected readonly selection: Record<number, boolean> = {};

  /** Toutes les classes sauf celle d'où l'on recopie. */
  protected targets(): SchoolClass[] {
    return this.data.classes.filter((item) => item.id !== this.data.sourceClass.id);
  }

  protected selectedIds(): number[] {
    return this.targets()
      .filter((target) => this.selection[target.id])
      .map((target) => target.id);
  }

  async submit(): Promise<void> {
    this.submitting.set(true);
    this.errorMessage.set(null);
    try {
      this.result.set(
        await this.schoolFeesService.copySchedules(this.data.sourceClass.id, this.selectedIds()),
      );
    } catch (error) {
      this.errorMessage.set(extractErrorMessage(error));
    } finally {
      this.submitting.set(false);
    }
  }

  close(): void {
    this.dialogRef.close(this.result() !== null);
  }
}
