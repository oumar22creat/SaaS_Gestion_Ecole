import { FRENCH_DATE_LOCALE } from '../core/date-locale.provider';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { extractErrorMessage } from '../core/http-error.util';
import { SchoolClass } from '../schoolclass/school-class.model';
import { SchoolClassService } from '../schoolclass/school-class.service';
import { GENDERS, Student } from './student.model';
import { StudentService } from './student.service';
import { fieldError } from '../core/form-error.util';

export interface StudentFormDialogData {
  student?: Student;
}

/**
 * Le sélecteur de date produit un Date à minuit dans le fuseau local. `toISOString()` le
 * convertirait en UTC : à l'est de Greenwich, minuit local tombe la veille en UTC et la date
 * de naissance serait enregistrée avec un jour de moins. On formate donc depuis les
 * composantes locales.
 */
function toIsoDate(date: Date | null): string | null {
  if (!date) {
    return null;
  }
  const month = `${date.getMonth() + 1}`.padStart(2, '0');
  const day = `${date.getDate()}`.padStart(2, '0');
  return `${date.getFullYear()}-${month}-${day}`;
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
    MatButtonModule,
    MatProgressSpinnerModule,
  ],
  providers: [FRENCH_DATE_LOCALE],
  templateUrl: './student-form.dialog.html',
  styleUrl: './student-form.dialog.scss',
})
export class StudentFormDialog {
  protected readonly fieldError = fieldError;
  private readonly formBuilder = inject(FormBuilder);
  private readonly studentService = inject(StudentService);
  private readonly schoolClassService = inject(SchoolClassService);
  private readonly dialogRef = inject(MatDialogRef<StudentFormDialog, Student | undefined>);
  protected readonly data = inject<StudentFormDialogData>(MAT_DIALOG_DATA);

  protected readonly submitting = signal(false);
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly classes = signal<SchoolClass[]>([]);

  /**
   * Le sexe était un champ libre : des fiches importées par CSV peuvent porter une valeur
   * absente de GENDERS. Une liste déroulante n'afficherait alors rien, et la fiche
   * repartirait vidée de son sexe à la première modification. La valeur déjà enregistrée
   * est donc ajoutée aux choix, telle quelle.
   */
  protected genderOptions(): { value: string; label: string }[] {
    const current = this.data.student?.gender;
    if (!current || GENDERS.some((gender) => gender.value === current)) {
      return GENDERS;
    }
    return [...GENDERS, { value: current, label: current }];
  }

  protected readonly form = this.formBuilder.nonNullable.group({
    studentNumber: [
      this.data.student?.studentNumber ?? '',
      [Validators.required, Validators.maxLength(64)],
    ],
    firstName: [
      this.data.student?.firstName ?? '',
      [Validators.required, Validators.maxLength(100)],
    ],
    lastName: [this.data.student?.lastName ?? '', [Validators.required, Validators.maxLength(100)]],
    birthDate: [
      this.data.student?.birthDate ? new Date(this.data.student.birthDate) : (null as Date | null),
    ],
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
        birthDate: toIsoDate(value.birthDate),
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
