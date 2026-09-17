import { Component, inject } from '@angular/core';
import { Router } from '@angular/router';
import { IonButton, IonContent } from '@ionic/angular';
import { AuthTokenService } from '../auth/auth-token.service';

/**
 * Écran d'accueil minimal une fois connecté — prouve le cycle complet connexion/garde de
 * route/déconnexion côté Mobile (voir ROADMAP.md : les modules métier eux-mêmes, comme sur
 * Web, restent à construire un par un dans de prochaines tâches).
 */
@Component({
  selector: 'app-home-page',
  imports: [IonContent, IonButton],
  templateUrl: './home.page.html',
})
export class HomePage {
  private readonly authTokenService = inject(AuthTokenService);
  private readonly router = inject(Router);

  protected readonly email = this.authTokenService.email();

  async logout(): Promise<void> {
    this.authTokenService.clear();
    await this.router.navigateByUrl('/login');
  }
}
