import { DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatTableModule } from '@angular/material/table';
import { AttendanceRecordChange } from './attendance.model';
import { AttendanceService } from './attendance.service';

/** Historique des modifications d'une feuille d'appel — cahier-des-charges.md §10, ROADMAP.md 1.7. */
@Component({
  selector: 'app-attendance-history-page',
  imports: [MatTableModule, MatButtonModule, RouterLink, DatePipe],
  templateUrl: './attendance-history.page.html',
  styleUrl: './attendance-history.page.scss',
})
export class AttendanceHistoryPage {
  private readonly route = inject(ActivatedRoute);
  private readonly attendanceService = inject(AttendanceService);

  protected readonly recordId = Number(this.route.snapshot.paramMap.get('recordId'));
  protected readonly changes = signal<AttendanceRecordChange[]>([]);
  protected readonly columns = [
    'changedAt',
    'previousStatus',
    'previousReason',
    'previousJustified',
  ];

  constructor() {
    void this.attendanceService.history(this.recordId).then((changes) => this.changes.set(changes));
  }
}
