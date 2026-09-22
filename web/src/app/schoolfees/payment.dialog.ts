import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { fieldError } from '../core/form-error.util';
import { extractErrorMessage } from '../core/http-error.util';
import { formatMoney } from '../core/money.util';
import { FeeOutstanding, PAYMENT_METHODS, SchoolFeesService } from './school-fees.service';

export interface PaymentDialogData {
  entry: FeeOutstanding;
}

/**
 * Encaissement d'un règlement. Le montant est pré-rempli avec le solde restant mais reste
 * modifiable : un règlement partiel est le cas courant, pas l'exception. Le moyen de paiement
 * et la référence sont saisis ici — l'écran ne proposait jusqu'ici que « encaisser le solde
 * en espèces », ce qui obligeait à mentir sur la moitié des règlements.
 */
@Component({
  selector: 'app-payment-dialog',
  imports: [
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatProgressSpinnerModule,
  ],
  template: `
    <h2 mat-dialog-title>Encaisser un règlement</h2>
    <form [formGroup]="form" (ngSubmit)="submit()">
      <mat-dialog-content>
        <p class="payment-context">
          <strong>{{ data.entry.studentName }}</strong> · {{ data.entry.className }}<br />
          {{ data.entry.scheduleLabel }} — reste
          <strong>{{ money(data.entry.amountRemainingCents) }}</strong>
        </p>

        <mat-form-field appearance="outline">
          <mat-label>Montant reçu</mat-label>
          <input matInput type="number" formControlName="amountCents" />
          <mat-hint>Laissez le montant proposé pour solder, ou saisissez un acompte.</mat-hint>
          <mat-error>{{ fieldError(form.controls.amountCents) }}</mat-error>
        </mat-form-field>

        <mat-form-field appearance="outline">
          <mat-label>Moyen de paiement</mat-label>
          <mat-select formControlName="method">
            @for (method of methods; track method.value) {
              <mat-option [value]="method.value">{{ method.label }}</mat-option>
            }
          </mat-select>
          <mat-error>{{ fieldError(form.controls.method) }}</mat-error>
        </mat-form-field>

        <mat-form-field appearance="outline">
          <mat-label>Référence</mat-label>
          <input matInput formControlName="reference" />
          <mat-hint>Numéro de transaction, de reçu ou de bordereau. Facultatif.</mat-hint>
        </mat-form-field>

        @if (errorMessage()) {
          <p class="error-message">{{ errorMessage() }}</p>
        }
      </mat-dialog-content>

      <mat-dialog-actions align="end">
        <button mat-button type="button" (click)="cancel()">Annuler</button>
        <button mat-flat-button type="submit" [disabled]="submitting()">
          @if (submitting()) {
            <mat-spinner diameter="20" />
          } @else {
            Enregistrer le règlement
          }
        </button>
      </mat-dialog-actions>
    </form>
  `,
  styles: `
    .payment-context {
      margin: 0 0 var(--space-4);
      padding: var(--space-3) var(--space-4);
      border-radius: var(--radius);
      background: var(--color-surface-muted, var(--tenant-primary-soft));
      line-height: 1.6;
    }
  `,
})
export class PaymentDialog {
  protected readonly fieldError = fieldError;
  protected readonly money = formatMoney;
  protected readonly methods = PAYMENT_METHODS;

  private readonly formBuilder = inject(FormBuilder);
  private readonly schoolFeesService = inject(SchoolFeesService);
  private readonly dialogRef = inject(MatDialogRef<PaymentDialog, boolean>);
  protected readonly data = inject<PaymentDialogData>(MAT_DIALOG_DATA);

  protected readonly submitting = signal(false);
  protected readonly errorMessage = signal<string | null>(null);

  protected readonly form = this.formBuilder.nonNullable.group({
    amountCents: [
      this.data.entry.amountRemainingCents,
      // Le plafond reprend le solde restant : le serveur refuse de toute façon un
      // trop-perçu, autant le dire avant l'aller-retour.
      [Validators.required, Validators.min(1), Validators.max(this.data.entry.amountRemainingCents)],
    ],
    method: ['CASH', [Validators.required]],
    reference: [''],
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
      await this.schoolFeesService.recordPayment(
        this.data.entry.invoiceId,
        value.amountCents,
        value.method,
        value.reference.trim() || null,
      );
      this.dialogRef.close(true);
    } catch (error) {
      this.errorMessage.set(extractErrorMessage(error));
    } finally {
      this.submitting.set(false);
    }
  }

  cancel(): void {
    this.dialogRef.close();
  }
}
