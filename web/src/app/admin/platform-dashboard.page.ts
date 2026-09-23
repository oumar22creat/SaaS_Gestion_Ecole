import { Component, inject, signal } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { formatMoney } from '../core/money.util';
import { PlatformDashboardService, PlatformSummary } from './platform-dashboard.service';

@Component({
  selector: 'app-platform-dashboard-page',
  imports: [MatCardModule, MatIconModule],
  template: `
    <div class="page-header">
      <h1><mat-icon class="page-icon" aria-hidden="true">monitoring</mat-icon>Vue d'ensemble</h1>
    </div>
    <p class="page-subtitle">
      Situation consolidée des établissements abonnés. Aucune donnée scolaire n'y figure :
      élèves, parents et notes restent invisibles depuis cette console.
    </p>

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
          <mat-icon class="metric-icon" aria-hidden="true">savings</mat-icon>
          <p class="metric-label">Revenu annualisé</p>
          <p class="metric-value">{{ money(data.arrCents, data.currency) }}</p>
        </mat-card>
        <mat-card class="metric">
          <mat-icon class="metric-icon" aria-hidden="true">trending_down</mat-icon>
          <p class="metric-label">Taux d'attrition</p>
          <p class="metric-value">{{ data.churnRate !== null ? data.churnRate + ' %' : '—' }}</p>
        </mat-card>
        <mat-card class="metric">
          <mat-icon class="metric-icon" aria-hidden="true">trending_up</mat-icon>
          <p class="metric-label">Taux de conversion</p>
          <p class="metric-value">
            {{ data.conversionRate !== null ? data.conversionRate + ' %' : '—' }}
          </p>
        </mat-card>
        <mat-card class="metric" [class.metric-alert]="data.suspendedCount > 0">
          <mat-icon class="metric-icon" aria-hidden="true">pause_circle</mat-icon>
          <p class="metric-label">Accès restreints</p>
          <p class="metric-value">{{ data.readOnlyCount + data.suspendedCount }}</p>
          <p class="metric-detail">
            {{ data.readOnlyCount }} en lecture seule · {{ data.suspendedCount }} suspendus
          </p>
        </mat-card>
        <mat-card class="metric">
          <mat-icon class="metric-icon" aria-hidden="true">cancel</mat-icon>
          <p class="metric-label">Résiliés</p>
          <p class="metric-value">{{ data.cancelledCount }}</p>
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
    /* Un accès restreint est la seule alerte de l'écran : il doit se distinguer sans faire
     * paraître les autres indicateurs anodins. */
    .metric.metric-alert {
      border: 1px solid var(--color-danger, #b3261e);
    }

    .metric-detail {
      margin: var(--space-1) 0 0;
      color: var(--color-text-secondary);
      font-size: var(--font-size-caption);
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
  protected readonly summary = signal<PlatformSummary | null>(null);
  protected readonly money = formatMoney;

  constructor() {
    void this.dashboardService.summary().then((summary) => this.summary.set(summary));
  }

}
