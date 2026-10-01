import { Component, computed, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { IonButton, IonContent, IonIcon, NavController } from '@ionic/angular';
import { AuthTokenService } from '../auth/auth-token.service';
import { homeEyebrowForRole, homeTilesForRole } from '../core/role-access';

@Component({
  selector: 'app-home-page',
  imports: [IonContent, IonButton, RouterLink, IonIcon],
  templateUrl: './home.page.html',
  styles: `
    .greeting {
      margin: var(--space-2) 0 var(--space-6);
      padding: var(--space-5);
      border-radius: var(--radius-lg);
      color: var(--color-nav-text);
      background:
        radial-gradient(
          420px 200px at 85% 0%,
          color-mix(in srgb, var(--tenant-primary) 70%, transparent),
          transparent
        ),
        linear-gradient(150deg, var(--color-nav-elevated), var(--color-nav));
    }
    .greeting-eyebrow {
      margin: 0;
      color: var(--color-nav-muted);
      font-size: var(--font-size-caption);
      font-weight: var(--font-weight-semibold);
      letter-spacing: var(--letter-spacing-caps);
      text-transform: uppercase;
    }
    .greeting-title {
      margin: var(--space-2) 0 var(--space-1);
      font-size: var(--font-size-display);
      font-weight: 700;
      line-height: 1.1;
    }
    .greeting-user {
      margin: 0;
      color: var(--color-nav-muted);
      font-size: var(--font-size-small);
      overflow-wrap: anywhere;
    }
    .tiles {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: var(--space-3);
    }
    .tile {
      display: flex;
      flex-direction: column;
      gap: var(--space-1);
      min-height: 124px;
      padding: var(--space-4);
      border: 1px solid var(--color-border);
      border-radius: var(--radius);
      box-shadow: var(--shadow-1);
      background: var(--color-surface);
      color: var(--color-text);
      text-decoration: none;
    }
    .tile-wide {
      grid-column: 1 / -1;
      min-height: 96px;
    }
    .tile-icon {
      display: grid;
      place-items: center;
      width: 44px;
      height: 44px;
      margin-bottom: var(--space-2);
      border-radius: var(--radius-sm);
      background: var(--tenant-primary-soft);
      font-size: 22px;
      line-height: 1;
    }
    .tile-label {
      font-family: var(--font-family-display);
      font-size: var(--font-size-title-3);
      font-weight: var(--font-weight-semibold);
    }
    .tile-hint {
      color: var(--color-text-secondary);
      font-size: var(--font-size-caption);
    }
    .logout {
      margin-top: var(--space-6);
      --color: var(--color-text-secondary);
    }
  `,
})
export class HomePage {
  private readonly authTokenService = inject(AuthTokenService);
  private readonly navController = inject(NavController);

  /*
   * Dérivé du jeton courant, et non figé à la construction.
   *
   * `ion-router-outlet` garde les écrans montés dans sa pile de navigation pour rendre le
   * retour instantané. Des champs initialisés une seule fois figeaient donc l'identité du
   * premier compte connecté : sur un téléphone partagé, l'enseignant se déconnectait, un
   * parent se connectait, et l'accueil lui présentait encore l'adresse de l'enseignant et
   * les tuiles de saisie des notes. Le jeton stocké était pourtant bien le sien.
   *
   * Des signaux calculés, et non de simples accesseurs : sans zone.js, un écran déjà monté
   * n'est redessiné que si une source réactive le demande.
   */
  protected readonly role = computed(() => this.authTokenService.role());
  protected readonly email = computed(() => this.authTokenService.email());
  protected readonly eyebrow = computed(() => homeEyebrowForRole(this.role()));
  protected readonly tiles = computed(() => homeTilesForRole(this.role()));

  async logout(): Promise<void> {
    this.authTokenService.clear();
    /*
     * `navigateRoot` et non `navigateByUrl` : il vide la pile d'Ionic au lieu d'empiler
     * l'écran de connexion par-dessus les écrans du compte précédent. Sans cela, les pages
     * déjà visitées restent montées avec leurs données en mémoire, et le compte suivant
     * peut les retrouver telles quelles.
     */
    await this.navController.navigateRoot('/login');
  }
}
