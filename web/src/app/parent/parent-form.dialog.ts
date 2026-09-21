import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { extractErrorMessage } from '../core/http-error.util';
import { Parent } from './parent.model';
import { ParentService } from './parent.service';
import { fieldError } from '../core/form-error.util';

export interface ParentFormDialogData {
  parent?: Parent;
}

@Component({
  selector: 'app-parent-form-dialog',
  imports: [
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './parent-form.dialog.html',
  styleUrl: './parent-form.dialog.scss',
})
export class ParentFormDialog {
  protected readonly fieldError = fieldError;
  private readonly formBuilder = inject(FormBuilder);
  private readonly parentService = inject(ParentService);
  private readonly dialogRef = inject(MatDialogRef<ParentFormDialog, Parent | undefined>);
  protected readonly data = inject<ParentFormDialogData>(MAT_DIALOG_DATA);

  protected readonly submitting = signal(false);
  protected readonly errorMessage = signal<string | null>(null);

  protected readonly form = this.formBuilder.nonNullable.group({
    firstName: [
      this.data.parent?.firstName ?? '',
      [Validators.required, Validators.maxLength(100)],
    ],
    lastName: [this.data.parent?.lastName ?? '', [Validators.required, Validators.maxLength(100)]],
    email: [this.data.parent?.email ?? '', [Validators.email]],
    phone: [this.data.parent?.phone ?? ''],
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
      const request = { ...value, email: value.email || null, phone: value.phone || null };
      const result = this.data.parent
        ? await this.parentService.update(this.data.parent.id, request)
        : await this.parentService.create(request);
      this.dialogRef.close(result);
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
