import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { Paged } from '../core/paged.model';
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

  /**
   * Les impayés de cantine, page par page. C'est la liste qui grossit le plus vite de
   * l'application : une facture par élève et par semaine, soit plusieurs milliers de lignes
   * par trimestre dans une école de mille élèves.
   */
  async unpaid(page: number, pageSize: number): Promise<Paged<CanteenInvoice>> {
    const response = await firstValueFrom(
      this.http.get<ApiResponse<CanteenInvoice[]>>(`${environment.apiUrl}/canteen/invoices/unpaid`, {
        params: { page: String(page), size: String(pageSize) },
      }),
    );
    return { items: response.data, total: response.meta?.total ?? response.data.length };
  }
}
