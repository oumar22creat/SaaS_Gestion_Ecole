import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../environments/environment';
import { ApiResponse } from '../core/api-response.model';
import { TokenPair } from './auth-token.service';

/**
 * Identique à web/src/app/auth/auth.service.ts. Le sous-domaine est requis par le backend
 * depuis docs/ARCHITECTURE.md ADR-029 : l'e-mail n'est unique que par établissement, donc le
 * tenant doit être connu avant de chercher l'utilisateur.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);

  async login(subdomain: string, email: string, password: string): Promise<TokenPair> {
    const response = await firstValueFrom(
      this.http.post<ApiResponse<TokenPair>>(`${environment.apiUrl}/auth/login`, { subdomain, email, password }),
    );
    return response.data;
  }
}
