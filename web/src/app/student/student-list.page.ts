import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatChipsModule } from '@angular/material/chips';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { confirmAction } from '../core/confirm-dialog.component';
import { StudentFormDialog } from './student-form.dialog';
import { Student } from './student.model';
import { StudentService } from './student.service';

@Component({
  selector: 'app-student-list-page',
  imports: [MatTableModule, MatButtonModule, MatIconModule, MatChipsModule, MatDialogModule, RouterLink],
  templateUrl: './student-list.page.html',
  styleUrl: './student-list.page.scss',
})
export class StudentListPage {
  private readonly studentService = inject(StudentService);
  private readonly dialog = inject(MatDialog);

  protected readonly students = signal<Student[]>([]);
  protected readonly loading = signal(false);
  protected readonly columns = ['studentNumber', 'firstName', 'lastName', 'schoolClassId', 'active', 'actions'];

  constructor() {
    void this.refresh();
  }

  async refresh(): Promise<void> {
    this.loading.set(true);
    try {
      this.students.set(await this.studentService.list());
    } finally {
      this.loading.set(false);
    }
  }

  openCreateDialog(): void {
    this.dialog
      .open(StudentFormDialog, { data: {}, width: '480px' })
      .afterClosed()
      .subscribe((result) => result && void this.refresh());
  }

  openEditDialog(student: Student): void {
    this.dialog
      .open(StudentFormDialog, { data: { student }, width: '480px' })
      .afterClosed()
      .subscribe((result) => result && void this.refresh());
  }

  async deactivate(student: Student): Promise<void> {
    const confirmed = await confirmAction(this.dialog, {
      title: 'Désactiver cet élève ?',
      message: `${student.firstName} ${student.lastName} n'apparaîtra plus comme actif.`,
    });
    if (!confirmed) {
      return;
    }
    await this.studentService.deactivate(student.id);
    await this.refresh();
  }
}
