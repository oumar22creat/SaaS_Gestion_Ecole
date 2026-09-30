import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../environments/environment';
import { ApiResponse } from '../core/api-response.model';
import { AuthTokenService, TokenPair } from './auth-token.service';

/**
 * Renouvellement silencieux du jeton d'accès.
 *
 * <p>Le jeton d'accès vit quinze minutes — durée volontairement courte, c'est ce qui limite
 * les dégâts s'il fuite. Sans renouvellement, l'utilisateur était renvoyé à l'écran de
 * connexion en pleine saisie au bout d'un quart d'heure, alors qu'un jeton de
 * rafraîchissement valable trente jours lui avait été remis et n'était jamais utilisé.
 *
 * <p>Un seul rafraîchissement à la fois. Le serveur révoque l'ancien jeton en émettant le
 * nouveau (rotation, voir AuthService#refresh) : si un écran lance cinq requêtes qui expirent
 * ensemble, cinq rafraîchissements concurrents feraient réussir le premier et échouer les
 * quatre autres sur un jeton déjà révoqué — l'utilisateur serait déconnecté malgré la
 * correction. La promesse en cours est donc partagée entre tous les appelants.
 */
@Injectable({ providedIn: 'root' })
export class TokenRefreshService {
  private readonly http = inject(HttpClient);
  private readonly authTokenService = inject(AuthTokenService);

  private enCours: Promise<TokenPair> | null = null;

  refresh(): Promise<TokenPair> {
    if (!this.enCours) {
      this.enCours = this.demander().finally(() => {
        this.enCours = null;
      });
    }
    return this.enCours;
  }

  private async demander(): Promise<TokenPair> {
    const tokens = this.authTokenService.read();
    if (!tokens) {
      throw new Error('Aucune session à renouveler');
    }
    const response = await firstValueFrom(
      this.http.post<ApiResponse<TokenPair>>(`${environment.apiUrl}/auth/refresh`, {
        refreshToken: tokens.refreshToken,
      }),
    );
    this.authTokenService.store(response.data);
    return response.data;
  }
}
