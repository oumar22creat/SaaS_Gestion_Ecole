import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../environments/environment';
import { ApiResponse } from '../core/api-response.model';

export interface BillingPlan {
  code: string;
  name: string;
  priceCents: number;
  currency: string;
  maxStudents: number | null;
  purchasable: boolean;
}

export interface Subscription {
  status: string;
  planCode: string;
  planName: string;
  trialEndsAt: string | null;
  currentPeriodEnd: string | null;
}

@Injectable({ providedIn: 'root' })
export class BillingService {
  private readonly http = inject(HttpClient);

  async plans(): Promise<BillingPlan[]> {
    const response = await firstValueFrom(
      this.http.get<ApiResponse<BillingPlan[]>>(`${environment.apiUrl}/billing/plans`),
    );
    return response.data;
  }

  async subscription(): Promise<Subscription> {
    const response = await firstValueFrom(
      this.http.get<ApiResponse<Subscription>>(`${environment.apiUrl}/billing/subscription`),
    );
    return response.data;
  }

  async checkout(planCode: string): Promise<string> {
    const response = await firstValueFrom(
      this.http.post<ApiResponse<{ checkoutUrl: string }>>(
        `${environment.apiUrl}/billing/checkout`,
        { planCode },
      ),
    );
    return response.data.checkoutUrl;
  }
}
