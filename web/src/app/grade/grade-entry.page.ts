import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatTableModule } from '@angular/material/table';
import { extractErrorMessage } from '../core/http-error.util';
import { Student } from '../student/student.model';
import { StudentService } from '../student/student.service';
import { Exam, ExamStatistics } from './grade.model';
import { ExamService } from './exam.service';
import { GradeService } from './grade.service';

interface GradeRow {
  studentId: number;
  studentLabel: string;
  score: number | null;
  absent: boolean;
  comment: string;
}

/**
 * Saisie de notes (cahier-des-charges.md §11, mockup docs/MOCKUPS.md §2 — Web, Angular
 * Material). Tabulation verticale entre les champs de note : ordre naturel du mat-table,
 * pas besoin de cliquer chaque champ à la souris.
 */
@Component({
  selector: 'app-grade-entry-page',
  imports: [FormsModule, RouterLink, MatTableModule, MatFormFieldModule, MatInputModule, MatCheckboxModule, MatButtonModule],
  templateUrl: './grade-entry.page.html',
  styleUrl: './grade-entry.page.scss',
})
export class GradeEntryPage {
  private readonly route = inject(ActivatedRoute);
  private readonly examService = inject(ExamService);
  private readonly gradeService = inject(GradeService);
  private readonly studentService = inject(StudentService);

  protected readonly examId = Number(this.route.snapshot.paramMap.get('examId'));
  protected readonly exam = signal<Exam | null>(null);
  protected readonly rows = signal<GradeRow[]>([]);
  protected readonly columns = ['student', 'score', 'absent', 'comment'];
  protected readonly submitting = signal(false);
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly statistics = signal<ExamStatistics | null>(null);

  constructor() {
    void this.load();
  }

  private async load(): Promise<void> {
    const [exam, students, grades] = await Promise.all([
      this.examService.getById(this.examId),
      this.studentService.list(),
      this.gradeService.listForExam(this.examId),
    ]);
    this.exam.set(exam);
    const gradesByStudent = new Map(grades.map((g) => [g.studentId, g]));

    this.rows.set(
      students
        .filter((s: Student) => s.schoolClassId === exam.schoolClassId && s.active)
        .map((student) => {
          const grade = gradesByStudent.get(student.id);
          return {
            studentId: student.id,
            studentLabel: `${student.firstName} ${student.lastName}`,
            score: grade?.score ?? null,
            absent: grade?.absent ?? false,
            comment: grade?.comment ?? '',
          };
        }),
    );
  }

  async submit(): Promise<void> {
    this.submitting.set(true);
    this.errorMessage.set(null);
    try {
      await this.gradeService.submit(
        this.examId,
        this.rows().map((row) => ({
          studentId: row.studentId,
          score: row.absent ? null : row.score,
          absent: row.absent,
          comment: row.comment || null,
        })),
      );
      this.statistics.set(await this.examService.statistics(this.examId));
    } catch (error) {
      this.errorMessage.set(extractErrorMessage(error));
    } finally {
      this.submitting.set(false);
    }
  }
}
