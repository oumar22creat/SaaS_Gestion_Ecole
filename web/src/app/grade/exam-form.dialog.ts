import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatNativeDateModule } from '@angular/material/core';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { extractErrorMessage } from '../core/http-error.util';
import { SchoolClass } from '../schoolclass/school-class.model';
import { SchoolClassService } from '../schoolclass/school-class.service';
import { Subject } from '../subject/subject.model';
import { SubjectService } from '../subject/subject.service';
import { Exam } from './grade.model';
import { ExamService } from './exam.service';
import { fieldError } from '../core/form-error.util';

@Component({
  selector: 'app-exam-form-dialog',
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
  templateUrl: './exam-form.dialog.html',
  styleUrl: './exam-form.dialog.scss',
})
export class ExamFormDialog {
  protected readonly fieldError = fieldError;
  private readonly formBuilder = inject(FormBuilder);
  private readonly examService = inject(ExamService);
  private readonly dialogRef = inject(MatDialogRef<ExamFormDialog, Exam | undefined>);

  protected readonly submitting = signal(false);
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly classes = signal<SchoolClass[]>([]);
  protected readonly subjects = signal<Subject[]>([]);

  protected readonly form = this.formBuilder.nonNullable.group({
    schoolClassId: [null as number | null, [Validators.required]],
    subjectId: [null as number | null, [Validators.required]],
    label: ['', [Validators.required, Validators.maxLength(255)]],
    maxScore: [20, [Validators.required, Validators.min(1)]],
    coefficient: [1, [Validators.required, Validators.min(1)]],
    examDate: [new Date(), [Validators.required]],
  });

  constructor(schoolClassService: SchoolClassService, subjectService: SubjectService) {
    void schoolClassService.list().then((classes) => this.classes.set(classes));
    void subjectService.list().then((subjects) => this.subjects.set(subjects));
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
      const result = await this.examService.create({
        schoolClassId: value.schoolClassId!,
        subjectId: value.subjectId!,
        label: value.label,
        maxScore: value.maxScore,
        coefficient: value.coefficient,
        examDate: value.examDate.toISOString().slice(0, 10),
      });
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
