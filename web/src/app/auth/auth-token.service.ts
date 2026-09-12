import { Injectable } from '@angular/core';

export interface TokenPair {
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
}

const STORAGE_KEY = 'auth-tokens';

// Stockage minimal des jetons (session courante). Un intercepteur HTTP posant
// automatiquement l'en-tête Authorization sur les appels protégés viendra avec les premiers
// écrans qui en ont réellement besoin (voir docs/ROADMAP.md Phase 1.5+).
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
}
