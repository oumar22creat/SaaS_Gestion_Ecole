import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../environments/environment';
import { ApiResponse } from '../core/api-response.model';

export interface OnboardingStep {
  key: string;
  count: number;
  done: boolean;
}

export interface OnboardingStatus {
  steps: OnboardingStep[];
  complete: boolean;
}

@Injectable({ providedIn: 'root' })
export class OnboardingService {
  private readonly http = inject(HttpClient);

  async status(): Promise<OnboardingStatus> {
    const response = await firstValueFrom(
      this.http.get<ApiResponse<OnboardingStatus>>(`${environment.apiUrl}/onboarding/status`),
    );
    return response.data;
  }
}
