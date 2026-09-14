import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatSelectModule } from '@angular/material/select';
import { MatTableModule } from '@angular/material/table';
import { confirmAction } from '../core/confirm-dialog.component';
import { extractErrorMessage } from '../core/http-error.util';
import { Subject } from '../subject/subject.model';
import { SubjectService } from '../subject/subject.service';
import { Teacher } from '../teacher/teacher.model';
import { TeacherService } from '../teacher/teacher.service';
import { ClassSubjectAssignment } from './school-class.model';
import { SchoolClassService } from './school-class.service';

/** Affectation enseignant/matière pour une classe — cahier-des-charges.md §8, ROADMAP.md 1.5. */
@Component({
  selector: 'app-class-subject-assignment-page',
  imports: [
    ReactiveFormsModule,
    RouterLink,
    MatTableModule,
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatSelectModule,
    MatDialogModule,
  ],
  templateUrl: './class-subject-assignment.page.html',
  styleUrl: './class-subject-assignment.page.scss',
})
export class ClassSubjectAssignmentPage {
  private readonly route = inject(ActivatedRoute);
  private readonly schoolClassService = inject(SchoolClassService);
  private readonly subjectService = inject(SubjectService);
  private readonly teacherService = inject(TeacherService);
  private readonly dialog = inject(MatDialog);
  private readonly formBuilder = inject(FormBuilder);

  protected readonly classId = Number(this.route.snapshot.paramMap.get('classId'));
  protected readonly assignments = signal<ClassSubjectAssignment[]>([]);
  protected readonly subjects = signal<Subject[]>([]);
  protected readonly teachers = signal<Teacher[]>([]);
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly columns = ['subjectId', 'teacherId', 'actions'];

  protected readonly form = this.formBuilder.nonNullable.group({
    subjectId: [null as number | null, [Validators.required]],
    teacherId: [null as number | null, [Validators.required]],
  });

  constructor() {
    void this.refresh();
    void this.subjectService.list().then((subjects) => this.subjects.set(subjects));
    void this.teacherService.list().then((teachers) => this.teachers.set(teachers));
  }

  async refresh(): Promise<void> {
    this.assignments.set(await this.schoolClassService.listAssignments(this.classId));
  }

  subjectName(subjectId: number): string {
    return this.subjects().find((s) => s.id === subjectId)?.name ?? `#${subjectId}`;
  }

  teacherName(teacherId: number): string {
    const teacher = this.teachers().find((t) => t.id === teacherId);
    return teacher ? `${teacher.firstName} ${teacher.lastName}` : `#${teacherId}`;
  }

  async assign(): Promise<void> {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.errorMessage.set(null);
    try {
      const value = this.form.getRawValue();
      await this.schoolClassService.assign(this.classId, { subjectId: value.subjectId!, teacherId: value.teacherId! });
      this.form.reset();
      await this.refresh();
    } catch (error) {
      this.errorMessage.set(extractErrorMessage(error));
    }
  }

  async unassign(assignment: ClassSubjectAssignment): Promise<void> {
    const confirmed = await confirmAction(this.dialog, {
      title: 'Retirer cette affectation ?',
      message: `${this.teacherName(assignment.teacherId)} n'enseignera plus ${this.subjectName(assignment.subjectId)} à cette classe.`,
    });
    if (!confirmed) {
      return;
    }
    await this.schoolClassService.unassign(this.classId, assignment.subjectId);
    await this.refresh();
  }
}
