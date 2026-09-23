import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../environments/environment';
import { ApiResponse } from '../core/api-response.model';
import { PageQuery, Paged, pageParams } from '../core/paged.model';

const BASE_URL = `${environment.apiUrl}/admin`;

export type TenantStatus = 'TRIAL' | 'ACTIVE' | 'READ_ONLY' | 'SUSPENDED' | 'CANCELLED';

/**
 * Un établissement client, vu depuis la console plateforme. Aucun effectif : les données
 * scolaires sont protégées par Row-Level Security et restent invisibles à l'éditeur.
 */
export interface TenantAdmin {
  id: number;
  name: string;
  subdomain: string;
  customDomain: string | null;
  status: TenantStatus;
  createdAt: string;
  planCode: string | null;
  planName: string | null;
  planPriceCents: number | null;
  currency: string | null;
  subscriptionStatus: string | null;
  trialEndsAt: string | null;
  currentPeriodEnd: string | null;
  paymentFailedAt: string | null;
}

export interface PlanAdmin {
  id: number;
  code: string;
  name: string;
  priceCents: number;
  currency: string;
  maxStudents: number | null;
  tenantCount: number;
}

export interface PlatformAccount {
  id: number;
  email: string;
  firstName: string;
  lastName: string;
  createdAt: string;
}

export interface PlatformAction {
  id: number;
  tenantId: number | null;
  /** Nom de l'établissement concerné : « #9 » n'apprend rien à qui relit le journal. */
  tenantName: string | null;
  action: string;
  detail: string | null;
  reason: string | null;
  platformAdminId: number;
  createdAt: string;
}

export const TENANT_STATUSES: { value: TenantStatus; label: string }[] = [
  { value: 'TRIAL', label: "À l'essai" },
  { value: 'ACTIVE', label: 'Actif' },
  { value: 'READ_ONLY', label: 'Lecture seule' },
  { value: 'SUSPENDED', label: 'Suspendu' },
  { value: 'CANCELLED', label: 'Résilié' },
];

export function tenantStatusLabel(status: string): string {
  return TENANT_STATUSES.find((entry) => entry.value === status)?.label ?? status;
}

@Injectable({ providedIn: 'root' })
export class PlatformAdminService {
  private readonly http = inject(HttpClient);

  async listTenants(query: PageQuery, status: TenantStatus | null): Promise<Paged<TenantAdmin>> {
    const params = pageParams(query);
    if (status) {
      params['status'] = status;
    }
    const response = await firstValueFrom(
      this.http.get<ApiResponse<TenantAdmin[]>>(`${BASE_URL}/tenants`, { params }),
    );
    return { items: response.data, total: response.meta?.total ?? response.data.length };
  }

  async updateStatus(
    id: number,
    status: TenantStatus,
    reason: string | null,
  ): Promise<TenantAdmin> {
    const response = await firstValueFrom(
      this.http.put<ApiResponse<TenantAdmin>>(`${BASE_URL}/tenants/${id}/status`, {
        status,
        reason,
      }),
    );
    return response.data;
  }

  async extendTrial(id: number, days: number, reason: string | null): Promise<TenantAdmin> {
    const response = await firstValueFrom(
      this.http.put<ApiResponse<TenantAdmin>>(`${BASE_URL}/tenants/${id}/trial`, { days, reason }),
    );
    return response.data;
  }

  /**
   * Enregistre un règlement reçu en espèces : c'est ce geste qui ouvre ou rouvre l'accès
   * d'un établissement, la plateforme n'encaissant rien en ligne.
   */
  async recordPayment(
    id: number,
    planId: number,
    months: number,
    reason: string | null,
  ): Promise<TenantAdmin> {
    const response = await firstValueFrom(
      this.http.post<ApiResponse<TenantAdmin>>(`${BASE_URL}/tenants/${id}/payments`, {
        planId,
        months,
        reason,
      }),
    );
    return response.data;
  }

  async changePlan(id: number, planId: number, reason: string | null): Promise<TenantAdmin> {
    const response = await firstValueFrom(
      this.http.put<ApiResponse<TenantAdmin>>(`${BASE_URL}/tenants/${id}/plan`, { planId, reason }),
    );
    return response.data;
  }

  async tenantActions(id: number): Promise<PlatformAction[]> {
    const response = await firstValueFrom(
      this.http.get<ApiResponse<PlatformAction[]>>(`${BASE_URL}/tenants/${id}/actions`),
    );
    return response.data;
  }

  async listPlans(): Promise<PlanAdmin[]> {
    const response = await firstValueFrom(
      this.http.get<ApiResponse<PlanAdmin[]>>(`${BASE_URL}/plans`),
    );
    return response.data;
  }

  async listAccounts(): Promise<PlatformAccount[]> {
    const response = await firstValueFrom(
      this.http.get<ApiResponse<PlatformAccount[]>>(`${BASE_URL}/accounts`),
    );
    return response.data;
  }

  async createAccount(
    email: string,
    password: string,
    firstName: string,
    lastName: string,
  ): Promise<PlatformAccount> {
    const response = await firstValueFrom(
      this.http.post<ApiResponse<PlatformAccount>>(`${BASE_URL}/accounts`, {
        email,
        password,
        firstName,
        lastName,
      }),
    );
    return response.data;
  }

  async listActions(query: PageQuery): Promise<Paged<PlatformAction>> {
    const response = await firstValueFrom(
      this.http.get<ApiResponse<PlatformAction[]>>(`${BASE_URL}/actions`, {
        params: pageParams(query),
      }),
    );
    return { items: response.data, total: response.meta?.total ?? response.data.length };
  }
}
