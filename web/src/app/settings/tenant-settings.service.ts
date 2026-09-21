import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../environments/environment';
import { ApiResponse } from '../core/api-response.model';
import { TenantBranding } from '../branding/tenant-branding.model';

@Injectable({ providedIn: 'root' })
export class TenantSettingsService {
  private readonly http = inject(HttpClient);

  async branding(): Promise<TenantBranding> {
    return firstValueFrom(this.http.get<TenantBranding>(`${environment.apiUrl}/tenants/current/branding`));
  }

  async updateBranding(logoUrl: string | null, primaryColor: string, secondaryColor: string): Promise<TenantBranding> {
    const response = await firstValueFrom(
      this.http.put<ApiResponse<TenantBranding>>(`${environment.apiUrl}/tenants/current/branding`, {
        logoUrl,
        primaryColor,
        secondaryColor,
      }),
    );
    return response.data;
  }

  async customDomain(): Promise<string | null> {
    const response = await firstValueFrom(
      this.http.get<ApiResponse<{ customDomain: string | null }>>(`${environment.apiUrl}/tenants/current/custom-domain`),
    );
    return response.data.customDomain;
  }

  async updateCustomDomain(customDomain: string): Promise<void> {
    await firstValueFrom(this.http.put(`${environment.apiUrl}/tenants/current/custom-domain`, { customDomain }));
  }
}
