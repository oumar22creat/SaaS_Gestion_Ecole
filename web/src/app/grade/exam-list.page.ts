import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { confirmAction } from '../core/confirm-dialog.component';
import { SchoolClass } from '../schoolclass/school-class.model';
import { SchoolClassService } from '../schoolclass/school-class.service';
import { Subject } from '../subject/subject.model';
import { SubjectService } from '../subject/subject.service';
import { ExamFormDialog } from './exam-form.dialog';
import { Exam } from './grade.model';
import { ExamService } from './exam.service';

@Component({
  selector: 'app-exam-list-page',
  imports: [MatTableModule, MatButtonModule, MatIconModule, MatDialogModule, RouterLink],
  templateUrl: './exam-list.page.html',
  styleUrl: './exam-list.page.scss',
})
export class ExamListPage {
  private readonly examService = inject(ExamService);
  private readonly dialog = inject(MatDialog);
  private readonly schoolClassService = inject(SchoolClassService);
  private readonly subjectService = inject(SubjectService);

  protected readonly exams = signal<Exam[]>([]);
  protected readonly classes = signal<SchoolClass[]>([]);
  protected readonly subjects = signal<Subject[]>([]);
  protected readonly loading = signal(false);
  protected readonly columns = ['examDate', 'label', 'schoolClassId', 'subjectId', 'coefficient', 'actions'];

  constructor() {
    void this.refresh();
    void this.schoolClassService.list().then((classes) => this.classes.set(classes));
    void this.subjectService.list().then((subjects) => this.subjects.set(subjects));
  }

  async refresh(): Promise<void> {
    this.loading.set(true);
    try {
      this.exams.set(await this.examService.list());
    } finally {
      this.loading.set(false);
    }
  }

  className(id: number): string {
    return this.classes().find((c) => c.id === id)?.name ?? `#${id}`;
  }

  subjectName(id: number): string {
    return this.subjects().find((s) => s.id === id)?.name ?? `#${id}`;
  }

  openCreateDialog(): void {
    this.dialog
      .open(ExamFormDialog, { width: '480px' })
      .afterClosed()
      .subscribe((result) => result && void this.refresh());
  }

  async remove(exam: Exam): Promise<void> {
    const confirmed = await confirmAction(this.dialog, {
      title: 'Supprimer cette évaluation ?',
      message: `"${exam.label}" et toutes ses notes seront définitivement supprimées.`,
    });
    if (!confirmed) {
      return;
    }
    await this.examService.remove(exam.id);
    await this.refresh();
  }
}
