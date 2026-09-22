import { FRENCH_DATE_LOCALE } from '../core/date-locale.provider';
import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatIconModule } from '@angular/material/icon';
import { RouterLink } from '@angular/router';
import { extractErrorMessage } from '../core/http-error.util';
import { SchoolClass } from '../schoolclass/school-class.model';
import { SchoolClassService } from '../schoolclass/school-class.service';
import { Student } from '../student/student.model';
import { StudentService } from '../student/student.service';
import { ATTENDANCE_STATUSES, AttendanceStatus } from './attendance.model';
import { AttendanceService } from './attendance.service';

interface RollCallRow {
  studentId: number;
  studentLabel: string;
  status: AttendanceStatus;
  reason: string;
  justified: boolean;
  comment: string;
}

/**
 * Feuille d'appel (cahier-des-charges.md §10, mockup docs/MOCKUPS.md §1). Mockup pensé
 * Mobile/Ionic, adapté ici en Web (Angular Material) : l'app Mobile n'a pas encore
 * d'authentification (voir docs/SESSION_LOG.md) — construire l'écran Ionic maintenant
 * signifierait le laisser inutilisable. Même principe conservé : tout coché "Présent" par
 * défaut, l'utilisateur ne touche que les exceptions.
 */
@Component({
  selector: 'app-roll-call-page',
  imports: [
    FormsModule,
    MatFormFieldModule,
    MatSelectModule,
    MatDatepickerModule,
    MatButtonToggleModule,
    MatButtonModule,
    MatInputModule,
    MatIconModule,
    RouterLink,
  ],
  providers: [FRENCH_DATE_LOCALE],
  templateUrl: './roll-call.page.html',
  styleUrl: './roll-call.page.scss',
})
export class RollCallPage {
  private readonly schoolClassService = inject(SchoolClassService);
  private readonly studentService = inject(StudentService);
  private readonly attendanceService = inject(AttendanceService);

  protected readonly classes = signal<SchoolClass[]>([]);
  protected readonly selectedClassId = signal<number | null>(null);
  protected readonly selectedDate = signal<Date>(new Date());
  protected readonly rows = signal<RollCallRow[]>([]);
  protected readonly loading = signal(false);
  protected readonly submitting = signal(false);
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly success = signal(false);
  protected readonly statuses = ATTENDANCE_STATUSES;

  constructor() {
    void this.schoolClassService.list().then((classes) => this.classes.set(classes));
  }

  async onClassOrDateChange(): Promise<void> {
    this.success.set(false);
    const classId = this.selectedClassId();
    if (classId === null) {
      this.rows.set([]);
      return;
    }

    this.loading.set(true);
    try {
      const dateIso = this.toIsoDate(this.selectedDate());
      const [students, existingRecords] = await Promise.all([
        this.studentService.list(),
        this.attendanceService.listForClassAndDate(classId, dateIso),
      ]);
      const existingByStudent = new Map(existingRecords.map((r) => [r.studentId, r]));

      this.rows.set(
        students
          .filter((s: Student) => s.schoolClassId === classId && s.active)
          .map((student) => {
            const existing = existingByStudent.get(student.id);
            return {
              studentId: student.id,
              studentLabel: `${student.firstName} ${student.lastName}`,
              status: existing?.status ?? 'PRESENT',
              reason: existing?.reason ?? '',
              justified: existing?.justified ?? false,
              comment: existing?.comment ?? '',
            };
          }),
      );
    } finally {
      this.loading.set(false);
    }
  }

  async submit(): Promise<void> {
    const classId = this.selectedClassId();
    if (classId === null || this.rows().length === 0) {
      return;
    }

    this.submitting.set(true);
    this.errorMessage.set(null);
    try {
      await this.attendanceService.submitRollCall({
        schoolClassId: classId,
        date: this.toIsoDate(this.selectedDate()),
        entries: this.rows().map((row) => ({
          studentId: row.studentId,
          status: row.status,
          reason: row.status === 'PRESENT' ? null : row.reason || null,
          justified: row.justified,
          comment: row.comment || null,
        })),
      });
      this.success.set(true);
    } catch (error) {
      this.errorMessage.set(extractErrorMessage(error));
    } finally {
      this.submitting.set(false);
    }
  }

  private toIsoDate(date: Date): string {
    return date.toISOString().slice(0, 10);
  }
}
