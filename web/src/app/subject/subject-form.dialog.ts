import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { extractErrorMessage } from '../core/http-error.util';
import { Subject } from './subject.model';
import { SubjectService } from './subject.service';
import { fieldError } from '../core/form-error.util';

export interface SubjectFormDialogData {
  subject?: Subject;
}

@Component({
  selector: 'app-subject-form-dialog',
  imports: [
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './subject-form.dialog.html',
  styleUrl: './subject-form.dialog.scss',
})
export class SubjectFormDialog {
  protected readonly fieldError = fieldError;
  private readonly formBuilder = inject(FormBuilder);
  private readonly subjectService = inject(SubjectService);
  private readonly dialogRef = inject(MatDialogRef<SubjectFormDialog, Subject | undefined>);
  protected readonly data = inject<SubjectFormDialogData>(MAT_DIALOG_DATA);

  protected readonly submitting = signal(false);
  protected readonly errorMessage = signal<string | null>(null);

  protected readonly form = this.formBuilder.nonNullable.group({
    name: [this.data.subject?.name ?? '', [Validators.required, Validators.maxLength(100)]],
    code: [this.data.subject?.code ?? '', [Validators.required, Validators.maxLength(32)]],
    coefficient: [this.data.subject?.coefficient ?? 1, [Validators.required, Validators.min(1)]],
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
      const result = this.data.subject
        ? await this.subjectService.update(this.data.subject.id, value)
        : await this.subjectService.create(value);
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
