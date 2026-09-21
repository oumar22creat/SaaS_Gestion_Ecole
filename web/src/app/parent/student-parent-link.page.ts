import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatTableModule } from '@angular/material/table';
import { confirmAction } from '../core/confirm-dialog.component';
import { extractErrorMessage } from '../core/http-error.util';
import { Student } from '../student/student.model';
import { StudentService } from '../student/student.service';
import { ParentService } from './parent.service';
import { fieldError } from '../core/form-error.util';

/** Association élève/parent, vue depuis un parent — cahier-des-charges.md §7, ROADMAP.md 1.5. */
@Component({
  selector: 'app-student-parent-link-page',
  imports: [
    ReactiveFormsModule,
    RouterLink,
    MatTableModule,
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatCheckboxModule,
    MatDialogModule,
  ],
  templateUrl: './student-parent-link.page.html',
  styleUrl: './student-parent-link.page.scss',
})
export class StudentParentLinkPage {
  protected readonly fieldError = fieldError;
  private readonly route = inject(ActivatedRoute);
  private readonly parentService = inject(ParentService);
  private readonly studentService = inject(StudentService);
  private readonly dialog = inject(MatDialog);
  private readonly formBuilder = inject(FormBuilder);

  protected readonly parentId = Number(this.route.snapshot.paramMap.get('parentId'));
  protected readonly childrenIds = signal<number[]>([]);
  protected readonly students = signal<Student[]>([]);
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly columns = ['student', 'actions'];

  protected readonly form = this.formBuilder.nonNullable.group({
    studentId: [null as number | null, [Validators.required]],
    relationship: ['', [Validators.required]],
    primaryContact: [false],
  });

  constructor() {
    void this.refresh();
    void this.studentService.list().then((students) => this.students.set(students));
  }

  async refresh(): Promise<void> {
    this.childrenIds.set(await this.parentService.childrenOf(this.parentId));
  }

  studentName(studentId: number): string {
    const student = this.students().find((s) => s.id === studentId);
    return student
      ? `${student.firstName} ${student.lastName} (${student.studentNumber})`
      : `#${studentId}`;
  }

  async link(): Promise<void> {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.errorMessage.set(null);
    try {
      const value = this.form.getRawValue();
      await this.parentService.link(value.studentId!, {
        parentId: this.parentId,
        relationship: value.relationship,
        primaryContact: value.primaryContact,
      });
      this.form.reset({ primaryContact: false });
      await this.refresh();
    } catch (error) {
      this.errorMessage.set(extractErrorMessage(error));
    }
  }

  async unlink(studentId: number): Promise<void> {
    const confirmed = await confirmAction(this.dialog, {
      title: 'Retirer cette association ?',
      message: `${this.studentName(studentId)} ne sera plus associé à ce parent.`,
    });
    if (!confirmed) {
      return;
    }
    await this.parentService.unlink(studentId, this.parentId);
    await this.refresh();
  }
}
