import { Component, inject } from '@angular/core';
import { Router } from '@angular/router';
import { IonButton, IonContent, IonIcon } from '@ionic/angular';
import { AuthTokenService } from '../auth/auth-token.service';
import { supportPhoneNumber, whatsAppUrl } from '../core/support.util';

/**
 * Écran affiché quand l'abonnement de l'établissement est échu : le serveur refuse alors
 * tout appel métier (TenantAccessInterceptor côté backend).
 *
 * <p>Un enseignant qui ouvre l'application pour faire l'appel n'a aucun moyen de savoir que
 * son école n'a pas renouvelé : sans cet écran, il verrait une erreur technique et
 * conclurait à une panne. Le message nomme la cause et renvoie d'abord vers la direction —
 * c'est elle qui règle l'abonnement, pas lui.
 */
@Component({
  selector: 'app-subscription-blocked-page',
  imports: [IonContent, IonButton, IonIcon],
  template: `
    <ion-content class="ion-padding">
      <div class="blocked">
        <ion-icon name="lock-closed-outline" class="blocked-icon" aria-hidden="true" />
        <h1>Abonnement échu</h1>
        <p>
          L'accès à l'application est fermé jusqu'au renouvellement de l'abonnement de votre
          établissement. Les données déjà saisies sont conservées.
        </p>
        <p class="blocked-hint">
          Prévenez la direction de votre école. Le renouvellement se règle auprès de l'éditeur,
          joignable au {{ supportNumber }}.
        </p>

        <ion-button expand="block" [href]="contactUrl" target="_blank" rel="noopener">
          Contacter l'éditeur sur WhatsApp
        </ion-button>
        <ion-button expand="block" fill="clear" (click)="signOut()">Se déconnecter</ion-button>
      </div>
    </ion-content>
  `,
  styles: `
    .blocked {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      min-height: 70vh;
      text-align: center;
    }
    .blocked-icon {
      font-size: 48px;
      color: var(--tenant-primary);
    }
    h1 {
      margin: var(--space-3) 0 var(--space-4);
      font-size: var(--font-size-title-1);
      font-weight: 700;
    }
    p {
      margin: 0 0 var(--space-4);
      color: var(--color-text-secondary);
      line-height: 1.5;
    }
    .blocked-hint {
      margin-bottom: var(--space-5);
    }
  `,
})
export class SubscriptionBlockedPage {
  private readonly authTokenService = inject(AuthTokenService);
  private readonly router = inject(Router);

  protected readonly supportNumber = supportPhoneNumber();
  protected readonly contactUrl = whatsAppUrl(
    "Bonjour, l'abonnement de mon établissement est échu et je souhaite le renouveler.",
  );

  protected signOut(): void {
    this.authTokenService.clear();
    void this.router.navigateByUrl('/login');
  }
}
