import { FRENCH_DATE_LOCALE } from '../core/date-locale.provider';
import { Component, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
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
import { TeachingScope, TeachingScopeService } from '../core/teaching-scope.service';

@Component({
  selector: 'app-exam-form-dialog',
  imports: [
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatDatepickerModule,
    MatButtonModule,
    MatProgressSpinnerModule,
  ],
  providers: [FRENCH_DATE_LOCALE],
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
  private readonly allClasses = signal<SchoolClass[]>([]);
  private readonly allSubjects = signal<Subject[]>([]);
  private readonly selectedClassId = signal<number | null>(null);
  /** Périmètre d'affichage (ADR-030) ; fermé tant qu'il n'est pas chargé. */
  private readonly scope = signal(new TeachingScope(null, true));

  /** Un enseignant ne voit que les classes où il a une matière, puis ses matières de la classe. */
  protected readonly classes = computed(() => this.scope().classesWithSubjects(this.allClasses()));
  protected readonly restricted = computed(() => this.scope().restricted);
  protected readonly subjects = computed(() =>
    this.scope().subjectsFor(this.selectedClassId(), this.allSubjects()),
  );

  protected readonly form = this.formBuilder.nonNullable.group({
    schoolClassId: [null as number | null, [Validators.required]],
    subjectId: [null as number | null, [Validators.required]],
    label: ['', [Validators.required, Validators.maxLength(255)]],
    maxScore: [20, [Validators.required, Validators.min(1)]],
    coefficient: [1, [Validators.required, Validators.min(1)]],
    examDate: [new Date(), [Validators.required]],
  });

  constructor(
    schoolClassService: SchoolClassService,
    subjectService: SubjectService,
    teachingScopeService: TeachingScopeService,
  ) {
    void schoolClassService.list().then((classes) => this.allClasses.set(classes));
    void subjectService.list().then((subjects) => this.allSubjects.set(subjects));
    void teachingScopeService.current().then((scope) => this.scope.set(scope));
    this.form.controls.schoolClassId.valueChanges
      .pipe(takeUntilDestroyed())
      .subscribe((classId) => {
        this.selectedClassId.set(classId);
        // Une matière choisie pour une autre classe n'est peut-être pas enseignée dans celle-ci.
        const subjectId = this.form.controls.subjectId.value;
        if (subjectId !== null && !this.subjects().some((subject) => subject.id === subjectId)) {
          this.form.controls.subjectId.setValue(null);
        }
      });
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
