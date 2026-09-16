import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../environments/environment';
import { ApiResponse } from '../core/api-response.model';
import { TokenPair } from './auth-token.service';

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
