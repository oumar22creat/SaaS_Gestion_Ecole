import { Component, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import {
  IonButton,
  IonButtons,
  IonContent,
  IonHeader,
  IonIcon,
  IonTitle,
  IonToolbar,
} from '@ionic/angular';
import { AuthTokenService } from '../auth/auth-token.service';
import { extractErrorMessage } from '../core/http-error.util';
import { PortalChild, PortalService } from './portal.service';

/**
 * Point d'entrée du portail famille. Un parent y choisit l'enfant à consulter ; un élève,
 * qui n'a qu'un seul dossier, est redirigé directement sans avoir à choisir.
 */
@Component({
  selector: 'app-portal-children-page',
  imports: [
    IonHeader,
    IonToolbar,
    IonButtons,
    IonButton,
    IonTitle,
    IonContent,
    IonIcon,
    RouterLink,
  ],
  template: `
    <ion-header>
      <ion-toolbar>
        <ion-buttons slot="start">
          <ion-button routerLink="/home">
            <ion-icon aria-hidden="true" name="home-outline"></ion-icon>
            Accueil
          </ion-button>
        </ion-buttons>
        <ion-title>Mes enfants</ion-title>
      </ion-toolbar>
    </ion-header>

    <ion-content class="ion-padding">
      @if (errorMessage()) {
        <p class="error-message">{{ errorMessage() }}</p>
      } @else if (loading()) {
        <p class="empty-state">Chargement…</p>
      } @else if (children().length === 0) {
        <p class="empty-state">
          Aucun enfant n'est rattaché à votre compte. Signalez-le au secrétariat de
          l'établissement.
        </p>
      } @else {
        <div class="child-list">
          @for (child of children(); track child.id) {
            <a class="child-card" [routerLink]="['/portal', child.id]">
              <ion-icon class="child-icon" aria-hidden="true" name="people-outline"></ion-icon>
              <span class="child-text">
                <span class="child-name">{{ child.firstName }} {{ child.lastName }}</span>
                <span class="child-meta">{{ child.studentNumber }}</span>
              </span>
            </a>
          }
        </div>
      }
    </ion-content>
  `,
  styles: `
    .child-list {
      display: grid;
      gap: var(--space-3);
    }

    .child-card {
      display: flex;
      align-items: center;
      gap: var(--space-4);
      padding: var(--space-4);
      border-radius: var(--radius);
      background: var(--color-surface);
      border: 1px solid var(--color-border);
      text-decoration: none;
      color: inherit;
    }

    .child-icon {
      flex: 0 0 auto;
      width: 40px;
      height: 40px;
      color: var(--tenant-primary);
      font-size: 26px;
    }

    .child-text {
      display: flex;
      flex-direction: column;
    }

    .child-name {
      font-weight: 600;
    }

    .child-meta {
      color: var(--color-text-secondary);
      font-size: var(--font-size-small);
    }
  `,
})
export class PortalChildrenPage {
  private readonly portalService = inject(PortalService);
  private readonly tokenService = inject(AuthTokenService);
  private readonly router = inject(Router);

  protected readonly children = signal<PortalChild[]>([]);
  protected readonly loading = signal(true);
  protected readonly errorMessage = signal<string | null>(null);

  constructor() {
    void this.load();
  }

  private async load(): Promise<void> {
    try {
      const children = await this.portalService.children();
      // Un élève n'a qu'un dossier : lui demander de le choisir serait une étape inutile.
      if (this.tokenService.role() === 'STUDENT' && children.length === 1) {
        void this.router.navigate(['/portal', children[0].id], { replaceUrl: true });
        return;
      }
      this.children.set(children);
    } catch (error) {
      this.errorMessage.set(extractErrorMessage(error));
    } finally {
      this.loading.set(false);
    }
  }
}
