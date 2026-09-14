import { Component, inject, signal } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { DashboardSummary } from './dashboard.model';
import { DashboardService } from './dashboard.service';

/** Dashboard établissement — cahier-des-charges.md §18, mockup docs/MOCKUPS.md §5. */
@Component({
  selector: 'app-dashboard-page',
  imports: [MatCardModule],
  templateUrl: './dashboard.page.html',
  styleUrl: './dashboard.page.scss',
})
export class DashboardPage {
  private readonly dashboardService = inject(DashboardService);

  protected readonly summary = signal<DashboardSummary | null>(null);

  constructor() {
    void this.dashboardService.summary().then((summary) => this.summary.set(summary));
  }
}
