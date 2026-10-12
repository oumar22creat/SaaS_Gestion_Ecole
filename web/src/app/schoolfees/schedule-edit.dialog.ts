import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { FRENCH_DATE_LOCALE } from '../core/date-locale.provider';
import { fieldError } from '../core/form-error.util';
import { extractErrorMessage } from '../core/http-error.util';
import { toIsoDate } from '../core/iso-date.util';
import { formatMoney } from '../core/money.util';
import { FeeSchedule, FeeScheduleUpdateResult, SchoolFeesService } from './school-fees.service';

export interface ScheduleEditDialogData {
  schedule: FeeSchedule;
}

/**
 * Correction d'une échéance déjà créée.
 *
 * <p>Changer le montant répercute le nouveau tarif sur les factures déjà émises : c'est
 * attendu, puisqu'on corrige précisément parce que le montant était faux. Mais certaines
 * factures ne peuvent pas suivre — celles des familles qui ont déjà versé davantage. Le
 * dialogue les nomme plutôt que de se refermer sur un succès trompeur : sans cette liste,
 * l'établissement croirait toute la classe au nouveau tarif.
 */
@Component({
  selector: 'app-schedule-edit-dialog',
  imports: [
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatDatepickerModule,
    MatButtonModule,
    MatProgressSpinnerModule,
  ],
  providers: [FRENCH_DATE_LOCALE],
  template: `
    <h2 mat-dialog-title>Corriger l'échéance</h2>
    @if (result(); as done) {
      <mat-dialog-content>
        <p class="result-summary">
          Échéance corrigée. <strong>{{ done.updatedInvoices }}</strong> facture(s) passée(s)
          au nouveau montant.
        </p>
        @if (done.skipped.length > 0) {
          <p class="flash-error">
            {{ done.skipped.length }} facture(s) n'ont pas pu suivre et restent à l'ancien
            montant. À traiter une par une :
          </p>
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
      <form [formGroup]="form" (ngSubmit)="submit()">
        <mat-dialog-content>
          <mat-form-field appearance="outline">
            <mat-label>Intitulé</mat-label>
            <input matInput formControlName="label" />
            <mat-error>{{ fieldError(form.controls.label) }}</mat-error>
          </mat-form-field>

          <mat-form-field appearance="outline">
            <mat-label>Montant par élève (XOF)</mat-label>
            <input matInput type="number" formControlName="amountCents" />
            <mat-hint>
              Actuellement {{ money(data.schedule.amountCents) }}. Le changer corrige aussi
              les factures déjà émises, remises individuelles conservées.
            </mat-hint>
            <mat-error>{{ fieldError(form.controls.amountCents) }}</mat-error>
          </mat-form-field>

          <mat-form-field appearance="outline">
            <mat-label>Date limite de règlement</mat-label>
            <input matInput [matDatepicker]="picker" formControlName="dueDate" />
            <mat-datepicker-toggle matIconSuffix [for]="picker" />
            <mat-datepicker #picker />
            <mat-error>{{ fieldError(form.controls.dueDate) }}</mat-error>
          </mat-form-field>

          @if (errorMessage()) {
            <p class="flash-error">{{ errorMessage() }}</p>
          }
        </mat-dialog-content>
        <mat-dialog-actions align="end">
          <button mat-button type="button" (click)="close()">Annuler</button>
          <button mat-flat-button type="submit" [disabled]="submitting()">Enregistrer</button>
        </mat-dialog-actions>
      </form>
    }
  `,
  styles: `
    form {
      display: flex;
      flex-direction: column;
    }

    mat-form-field {
      width: 100%;
    }

    .result-summary {
      margin: 0 0 var(--space-2);
    }

    .skipped {
      margin: 0;
      padding-left: 1.2rem;
      font-size: 0.85rem;
      line-height: 1.6;
    }
  `,
})
export class ScheduleEditDialog {
  protected readonly fieldError = fieldError;
  protected readonly money = formatMoney;
  private readonly formBuilder = inject(FormBuilder);
  private readonly schoolFeesService = inject(SchoolFeesService);
  private readonly dialogRef = inject(MatDialogRef<ScheduleEditDialog, boolean>);
  protected readonly data = inject<ScheduleEditDialogData>(MAT_DIALOG_DATA);

  protected readonly submitting = signal(false);
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly result = signal<FeeScheduleUpdateResult | null>(null);

  protected readonly form = this.formBuilder.nonNullable.group({
    label: [this.data.schedule.label, [Validators.required, Validators.maxLength(255)]],
    amountCents: [this.data.schedule.amountCents, [Validators.required, Validators.min(1)]],
    dueDate: [new Date(this.data.schedule.dueDate) as Date | null, [Validators.required]],
  });

  async submit(): Promise<void> {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.submitting.set(true);
    this.errorMessage.set(null);
    try {
      const value = this.form.getRawValue();
      this.result.set(
        await this.schoolFeesService.updateSchedule(
          this.data.schedule.id,
          value.label,
          value.amountCents,
          toIsoDate(value.dueDate!),
        ),
      );
    } catch (error) {
      this.errorMessage.set(extractErrorMessage(error));
    } finally {
      this.submitting.set(false);
    }
  }

  /** Renvoie « modifié » dès qu'une correction a eu lieu, pour que l'écran se recharge. */
  close(): void {
    this.dialogRef.close(this.result() !== null);
  }
}
