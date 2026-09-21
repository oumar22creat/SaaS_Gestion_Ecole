import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../environments/environment';
import { ApiResponse } from '../core/api-response.model';
import { StaffAccount, StaffAccountRequest } from './staff-account.model';

const BASE_URL = `${environment.apiUrl}/users`;

@Injectable({ providedIn: 'root' })
export class StaffAccountService {
  private readonly http = inject(HttpClient);

  async list(): Promise<StaffAccount[]> {
    const response = await firstValueFrom(
      this.http.get<ApiResponse<StaffAccount[]>>(BASE_URL, { params: { pageSize: 200 } }),
    );
    return response.data;
  }

  async create(request: StaffAccountRequest): Promise<StaffAccount> {
    const response = await firstValueFrom(this.http.post<ApiResponse<StaffAccount>>(BASE_URL, request));
    return response.data;
  }

  async setActive(id: number, active: boolean): Promise<StaffAccount> {
    const response = await firstValueFrom(
      this.http.put<ApiResponse<StaffAccount>>(`${BASE_URL}/${id}/status`, { active }),
    );
    return response.data;
  }

  async changeRole(id: number, role: string): Promise<StaffAccount> {
    const response = await firstValueFrom(
      this.http.put<ApiResponse<StaffAccount>>(`${BASE_URL}/${id}/role`, { role }),
    );
    return response.data;
  }

  async resetPassword(id: number, password: string): Promise<StaffAccount> {
    const response = await firstValueFrom(
      this.http.put<ApiResponse<StaffAccount>>(`${BASE_URL}/${id}/password`, { password }),
    );
    return response.data;
  }
}
