import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { extractErrorMessage } from '../core/http-error.util';
import { StaffAccount } from './staff-account.model';
import { StaffAccountService } from './staff-account.service';
import { fieldError } from '../core/form-error.util';

export interface ResetPasswordDialogData {
  account: StaffAccount;
}

@Component({
  selector: 'app-reset-password-dialog',
  imports: [
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatProgressSpinnerModule,
  ],
  template: `
    <h2 mat-dialog-title>Réinitialiser le mot de passe</h2>
    <form [formGroup]="form" (ngSubmit)="submit()">
      <mat-dialog-content>
        <p class="dialog-lead">
          Nouveau mot de passe de {{ data.account.firstName }} {{ data.account.lastName }} ({{
            data.account.email
          }}).
        </p>

        <mat-form-field appearance="outline">
          <mat-label>Nouveau mot de passe</mat-label>
          <input matInput type="password" formControlName="password" autocomplete="new-password" />
          <mat-hint>8 caractères minimum. À communiquer à la personne concernée.</mat-hint>
          <mat-error>{{ fieldError(form.controls.password) }}</mat-error>
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
            Réinitialiser
          }
        </button>
      </mat-dialog-actions>
    </form>
  `,
  styles: `
    .dialog-lead {
      margin: 0 0 var(--space-4);
      color: var(--color-text-secondary);
      font-size: var(--font-size-small);
    }

    mat-form-field {
      width: 100%;
    }
  `,
})
export class ResetPasswordDialog {
  protected readonly fieldError = fieldError;
  private readonly formBuilder = inject(FormBuilder);
  private readonly accountService = inject(StaffAccountService);
  private readonly dialogRef = inject(MatDialogRef<ResetPasswordDialog, boolean>);
  protected readonly data = inject<ResetPasswordDialogData>(MAT_DIALOG_DATA);

  protected readonly submitting = signal(false);
  protected readonly errorMessage = signal<string | null>(null);

  protected readonly form = this.formBuilder.nonNullable.group({
    password: ['', [Validators.required, Validators.minLength(8), Validators.maxLength(100)]],
  });

  async submit(): Promise<void> {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.submitting.set(true);
    this.errorMessage.set(null);
    try {
      await this.accountService.resetPassword(
        this.data.account.id,
        this.form.getRawValue().password,
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
