import { Component, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { AuthTokenService } from '../auth/auth-token.service';

/**
 * Coquille de la console plateforme.
 *
 * <p>La console tenait sur un seul écran sans navigation : dès qu'elle en compte plusieurs, il
 * faut un endroit d'où passer de l'un à l'autre. Volontairement distincte de la coquille des
 * établissements — ce ne sont pas les mêmes utilisateurs, ni le même périmètre de données.
 */
@Component({
  selector: 'app-platform-shell',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, MatButtonModule, MatIconModule],
  template: `
    <div class="platform-shell">
      <header class="platform-bar">
        <span class="platform-brand">
          <mat-icon aria-hidden="true">shield</mat-icon>
          Console plateforme
        </span>
        <nav class="platform-nav">
          <a routerLink="/admin" routerLinkActive="is-active" [routerLinkActiveOptions]="{ exact: true }">
            Vue d'ensemble
          </a>
          <a routerLink="/admin/tenants" routerLinkActive="is-active">Établissements</a>
          <a routerLink="/admin/accounts" routerLinkActive="is-active">Comptes</a>
          <a routerLink="/admin/journal" routerLinkActive="is-active">Journal</a>
        </nav>
        <button mat-stroked-button (click)="logout()">
          <mat-icon>logout</mat-icon> Déconnexion
        </button>
      </header>
      <main class="platform-content">
        <router-outlet />
      </main>
    </div>
  `,
  styles: `
    .platform-shell {
      min-height: 100vh;
      background: var(--color-canvas);
    }

    /* Barre sombre, comme la coquille des établissements : on reste dans le même produit,
     * mais la mention « Console plateforme » rappelle en permanence qu'on n'y est pas. */
    .platform-bar {
      display: flex;
      align-items: center;
      gap: var(--space-5);
      padding: var(--space-3) var(--space-5);
      background: var(--color-nav);
      color: #fff;
    }

    .platform-brand {
      display: flex;
      align-items: center;
      gap: var(--space-2);
      font-family: var(--font-family-display);
      font-weight: var(--font-weight-semibold);
    }

    .platform-nav {
      display: flex;
      gap: var(--space-4);
      margin-inline-end: auto;
    }

    .platform-nav a {
      color: rgba(255, 255, 255, 0.72);
      text-decoration: none;
      font-size: var(--font-size-small);
    }

    .platform-nav a.is-active,
    .platform-nav a:hover {
      color: #fff;
    }

    .platform-content {
      padding: var(--space-5);
    }
  `,
})
export class PlatformShellComponent {
  private readonly authTokenService = inject(AuthTokenService);
  private readonly router = inject(Router);

  async logout(): Promise<void> {
    this.authTokenService.clear();
    await this.router.navigateByUrl('/admin/login');
  }
}
