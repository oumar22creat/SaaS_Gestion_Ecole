import { DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { extractErrorMessage } from '../core/http-error.util';
import { formatMoney } from '../core/money.util';
import { supportPhoneNumber, whatsAppUrl } from '../core/support.util';
import { BillingPlan, BillingService, Subscription } from './billing.service';

/** Deux semaines : le temps de réunir la somme et de se déplacer pour la remettre. */
const EXPIRY_WARNING_DAYS = 14;

@Component({
  selector: 'app-billing-page',
  imports: [DatePipe, MatButtonModule, MatIconModule],
  templateUrl: './billing.page.html',
  styles: `
    .current-plan {
      display: flex;
      flex-direction: column;
      gap: 2px;
      margin-bottom: var(--space-5);
      padding: var(--space-4);
      border-radius: var(--radius);
      background: var(--color-surface);
      border: 1px solid var(--color-border);
    }

    .current-label {
      color: var(--color-text-secondary);
      font-size: var(--font-size-caption);
      font-weight: var(--font-weight-semibold);
      letter-spacing: var(--letter-spacing-caps);
      text-transform: uppercase;
    }

    .current-name {
      font-family: var(--font-family-display);
      font-size: var(--font-size-title-2, 1.4rem);
      font-weight: 700;
    }

    .current-detail {
      color: var(--color-text-secondary);
      font-size: var(--font-size-small);
    }

    .plan-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
      gap: var(--space-4);
    }

    .plan-card {
      position: relative;
      display: flex;
      flex-direction: column;
      gap: var(--space-2);
      padding: var(--space-5);
      border-radius: var(--radius);
      background: var(--color-surface);
      border: 1px solid var(--color-border);
      box-shadow: var(--shadow-1);
    }

    /* Le plan en cours est encadré plutôt que coloré : c'est un repère, pas une promotion. */
    .plan-card.plan-current {
      border-color: var(--tenant-primary);
    }

    .plan-badge {
      position: absolute;
      top: var(--space-3);
      inset-inline-end: var(--space-3);
      padding: 2px var(--space-2);
      border-radius: var(--radius-pill);
      background: var(--tenant-primary-soft);
      color: var(--tenant-primary);
      font-size: var(--font-size-caption);
      font-weight: var(--font-weight-semibold);
    }

    .plan-name {
      margin: 0;
      font-size: var(--font-size-title-2, 1.4rem);
    }

    .plan-scale {
      margin: 0;
      color: var(--tenant-secondary, var(--color-text-secondary));
      font-weight: var(--font-weight-semibold);
    }

    .plan-price {
      margin: var(--space-3) 0 0;
      font-family: var(--font-family-display);
      font-size: var(--font-size-display);
      font-weight: 700;
      line-height: 1.15;
    }

    .plan-prefix,
    .plan-unit {
      color: var(--color-text-secondary);
      font-family: var(--font-family-base);
      font-size: var(--font-size-small);
      font-weight: var(--font-weight-regular, 400);
    }

    .plan-note {
      margin: 0;
      color: var(--color-text-secondary);
      font-size: var(--font-size-small);
    }

    .plan-features {
      flex: 1 1 auto;
      margin: var(--space-4) 0;
      padding: 0;
      list-style: none;
      display: grid;
      gap: var(--space-2);
    }

    .plan-features li {
      display: flex;
      align-items: flex-start;
      gap: var(--space-2);
      font-size: var(--font-size-small);
    }

    .plan-features mat-icon {
      flex: 0 0 auto;
      font-size: 18px;
      width: 18px;
      height: 18px;
      color: var(--tenant-primary);
    }

    /* Un module absent reste listé mais en retrait : c'est ce qui rend la comparaison
     * possible entre deux plans. */
    .plan-features li.off,
    .plan-features li.off mat-icon {
      color: var(--color-text-secondary);
    }

    .expiry-notice {
      display: flex;
      align-items: flex-start;
      gap: var(--space-2);
      margin-bottom: var(--space-5);
      padding: var(--space-3) var(--space-4);
      border-radius: var(--radius);
      border: 1px solid var(--color-warning-border, #c9a227);
      background: var(--color-warning-soft, rgba(201, 162, 39, 0.12));
      font-size: var(--font-size-small);
    }

    .plans-note {
      margin-top: var(--space-5);
      color: var(--color-text-secondary);
      font-size: var(--font-size-small);
    }
  `,
})
export class BillingPage {
  private readonly billingService = inject(BillingService);
  protected readonly plans = signal<BillingPlan[]>([]);
  protected readonly subscription = signal<Subscription | null>(null);
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly money = formatMoney;
  /** Numéro affiché en clair : tout le monde n'a pas WhatsApp installé. */
  protected readonly supportNumber = supportPhoneNumber();

  constructor() {
    void this.billingService.plans().then((plans) => this.plans.set(plans));
    void this.billingService
      .subscription()
      .then((subscription) => this.subscription.set(subscription))
      .catch(() => undefined);
  }

  protected isCurrent(plan: BillingPlan): boolean {
    return this.subscription()?.planCode === plan.code;
  }

  /** Le prix à l'élève, tel que l'annonce l'offre publique : c'est ainsi qu'une école compare. */
  protected perStudent(plan: BillingPlan): string {
    if (!plan.maxStudents) {
      return '';
    }
    return new Intl.NumberFormat('fr-FR').format(
      Math.round(plan.annualPriceCents / plan.maxStudents),
    );
  }

  protected statusLabel(status: string): string {
    return (
      {
        TRIALING: "À l'essai",
        ACTIVE: 'Actif',
        PAST_DUE: 'Paiement en retard',
        EXPIRED: 'Échu',
        CANCELED: 'Résilié',
      }[status] ?? status
    );
  }

  /**
   * Prévient avant la coupure plutôt qu'après. Un abonnement réglé en espèces ne se
   * renouvelle pas tout seul : sans ce rappel, l'établissement découvre l'échéance le jour
   * où l'application se ferme.
   */
  protected expiryNotice(): string | null {
    const current = this.subscription();
    if (!current?.currentPeriodEnd || current.status === 'TRIALING') {
      return null;
    }
    const end = new Date(current.currentPeriodEnd);
    const days = Math.ceil((end.getTime() - Date.now()) / 86_400_000);
    if (days < 0) {
      return `Votre abonnement est arrivé à échéance. Contactez-nous au ${this.supportNumber} pour le renouveler et rouvrir l'accès.`;
    }
    if (days <= EXPIRY_WARNING_DAYS) {
      const when = days === 0 ? "aujourd'hui" : days === 1 ? 'demain' : `dans ${days} jours`;
      return `Votre abonnement prend fin ${when}. Passé cette date, l'accès à l'application est fermé jusqu'au renouvellement.`;
    }
    return null;
  }

  protected contactUrl(plan: BillingPlan): string {
    return whatsAppUrl(
      `Bonjour, je souhaite souscrire l'abonnement ${plan.name} pour mon établissement.`,
    );
  }

  protected renewUrl(plan: BillingPlan): string {
    return whatsAppUrl(
      `Bonjour, je souhaite renouveler l'abonnement ${plan.name} de mon établissement.`,
    );
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
