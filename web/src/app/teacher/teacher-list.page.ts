import { Component, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatChipsModule } from '@angular/material/chips';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatTableModule } from '@angular/material/table';
import { confirmAction } from '../core/confirm-dialog.component';
import { ListSearchComponent } from '../core/list-search.component';
import { TeacherFormDialog } from './teacher-form.dialog';
import { Teacher } from './teacher.model';
import { TeacherService } from './teacher.service';

@Component({
  selector: 'app-teacher-list-page',
  imports: [
    MatTableModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatDialogModule,
    MatPaginatorModule,
    ListSearchComponent,
  ],
  templateUrl: './teacher-list.page.html',
  styleUrl: './teacher-list.page.scss',
})
export class TeacherListPage {
  private readonly teacherService = inject(TeacherService);
  private readonly dialog = inject(MatDialog);

  protected readonly teachers = signal<Teacher[]>([]);
  protected readonly loading = signal(false);
  protected readonly total = signal(0);
  protected readonly search = signal('');
  protected readonly pageIndex = signal(0);
  protected readonly pageSize = signal(25);
  protected readonly columns = ['firstName', 'lastName', 'email', 'phone', 'active', 'actions'];

  constructor() {
    void this.refresh();
  }

  /** Une nouvelle recherche repart de la première page : rester en page 4 n'aurait aucun sens. */
  protected onSearch(term: string): void {
    this.search.set(term);
    this.pageIndex.set(0);
    void this.refresh();
  }

  protected onPage(event: PageEvent): void {
    this.pageIndex.set(event.pageIndex);
    this.pageSize.set(event.pageSize);
    void this.refresh();
  }

  async refresh(): Promise<void> {
    this.loading.set(true);
    try {
      const page = await this.teacherService.page({
        page: this.pageIndex(),
        pageSize: this.pageSize(),
        search: this.search(),
      });
      this.teachers.set(page.items);
      this.total.set(page.total);
    } finally {
      this.loading.set(false);
    }
  }

  openCreateDialog(): void {
    this.dialog
      .open(TeacherFormDialog, { data: {}, width: '480px' })
      .afterClosed()
      .subscribe((result) => {
        if (result) {
          void this.refresh();
        }
      });
  }

  openEditDialog(teacher: Teacher): void {
    this.dialog
      .open(TeacherFormDialog, { data: { teacher }, width: '480px' })
      .afterClosed()
      .subscribe((result) => {
        if (result) {
          void this.refresh();
        }
      });
  }

  async deactivate(teacher: Teacher): Promise<void> {
    const confirmed = await confirmAction(this.dialog, {
      title: 'Désactiver cet enseignant ?',
      message: `${teacher.firstName} ${teacher.lastName} n'apparaîtra plus comme actif.`,
    });
    if (!confirmed) {
      return;
    }
    await this.teacherService.deactivate(teacher.id);
    await this.refresh();
  }
}
