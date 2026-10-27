import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { fieldError } from '../core/form-error.util';
import { extractErrorMessage } from '../core/http-error.util';
import { formatMoney } from '../core/money.util';
import { FeeOutstanding, SchoolFeesService } from './school-fees.service';

export interface DiscountDialogData {
  entry: FeeOutstanding;
}

/**
 * Remise sur une facture : bourse, tarif fratrie, exonération.
 *
 * <p>Le motif est obligatoire. Une réduction sans motif est un trou dans la comptabilité :
 * six mois plus tard, personne ne saura si c'était une bourse, une erreur de saisie ou un
 * arrangement non autorisé. Il s'imprime sur le reçu et la famille le lit dans son portail.
 */
@Component({
  selector: 'app-discount-dialog',
  imports: [
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatProgressSpinnerModule,
  ],
  template: `
    <h2 mat-dialog-title>Accorder une remise</h2>
    <form [formGroup]="form" (ngSubmit)="submit()">
      <mat-dialog-content>
        <p class="discount-context">
          <strong>{{ data.entry.studentName }}</strong> · {{ data.entry.className }}<br />
          {{ data.entry.scheduleLabel }} — tarif de la classe
          <strong>{{ money(grossCents) }}</strong>
          @if (data.entry.discountCents > 0) {
            <br />Remise en cours : {{ money(data.entry.discountCents) }}
            @if (data.entry.discountReason) {
              ({{ data.entry.discountReason }})
            }
          }
        </p>

        <mat-form-field appearance="outline">
          <mat-label>Montant de la remise (FCFA)</mat-label>
          <input matInput type="number" formControlName="amountCents" />
          <mat-hint>
            Reste à la charge de la famille : {{ money(remainingAfterDiscount()) }}. Zéro
            annule une remise accordée par erreur.
          </mat-hint>
          <mat-error>{{ fieldError(form.controls.amountCents) }}</mat-error>
        </mat-form-field>

        <mat-form-field appearance="outline">
          <mat-label>Motif</mat-label>
          <input matInput formControlName="reason" />
          <mat-hint>« Tarif fratrie, 2e enfant », « Bourse accordée par le conseil ».</mat-hint>
          <mat-error>{{ fieldError(form.controls.reason) }}</mat-error>
        </mat-form-field>

        @if (errorMessage()) {
          <p class="flash-error">{{ errorMessage() }}</p>
        }
      </mat-dialog-content>
      <mat-dialog-actions align="end">
        <button mat-button type="button" (click)="cancel()">Annuler</button>
        <button mat-flat-button type="submit" [disabled]="submitting()">Enregistrer</button>
      </mat-dialog-actions>
    </form>
  `,
  styles: `
    form {
      display: flex;
      flex-direction: column;
    }

    mat-form-field {
      width: 100%;
    }

    .discount-context {
      margin: 0 0 var(--space-3);
      line-height: 1.6;
    }
  `,
})
export class DiscountDialog {
  protected readonly fieldError = fieldError;
  protected readonly money = formatMoney;
  private readonly formBuilder = inject(FormBuilder);
  private readonly schoolFeesService = inject(SchoolFeesService);
  private readonly dialogRef = inject(MatDialogRef<DiscountDialog, boolean>);
  protected readonly data = inject<DiscountDialogData>(MAT_DIALOG_DATA);

  protected readonly submitting = signal(false);
  protected readonly errorMessage = signal<string | null>(null);

  /** Le tarif de la classe : le net dû et la remise déjà accordée réunis. */
  protected readonly grossCents = this.data.entry.amountDueCents + this.data.entry.discountCents;

  protected readonly form = this.formBuilder.nonNullable.group({
    amountCents: [this.data.entry.discountCents, [Validators.required, Validators.min(0)]],
    reason: [this.data.entry.discountReason ?? '', [Validators.required, Validators.maxLength(255)]],
  });

  /** Ce que la famille devra après remise : le chiffre qu'on annonce au guichet. */
  protected remainingAfterDiscount(): number {
    return Math.max(0, this.grossCents - (this.form.controls.amountCents.value || 0));
  }

  async submit(): Promise<void> {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.submitting.set(true);
    this.errorMessage.set(null);
    try {
      const value = this.form.getRawValue();
      await this.schoolFeesService.applyDiscount(
        this.data.entry.invoiceId,
        value.amountCents,
        value.reason,
      );
      this.dialogRef.close(true);
    } catch (error) {
      this.errorMessage.set(extractErrorMessage(error));
    } finally {
      this.submitting.set(false);
    }
  }

  cancel(): void {
    this.dialogRef.close(false);
  }
}
