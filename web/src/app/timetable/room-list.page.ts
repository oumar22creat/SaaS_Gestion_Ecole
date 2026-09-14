import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { confirmAction } from '../core/confirm-dialog.component';
import { Room } from './room.model';
import { RoomFormDialog } from './room-form.dialog';
import { RoomService } from './room.service';

@Component({
  selector: 'app-room-list-page',
  imports: [MatTableModule, MatButtonModule, MatIconModule, MatDialogModule, RouterLink],
  templateUrl: './room-list.page.html',
  styleUrl: './room-list.page.scss',
})
export class RoomListPage {
  private readonly roomService = inject(RoomService);
  private readonly dialog = inject(MatDialog);

  protected readonly rooms = signal<Room[]>([]);
  protected readonly loading = signal(false);
  protected readonly columns = ['name', 'capacity', 'actions'];

  constructor() {
    void this.refresh();
  }

  async refresh(): Promise<void> {
    this.loading.set(true);
    try {
      this.rooms.set(await this.roomService.list());
    } finally {
      this.loading.set(false);
    }
  }

  openCreateDialog(): void {
    this.dialog
      .open(RoomFormDialog, { data: {}, width: '480px' })
      .afterClosed()
      .subscribe((result) => result && void this.refresh());
  }

  openEditDialog(room: Room): void {
    this.dialog
      .open(RoomFormDialog, { data: { room }, width: '480px' })
      .afterClosed()
      .subscribe((result) => result && void this.refresh());
  }

  async remove(room: Room): Promise<void> {
    const confirmed = await confirmAction(this.dialog, {
      title: 'Supprimer cette salle ?',
      message: `"${room.name}" sera définitivement supprimée.`,
    });
    if (!confirmed) {
      return;
    }
    await this.roomService.remove(room.id);
    await this.refresh();
  }
}
