import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { extractErrorMessage } from '../core/http-error.util';
import { Teacher } from './teacher.model';
import { TeacherService } from './teacher.service';
import { fieldError } from '../core/form-error.util';

export interface TeacherFormDialogData {
  teacher?: Teacher;
}

@Component({
  selector: 'app-teacher-form-dialog',
  imports: [
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './teacher-form.dialog.html',
  styleUrl: './teacher-form.dialog.scss',
})
export class TeacherFormDialog {
  protected readonly fieldError = fieldError;
  private readonly formBuilder = inject(FormBuilder);
  private readonly teacherService = inject(TeacherService);
  private readonly dialogRef = inject(MatDialogRef<TeacherFormDialog, Teacher | undefined>);
  protected readonly data = inject<TeacherFormDialogData>(MAT_DIALOG_DATA);

  protected readonly submitting = signal(false);
  protected readonly errorMessage = signal<string | null>(null);

  protected readonly form = this.formBuilder.nonNullable.group({
    firstName: [
      this.data.teacher?.firstName ?? '',
      [Validators.required, Validators.maxLength(100)],
    ],
    lastName: [this.data.teacher?.lastName ?? '', [Validators.required, Validators.maxLength(100)]],
    email: [this.data.teacher?.email ?? '', [Validators.email]],
    phone: [this.data.teacher?.phone ?? ''],
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
      const result = this.data.teacher
        ? await this.teacherService.update(this.data.teacher.id, request)
        : await this.teacherService.create(request);
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
