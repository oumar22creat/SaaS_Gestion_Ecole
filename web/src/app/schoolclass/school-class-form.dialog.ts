import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { extractErrorMessage } from '../core/http-error.util';
import { Teacher } from '../teacher/teacher.model';
import { TeacherService } from '../teacher/teacher.service';
import { SchoolClass } from './school-class.model';
import { SchoolClassService } from './school-class.service';

export interface SchoolClassFormDialogData {
  schoolClass?: SchoolClass;
}

@Component({
  selector: 'app-school-class-form-dialog',
  imports: [
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './school-class-form.dialog.html',
  styleUrl: './school-class-form.dialog.scss',
})
export class SchoolClassFormDialog {
  private readonly formBuilder = inject(FormBuilder);
  private readonly schoolClassService = inject(SchoolClassService);
  private readonly teacherService = inject(TeacherService);
  private readonly dialogRef = inject(MatDialogRef<SchoolClassFormDialog, SchoolClass | undefined>);
  protected readonly data = inject<SchoolClassFormDialogData>(MAT_DIALOG_DATA);

  protected readonly submitting = signal(false);
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly teachers = signal<Teacher[]>([]);

  protected readonly form = this.formBuilder.nonNullable.group({
    name: [this.data.schoolClass?.name ?? '', [Validators.required, Validators.maxLength(100)]],
    headTeacherId: [this.data.schoolClass?.headTeacherId ?? null],
  });

  constructor() {
    void this.teacherService.list().then((teachers) => this.teachers.set(teachers));
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
      const result = this.data.schoolClass
        ? await this.schoolClassService.update(this.data.schoolClass.id, value)
        : await this.schoolClassService.create(value);
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
