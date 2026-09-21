import { Component, inject } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { IonButton, IonContent, IonIcon } from '@ionic/angular';
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
  private readonly router = inject(Router);

  protected readonly email = this.authTokenService.email();
  protected readonly role = this.authTokenService.role();
  protected readonly eyebrow = homeEyebrowForRole(this.role);
  protected readonly tiles = homeTilesForRole(this.role);

  async logout(): Promise<void> {
    this.authTokenService.clear();
    await this.router.navigateByUrl('/login');
  }
}
