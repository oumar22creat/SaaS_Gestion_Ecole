import { Component, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { extractErrorMessage } from '../core/http-error.util';
import { formatMoney } from '../core/money.util';
import { BillingPlan, BillingService, Subscription } from './billing.service';

@Component({
  selector: 'app-billing-page',
  imports: [MatCardModule, MatButtonModule, MatIconModule],
  template: `
    <div class="page-header">
      <h1><mat-icon class="page-icon" aria-hidden="true">credit_card</mat-icon>Abonnement</h1>
    </div>
    @if (subscription()) {
      <mat-card>
        <p>Plan actuel : {{ subscription()!.planName }} ({{ subscription()!.status }})</p>
      </mat-card>
    }
    @if (errorMessage()) {
      <p class="flash-error">{{ errorMessage() }}</p>
    }
    <div class="cards">
      @for (plan of plans(); track plan.code) {
        <mat-card>
          <h2>{{ plan.name }}</h2>
          <p>{{ money(plan.priceCents, plan.currency) }} / mois</p>
          <button mat-flat-button [disabled]="!plan.purchasable" (click)="checkout(plan)">
            Choisir
          </button>
        </mat-card>
      }
    </div>
  `,
  styles: `
    .cards {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
      gap: var(--space-4);
      margin-top: var(--space-4);
    }
    mat-card {
      padding: var(--space-4);
    }
  `,
})
export class BillingPage {
  private readonly billingService = inject(BillingService);
  protected readonly plans = signal<BillingPlan[]>([]);
  protected readonly subscription = signal<Subscription | null>(null);
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly money = formatMoney;

  constructor() {
    void this.billingService.plans().then((plans) => this.plans.set(plans));
    void this.billingService
      .subscription()
      .then((subscription) => this.subscription.set(subscription))
      .catch(() => undefined);
  }

  async checkout(plan: BillingPlan): Promise<void> {
    try {
      const url = await this.billingService.checkout(plan.code);
      window.location.href = url;
    } catch (error) {
      this.errorMessage.set(extractErrorMessage(error));
    }
  }
}
