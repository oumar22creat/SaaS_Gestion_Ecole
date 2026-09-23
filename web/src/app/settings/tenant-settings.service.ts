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
    return firstValueFrom(
      this.http.get<TenantBranding>(`${environment.apiUrl}/tenants/current/branding`),
    );
  }

  async updateBranding(
    logoUrl: string | null,
    primaryColor: string,
    secondaryColor: string,
  ): Promise<TenantBranding> {
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
      this.http.get<ApiResponse<{ customDomain: string | null }>>(
        `${environment.apiUrl}/tenants/current/custom-domain`,
      ),
    );
    return response.data.customDomain;
  }

  async updateCustomDomain(customDomain: string): Promise<void> {
    await firstValueFrom(
      this.http.put(`${environment.apiUrl}/tenants/current/custom-domain`, { customDomain }),
    );
  }

  /**
   * Téléverse le logo utilisé sur les documents officiels. Distinct de l'URL du logo, que le
   * navigateur va chercher lui-même : les PDF sont fabriqués par le serveur, qui ne suit
   * jamais une adresse saisie ici.
   */
  async uploadLogo(file: File): Promise<void> {
    const formData = new FormData();
    formData.append('file', file);
    await firstValueFrom(
      this.http.post(`${environment.apiUrl}/tenants/current/logo`, formData),
    );
  }

  /**
   * Récupère le logo pour l'aperçu. Passe par HttpClient et non par une balise `img` : hors
   * production le tenant est résolu depuis le jeton d'authentification, qu'une image chargée
   * par le navigateur ne transmet pas — l'aperçu serait cassé en développement.
   *
   * @returns une URL d'objet à révoquer après usage, ou null si aucun logo n'est défini
   */
  async loadLogoPreview(): Promise<string | null> {
    try {
      const blob = await firstValueFrom(
        this.http.get(`${environment.apiUrl}/tenants/current/logo`, { responseType: 'blob' }),
      );
      return URL.createObjectURL(blob);
    } catch {
      // 404 = aucun logo : ce n'est pas une erreur à signaler à l'utilisateur.
      return null;
    }
  }

  async removeLogo(): Promise<void> {
    await firstValueFrom(this.http.delete(`${environment.apiUrl}/tenants/current/logo`));
  }
}
