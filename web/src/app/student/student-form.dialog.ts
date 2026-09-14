import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatNativeDateModule } from '@angular/material/core';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { extractErrorMessage } from '../core/http-error.util';
import { SchoolClass } from '../schoolclass/school-class.model';
import { SchoolClassService } from '../schoolclass/school-class.service';
import { Student } from './student.model';
import { StudentService } from './student.service';

export interface StudentFormDialogData {
  student?: Student;
}

@Component({
  selector: 'app-student-form-dialog',
  imports: [
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatDatepickerModule,
    MatNativeDateModule,
    MatButtonModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './student-form.dialog.html',
  styleUrl: './student-form.dialog.scss',
})
export class StudentFormDialog {
  private readonly formBuilder = inject(FormBuilder);
  private readonly studentService = inject(StudentService);
  private readonly schoolClassService = inject(SchoolClassService);
  private readonly dialogRef = inject(MatDialogRef<StudentFormDialog, Student | undefined>);
  protected readonly data = inject<StudentFormDialogData>(MAT_DIALOG_DATA);

  protected readonly submitting = signal(false);
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly classes = signal<SchoolClass[]>([]);

  protected readonly form = this.formBuilder.nonNullable.group({
    studentNumber: [this.data.student?.studentNumber ?? '', [Validators.required, Validators.maxLength(64)]],
    firstName: [this.data.student?.firstName ?? '', [Validators.required, Validators.maxLength(100)]],
    lastName: [this.data.student?.lastName ?? '', [Validators.required, Validators.maxLength(100)]],
    birthDate: [this.data.student?.birthDate ? new Date(this.data.student.birthDate) : (null as Date | null)],
    gender: [this.data.student?.gender ?? ''],
    schoolClassId: [this.data.student?.schoolClassId ?? null],
  });

  constructor() {
    void this.schoolClassService.list().then((classes) => this.classes.set(classes));
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
      const request = {
        ...value,
        gender: value.gender || null,
        birthDate: value.birthDate ? value.birthDate.toISOString().slice(0, 10) : null,
      };
      const result = this.data.student
        ? await this.studentService.update(this.data.student.id, request)
        : await this.studentService.create(request);
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
