import { Injectable } from '@angular/core';
import { decodeAccessToken } from '../core/jwt.util';

export interface TokenPair {
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
}

const STORAGE_KEY = 'auth-tokens';

/**
 * Stockage des jetons.
 *
 * <p>`localStorage` et non `sessionStorage`, contrairement au Web : un téléphone appartient à
 * son porteur, alors qu'un poste de secrétariat est partagé. Sur `sessionStorage`, la session
 * disparaissait dès la fermeture de l'application — un enseignant devait se reconnecter à
 * chaque fois qu'il l'ouvrait pour faire l'appel, ce qui est précisément l'usage attendu.
 *
 * <p>Le jeton de rafraîchissement vit trente jours : un enseignant traverse donc un trimestre
 * sans ressaisir son mot de passe, tant qu'il ouvre l'application au moins une fois par mois.
 */
@Injectable({ providedIn: 'root' })
export class AuthTokenService {
  store(tokens: TokenPair): void {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(tokens));
  }

  read(): TokenPair | null {
    const raw = localStorage.getItem(STORAGE_KEY);
    return raw ? (JSON.parse(raw) as TokenPair) : null;
  }

  clear(): void {
    localStorage.removeItem(STORAGE_KEY);
  }

  isAuthenticated(): boolean {
    return this.read() !== null;
  }

  /** Affichage uniquement (nom du rôle dans l'app) — voir core/jwt.util.ts. */
  role(): string | null {
    const tokens = this.read();
    return tokens ? (decodeAccessToken(tokens.accessToken)?.role ?? null) : null;
  }

  email(): string | null {
    const tokens = this.read();
    return tokens ? (decodeAccessToken(tokens.accessToken)?.email ?? null) : null;
  }
}
