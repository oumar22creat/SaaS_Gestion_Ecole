import { Component, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { Router } from '@angular/router';
import { AuthTokenService } from '../auth/auth-token.service';
import { formatMoney } from '../core/money.util';
import { supportPhoneNumber, whatsAppUrl } from '../core/support.util';
import { BillingService, Subscription } from './billing.service';

/**
 * Écran affiché à un établissement dont l'abonnement est échu.
 *
 * <p>Le serveur refuse déjà tout appel métier (TenantAccessInterceptor) : sans cet écran,
 * l'utilisateur verrait une succession de messages d'erreur sans comprendre qu'il s'agit de
 * son abonnement. La consigne est donc au centre de la page, et la seule action possible —
 * joindre l'éditeur — y est un bouton, pas une note en bas de page.
 *
 * <p>L'écran vit hors du shell : afficher le menu latéral d'une application entièrement
 * fermée reviendrait à proposer des portes verrouillées.
 */
@Component({
  selector: 'app-subscription-blocked-page',
  imports: [DatePipe, MatButtonModule, MatIconModule],
  template: `
    <main class="blocked">
      <div class="blocked-card">
        <mat-icon class="blocked-icon" aria-hidden="true">lock</mat-icon>
        <h1>Abonnement échu</h1>
        <p class="blocked-lead">
          L'accès à l'application est fermé jusqu'au renouvellement de votre abonnement. Les données
          de votre établissement sont conservées : elles seront de nouveau accessibles dès la
          réactivation.
        </p>

        @if (subscription(); as current) {
          <dl class="blocked-facts">
            <div>
              <dt>Plan</dt>
              <dd>{{ current.planName }}</dd>
            </div>
            @if (current.currentPeriodEnd) {
              <div>
                <dt>Échéance</dt>
                <dd>{{ current.currentPeriodEnd | date: 'dd/MM/yyyy' }}</dd>
              </div>
            }
          </dl>
        }

        <p class="blocked-lead">
          Le règlement se fait en espèces auprès de l'éditeur. Une fois reçu, votre abonnement est
          réactivé pour la durée réglée.
        </p>

        <div class="blocked-actions">
          <a mat-flat-button [href]="contactUrl()" target="_blank" rel="noopener">
            <mat-icon aria-hidden="true">chat</mat-icon>
            Contacter sur WhatsApp
          </a>
          <a mat-stroked-button [href]="'tel:+' + rawNumber">{{ supportNumber }}</a>
        </div>

        <button class="blocked-logout" type="button" (click)="signOut()">Se déconnecter</button>
      </div>
    </main>
  `,
  styles: `
    .blocked {
      display: grid;
      place-items: center;
      min-height: 100vh;
      padding: var(--space-5);
      background: var(--color-background, #f7f7f5);
    }

    .blocked-card {
      max-width: 520px;
      padding: var(--space-6, 32px);
      border-radius: var(--radius);
      background: var(--color-surface);
      border: 1px solid var(--color-border);
      box-shadow: var(--shadow-1);
      text-align: center;
    }

    .blocked-icon {
      width: 40px;
      height: 40px;
      font-size: 40px;
      color: var(--tenant-primary);
    }

    h1 {
      margin: var(--space-3) 0 var(--space-4);
      font-family: var(--font-family-display);
    }

    .blocked-lead {
      margin: 0 0 var(--space-4);
      color: var(--color-text-secondary);
      line-height: 1.5;
    }

    .blocked-facts {
      display: flex;
      justify-content: center;
      gap: var(--space-5);
      margin: 0 0 var(--space-4);
      padding: var(--space-3) var(--space-4);
      border-radius: var(--radius);
      background: var(--tenant-primary-soft);
    }

    .blocked-facts dt {
      color: var(--color-text-secondary);
      font-size: var(--font-size-caption);
      letter-spacing: var(--letter-spacing-caps);
      text-transform: uppercase;
    }

    .blocked-facts dd {
      margin: 2px 0 0;
      font-weight: var(--font-weight-semibold);
    }

    .blocked-actions {
      display: flex;
      flex-wrap: wrap;
      justify-content: center;
      gap: var(--space-3);
    }

    /* Se déconnecter reste possible sans occuper le même rang que l'action utile : un
     * directeur qui arrive ici doit d'abord voir comment rouvrir son accès. */
    .blocked-logout {
      margin-top: var(--space-5);
      border: 0;
      background: none;
      color: var(--color-text-secondary);
      font: inherit;
      font-size: var(--font-size-small);
      text-decoration: underline;
      cursor: pointer;
    }
  `,
})
export class SubscriptionBlockedPage {
  private readonly billingService = inject(BillingService);
  private readonly authTokenService = inject(AuthTokenService);
  private readonly router = inject(Router);

  protected readonly subscription = signal<Subscription | null>(null);
  protected readonly money = formatMoney;
  protected readonly supportNumber = supportPhoneNumber();
  protected readonly rawNumber = supportPhoneNumber().replace(/[^\d]/g, '');

  constructor() {
    // /billing reste ouvert aux établissements suspendus (TenantAccessInterceptor) : c'est ce
    // qui permet d'afficher le plan et l'échéance plutôt qu'un message générique. L'échec est
    // sans conséquence — un enseignant, par exemple, n'a pas le droit de lire l'abonnement.
    void this.billingService
      .subscription()
      .then((subscription) => this.subscription.set(subscription))
      .catch(() => undefined);
  }

  protected contactUrl(): string {
    return whatsAppUrl(
      "Bonjour, l'abonnement de mon établissement est échu et je souhaite le renouveler.",
    );
  }

  protected signOut(): void {
    this.authTokenService.clear();
    void this.router.navigateByUrl('/login');
  }
}
