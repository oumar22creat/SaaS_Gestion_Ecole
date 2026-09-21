import { BreakpointObserver } from '@angular/cdk/layout';
import { Component, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatListModule } from '@angular/material/list';
import { MatDrawer, MatSidenavModule } from '@angular/material/sidenav';
import { map } from 'rxjs';
import { AuthTokenService } from '../auth/auth-token.service';
import { navSectionsForRole, roleLabel } from './nav-links';

/** Largeur en dessous de laquelle la navigation passe en tiroir superposé. */
const COMPACT_QUERY = '(max-width: 1023px)';

/** Layout des écrans authentifiés (Élèves, Notes, etc.) — voir docs/DESIGN.md §3 (Angular Material uniquement). */
@Component({
  selector: 'app-shell',
  imports: [
    RouterOutlet,
    RouterLink,
    RouterLinkActive,
    MatSidenavModule,
    MatListModule,
    MatButtonModule,
    MatIconModule,
  ],
  templateUrl: './shell.component.html',
  styleUrl: './shell.component.scss',
})
export class ShellComponent {
  private readonly authTokenService = inject(AuthTokenService);
  private readonly router = inject(Router);
  private readonly breakpointObserver = inject(BreakpointObserver);

  protected readonly email = this.authTokenService.email();
  protected readonly role = this.authTokenService.role();
  protected readonly roleLabel = roleLabel(this.role);
  protected readonly navSections = navSectionsForRole(this.role);
  protected readonly initials = initialsFromEmail(this.email);

  protected readonly isCompact = toSignal(
    this.breakpointObserver.observe(COMPACT_QUERY).pipe(map((state) => state.matches)),
    { initialValue: this.breakpointObserver.isMatched(COMPACT_QUERY) },
  );

  /** Sur petit écran le tiroir recouvre le contenu : il se referme après navigation. */
  closeOnCompact(drawer: MatDrawer): void {
    if (this.isCompact()) {
      void drawer.close();
    }
  }

  async logout(): Promise<void> {
    this.authTokenService.clear();
    await this.router.navigateByUrl('/login');
  }
}

/** Initiales pour l'avatar (affichage uniquement, pas d'identité métier). */
function initialsFromEmail(email: string | null): string {
  if (!email) {
    return 'school';
  }
  const [localPart] = email.split('@');
  const parts = localPart.split(/[._-]/).filter(Boolean);
  const letters = parts.length >= 2 ? `${parts[0][0]}${parts[1][0]}` : localPart.slice(0, 2);
  return letters.toUpperCase();
}
