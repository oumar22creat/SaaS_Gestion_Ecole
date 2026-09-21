import { Component, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { formatMoney } from '../core/money.util';
import { AuthTokenService } from '../auth/auth-token.service';
import { PlatformDashboardService, PlatformSummary } from './platform-dashboard.service';

@Component({
  selector: 'app-platform-dashboard-page',
  imports: [MatCardModule, MatButtonModule, MatIconModule],
  template: `
    <div class="page-header">
      <h1><mat-icon class="page-icon" aria-hidden="true">shield</mat-icon>Console plateforme</h1>
      <button mat-stroked-button (click)="logout()"><mat-icon>logout</mat-icon> Déconnexion</button>
    </div>
    <p class="page-subtitle">Vue consolidée des établissements abonnés (hors données élèves).</p>

    @if (summary(); as data) {
      <div class="stat-grid">
        <mat-card class="metric">
          <mat-icon class="metric-icon" aria-hidden="true">school</mat-icon>
          <p class="metric-label">Essais en cours</p>
          <p class="metric-value">{{ data.trialCount }}</p>
        </mat-card>
        <mat-card class="metric">
          <mat-icon class="metric-icon" aria-hidden="true">how_to_reg</mat-icon>
          <p class="metric-label">Établissements actifs</p>
          <p class="metric-value">{{ data.activeCount }}</p>
        </mat-card>
        <mat-card class="metric">
          <mat-icon class="metric-icon" aria-hidden="true">credit_card</mat-icon>
          <p class="metric-label">Revenu mensuel</p>
          <p class="metric-value">{{ money(data.mrrCents, data.currency) }}</p>
        </mat-card>
        <mat-card class="metric">
          <mat-icon class="metric-icon" aria-hidden="true">trending_down</mat-icon>
          <p class="metric-label">Taux d'attrition</p>
          <p class="metric-value">{{ data.churnRate !== null ? data.churnRate + ' %' : '—' }}</p>
        </mat-card>
        <mat-card class="metric">
          <mat-icon class="metric-icon" aria-hidden="true">notifications</mat-icon>
          <p class="metric-label">Notifications envoyées</p>
          <p class="metric-value">{{ data.notificationsSentCount }}</p>
        </mat-card>
      </div>
    } @else {
      <p class="loading-state" role="status" aria-label="Chargement des indicateurs"></p>
    }
  `,
  styles: `
    .metric {
      padding: var(--space-5);
    }
    .metric-emoji {
      display: grid;
      place-items: center;
      width: 38px;
      height: 38px;
      margin-bottom: var(--space-3);
      border-radius: var(--radius-sm);
      background: var(--tenant-primary-soft);
      font-size: 19px;
      line-height: 1;
    }
    .metric-label {
      margin: 0;
      color: var(--color-text-secondary);
      font-size: var(--font-size-caption);
      font-weight: var(--font-weight-semibold);
      letter-spacing: var(--letter-spacing-caps);
      text-transform: uppercase;
    }
    .metric-value {
      margin: var(--space-1) 0 0;
      font-family: var(--font-family-display);
      font-size: var(--font-size-title-1);
      font-weight: 700;
      letter-spacing: var(--letter-spacing-tight);
    }
  `,
})
export class PlatformDashboardPage {
  private readonly dashboardService = inject(PlatformDashboardService);
  private readonly authTokenService = inject(AuthTokenService);
  private readonly router = inject(Router);
  protected readonly summary = signal<PlatformSummary | null>(null);
  protected readonly money = formatMoney;

  constructor() {
    void this.dashboardService.summary().then((summary) => this.summary.set(summary));
  }

  async logout(): Promise<void> {
    this.authTokenService.clear();
    await this.router.navigateByUrl('/admin/login');
  }
}
