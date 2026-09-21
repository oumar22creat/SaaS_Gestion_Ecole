import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../environments/environment';
import { ApiResponse } from '../core/api-response.model';

export interface Menu {
  id: number;
  date: string;
  mainDescription: string;
  specialDietDescription: string | null;
}

export interface CanteenInvoice {
  id: number;
  studentId: number;
  amountDueCents: number;
  amountPaidCents: number;
  status: string;
}

@Injectable({ providedIn: 'root' })
export class CanteenService {
  private readonly http = inject(HttpClient);

  async listMenus(from: string, to: string): Promise<Menu[]> {
    const response = await firstValueFrom(
      this.http.get<ApiResponse<Menu[]>>(`${environment.apiUrl}/canteen/menus`, {
        params: { from, to },
      }),
    );
    return response.data;
  }

  async upsertMenu(
    date: string,
    mainDescription: string,
    specialDietDescription: string | null,
  ): Promise<Menu> {
    const response = await firstValueFrom(
      this.http.post<ApiResponse<Menu>>(`${environment.apiUrl}/canteen/menus`, {
        date,
        mainDescription,
        specialDietDescription,
      }),
    );
    return response.data;
  }

  async reserve(studentId: number, date: string, specialDiet: boolean): Promise<void> {
    await firstValueFrom(
      this.http.post(`${environment.apiUrl}/canteen/reservations`, {
        studentId,
        date,
        specialDiet,
      }),
    );
  }

  async unpaid(): Promise<CanteenInvoice[]> {
    const response = await firstValueFrom(
      this.http.get<ApiResponse<CanteenInvoice[]>>(`${environment.apiUrl}/canteen/invoices/unpaid`),
    );
    return response.data;
  }
}
