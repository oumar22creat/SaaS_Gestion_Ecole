import { Component, inject, signal } from '@angular/core';
import { NavigationEnd, Router, RouterLink, RouterOutlet } from '@angular/router';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatIconModule } from '@angular/material/icon';
import { filter } from 'rxjs';
import { TenantBrandingService } from './branding/tenant-branding.service';

@Component({
  imports: [RouterOutlet, RouterLink, MatToolbarModule, MatIconModule],
  selector: 'app-root',
  styleUrl: './app.scss',
  templateUrl: './app.html',
})
export class App {
  private readonly brandingService = inject(TenantBrandingService);
  private readonly router = inject(Router);

  protected readonly branding = this.brandingService.branding;

  /**
   * La barre au nom de l'établissement est masquée dans la console plateforme : un
   * Super-Administrateur n'est dans aucune école, et afficher le nom et le logo de l'une
   * d'elles au-dessus de la liste de toutes laisse croire qu'on y est entré.
   */
  protected readonly showTenantBrand = signal(!this.router.url.startsWith('/admin'));

  constructor() {
    this.router.events
      .pipe(filter((event): event is NavigationEnd => event instanceof NavigationEnd))
      .subscribe((event) => this.showTenantBrand.set(!event.urlAfterRedirects.startsWith('/admin')));
  }
}
