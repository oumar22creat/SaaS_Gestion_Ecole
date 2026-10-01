import { Injectable, computed, signal } from '@angular/core';
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
  /*
   * Un signal double le stockage, qui n'est pas réactif.
   *
   * L'application tourne sans zone.js : un écran n'est redessiné que si une source réactive
   * le signale. Tant que le rôle et l'adresse n'étaient que des lectures de `localStorage`,
   * l'accueil gardait l'identité du compte précédent après une reconnexion — `ion-router-outlet`
   * conserve les écrans montés dans sa pile, et rien ne venait les marquer à rafraîchir.
   *
   * `localStorage` reste la mémoire qui survit à la fermeture de l'application ; le signal
   * n'en est que le reflet en mémoire, et toute écriture passe par `store` ou `clear`.
   */
  private readonly jetons = signal<TokenPair | null>(lireStockage());

  private readonly jetonDecode = computed(() => {
    const tokens = this.jetons();
    return tokens ? decodeAccessToken(tokens.accessToken) : null;
  });

  store(tokens: TokenPair): void {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(tokens));
    this.jetons.set(tokens);
  }

  read(): TokenPair | null {
    return this.jetons();
  }

  clear(): void {
    localStorage.removeItem(STORAGE_KEY);
    this.jetons.set(null);
  }

  isAuthenticated(): boolean {
    return this.read() !== null;
  }

  /** Affichage uniquement (nom du rôle dans l'app) — voir core/jwt.util.ts. */
  role(): string | null {
    return this.jetonDecode()?.role ?? null;
  }

  email(): string | null {
    return this.jetonDecode()?.email ?? null;
  }
}

/** Jetons retenus d'une exécution précédente ; une valeur illisible vaut « pas de session ». */
function lireStockage(): TokenPair | null {
  const raw = localStorage.getItem(STORAGE_KEY);
  if (!raw) {
    return null;
  }
  try {
    return JSON.parse(raw) as TokenPair;
  } catch {
    localStorage.removeItem(STORAGE_KEY);
    return null;
  }
}
