import { Injectable } from '@angular/core';
import { decodeAccessToken } from '../core/jwt.util';

export interface TokenPair {
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
}

const STORAGE_KEY = 'auth-tokens';

/** Stockage minimal des jetons (session courante) — identique à web/src/app/auth/auth-token.service.ts. */
@Injectable({ providedIn: 'root' })
export class AuthTokenService {
  store(tokens: TokenPair): void {
    sessionStorage.setItem(STORAGE_KEY, JSON.stringify(tokens));
  }

  read(): TokenPair | null {
    const raw = sessionStorage.getItem(STORAGE_KEY);
    return raw ? (JSON.parse(raw) as TokenPair) : null;
  }

  clear(): void {
    sessionStorage.removeItem(STORAGE_KEY);
  }

  isAuthenticated(): boolean {
    return this.read() !== null;
  }

  /** Affichage uniquement (nom du rôle dans l'app) — voir core/jwt.util.ts. */
  role(): string | null {
    const tokens = this.read();
    return tokens ? decodeAccessToken(tokens.accessToken)?.role ?? null : null;
  }

  email(): string | null {
    const tokens = this.read();
    return tokens ? decodeAccessToken(tokens.accessToken)?.email ?? null : null;
  }
}
