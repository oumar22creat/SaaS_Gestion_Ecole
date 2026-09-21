import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../environments/environment';
import { TenantRegistrationRequest, TenantRegistrationResponse } from './registration.model';

interface ApiResponse<T> {
  data: T;
}

@Injectable({ providedIn: 'root' })
export class RegistrationService {
  private readonly http = inject(HttpClient);

  async register(request: TenantRegistrationRequest): Promise<TenantRegistrationResponse> {
    const response = await firstValueFrom(
      this.http.post<ApiResponse<TenantRegistrationResponse>>(
        `${environment.apiUrl}/tenants/register`,
        request,
      ),
    );
    return response.data;
  }
}
