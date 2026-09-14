import { Component, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatListModule } from '@angular/material/list';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatToolbarModule } from '@angular/material/toolbar';
import { AuthTokenService } from '../auth/auth-token.service';
import { NAV_LINKS } from './nav-links';

/** Layout des écrans authentifiés (Élèves, Notes, etc.) — voir docs/DESIGN.md §3 (Angular Material uniquement). */
@Component({
  selector: 'app-shell',
  imports: [
    RouterOutlet,
    RouterLink,
    RouterLinkActive,
    MatSidenavModule,
    MatToolbarModule,
    MatListModule,
    MatIconModule,
    MatButtonModule,
  ],
  templateUrl: './shell.component.html',
  styleUrl: './shell.component.scss',
})
export class ShellComponent {
  private readonly authTokenService = inject(AuthTokenService);
  private readonly router = inject(Router);

  protected readonly email = this.authTokenService.email();
  protected readonly navLinks = NAV_LINKS.filter((link) => {
    const role = this.authTokenService.role();
    return role !== null && link.roles.includes(role);
  });

  async logout(): Promise<void> {
    this.authTokenService.clear();
    await this.router.navigateByUrl('/login');
  }
}
