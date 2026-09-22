import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatIconModule } from '@angular/material/icon';
import { SetupGuideComponent } from '../onboarding/setup-guide.component';
import { formatMoney } from '../core/money.util';
import { DashboardSummary } from './dashboard.model';
import { DashboardService } from './dashboard.service';

/** Dashboard établissement — cahier-des-charges.md §18, mockup docs/MOCKUPS.md §5. */
@Component({
  selector: 'app-dashboard-page',
  imports: [RouterLink, MatCardModule, MatProgressBarModule, MatIconModule, SetupGuideComponent],
  templateUrl: './dashboard.page.html',
  styleUrl: './dashboard.page.scss',
})
export class DashboardPage {
  protected readonly money = formatMoney;
  private readonly dashboardService = inject(DashboardService);

  protected readonly summary = signal<DashboardSummary | null>(null);

  constructor() {
    void this.dashboardService.summary().then((summary) => this.summary.set(summary));
  }
}
