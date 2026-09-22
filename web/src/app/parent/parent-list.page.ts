import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatTableModule } from '@angular/material/table';
import { confirmAction } from '../core/confirm-dialog.component';
import { ListSearchComponent } from '../core/list-search.component';
import { ParentFormDialog } from './parent-form.dialog';
import { Parent } from './parent.model';
import { ParentService } from './parent.service';

@Component({
  selector: 'app-parent-list-page',
  imports: [MatTableModule, MatButtonModule, MatIconModule, MatDialogModule, MatPaginatorModule, ListSearchComponent, RouterLink],
  templateUrl: './parent-list.page.html',
  styleUrl: './parent-list.page.scss',
})
export class ParentListPage {
  private readonly parentService = inject(ParentService);
  private readonly dialog = inject(MatDialog);

  protected readonly parents = signal<Parent[]>([]);
  protected readonly loading = signal(false);
  protected readonly total = signal(0);
  protected readonly search = signal('');
  protected readonly pageIndex = signal(0);
  protected readonly pageSize = signal(25);
  protected readonly columns = ['firstName', 'lastName', 'email', 'phone', 'actions'];

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
      const page = await this.parentService.page({
        page: this.pageIndex(),
        pageSize: this.pageSize(),
        search: this.search(),
      });
      this.parents.set(page.items);
      this.total.set(page.total);
    } finally {
      this.loading.set(false);
    }
  }

  openCreateDialog(): void {
    this.dialog
      .open(ParentFormDialog, { data: {}, width: '480px' })
      .afterClosed()
      .subscribe((result) => result && void this.refresh());
  }

  openEditDialog(parent: Parent): void {
    this.dialog
      .open(ParentFormDialog, { data: { parent }, width: '480px' })
      .afterClosed()
      .subscribe((result) => result && void this.refresh());
  }

  async remove(parent: Parent): Promise<void> {
    const confirmed = await confirmAction(this.dialog, {
      title: 'Supprimer ce parent ?',
      message: `${parent.firstName} ${parent.lastName} sera définitivement supprimé.`,
    });
    if (!confirmed) {
      return;
    }
    await this.parentService.remove(parent.id);
    await this.refresh();
  }
}
