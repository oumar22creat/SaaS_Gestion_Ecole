import { Component, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { confirmAction } from '../core/confirm-dialog.component';
import { SubjectFormDialog } from './subject-form.dialog';
import { Subject } from './subject.model';
import { SubjectService } from './subject.service';

@Component({
  selector: 'app-subject-list-page',
  imports: [MatTableModule, MatButtonModule, MatIconModule, MatDialogModule],
  templateUrl: './subject-list.page.html',
  styleUrl: './subject-list.page.scss',
})
export class SubjectListPage {
  private readonly subjectService = inject(SubjectService);
  private readonly dialog = inject(MatDialog);

  protected readonly subjects = signal<Subject[]>([]);
  protected readonly loading = signal(false);
  protected readonly columns = ['name', 'code', 'coefficient', 'actions'];

  constructor() {
    void this.refresh();
  }

  async refresh(): Promise<void> {
    this.loading.set(true);
    try {
      this.subjects.set(await this.subjectService.list());
    } finally {
      this.loading.set(false);
    }
  }

  openCreateDialog(): void {
    this.dialog
      .open(SubjectFormDialog, { data: {}, width: '480px' })
      .afterClosed()
      .subscribe((result) => result && void this.refresh());
  }

  openEditDialog(subject: Subject): void {
    this.dialog
      .open(SubjectFormDialog, { data: { subject }, width: '480px' })
      .afterClosed()
      .subscribe((result) => result && void this.refresh());
  }

  async remove(subject: Subject): Promise<void> {
    const confirmed = await confirmAction(this.dialog, {
      title: 'Supprimer cette matière ?',
      message: `"${subject.name}" sera définitivement supprimée.`,
    });
    if (!confirmed) {
      return;
    }
    await this.subjectService.remove(subject.id);
    await this.refresh();
  }
}
