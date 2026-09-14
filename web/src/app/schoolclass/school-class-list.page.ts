import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { confirmAction } from '../core/confirm-dialog.component';
import { SchoolClassFormDialog } from './school-class-form.dialog';
import { SchoolClass } from './school-class.model';
import { SchoolClassService } from './school-class.service';

@Component({
  selector: 'app-school-class-list-page',
  imports: [MatTableModule, MatButtonModule, MatIconModule, MatDialogModule, RouterLink],
  templateUrl: './school-class-list.page.html',
  styleUrl: './school-class-list.page.scss',
})
export class SchoolClassListPage {
  private readonly schoolClassService = inject(SchoolClassService);
  private readonly dialog = inject(MatDialog);

  protected readonly classes = signal<SchoolClass[]>([]);
  protected readonly loading = signal(false);
  protected readonly columns = ['name', 'headTeacherId', 'actions'];

  constructor() {
    void this.refresh();
  }

  async refresh(): Promise<void> {
    this.loading.set(true);
    try {
      this.classes.set(await this.schoolClassService.list());
    } finally {
      this.loading.set(false);
    }
  }

  openCreateDialog(): void {
    this.dialog
      .open(SchoolClassFormDialog, { data: {}, width: '480px' })
      .afterClosed()
      .subscribe((result) => result && void this.refresh());
  }

  openEditDialog(schoolClass: SchoolClass): void {
    this.dialog
      .open(SchoolClassFormDialog, { data: { schoolClass }, width: '480px' })
      .afterClosed()
      .subscribe((result) => result && void this.refresh());
  }

  async remove(schoolClass: SchoolClass): Promise<void> {
    const confirmed = await confirmAction(this.dialog, {
      title: 'Supprimer cette classe ?',
      message: `"${schoolClass.name}" sera définitivement supprimée.`,
    });
    if (!confirmed) {
      return;
    }
    await this.schoolClassService.remove(schoolClass.id);
    await this.refresh();
  }
}
