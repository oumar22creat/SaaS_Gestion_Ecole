import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../environments/environment';
import { ApiResponse } from '../core/api-response.model';

export interface FeeSchedule {
  id: number;
  schoolClassId: number;
  label: string;
  amountCents: number;
  currency: string;
  dueDate: string;
}

export interface FeeInvoice {
  id: number;
  feeScheduleId: number;
  studentId: number;
  amountDueCents: number;
  amountPaidCents: number;
  status: string;
}

export interface FeeReporting {
  totalDueCents: number;
  totalPaidCents: number;
  totalOutstandingCents: number;
  unpaidInvoices: FeeInvoice[];
}

@Injectable({ providedIn: 'root' })
export class SchoolFeesService {
  private readonly http = inject(HttpClient);

  async listSchedules(schoolClassId: number): Promise<FeeSchedule[]> {
    const response = await firstValueFrom(
      this.http.get<ApiResponse<FeeSchedule[]>>(`${environment.apiUrl}/school-fees/schedules`, {
        params: { schoolClassId },
      }),
    );
    return response.data;
  }

  async createSchedule(schoolClassId: number, label: string, amountCents: number, dueDate: string): Promise<FeeSchedule> {
    const response = await firstValueFrom(
      this.http.post<ApiResponse<FeeSchedule>>(`${environment.apiUrl}/school-fees/schedules`, {
        schoolClassId,
        label,
        amountCents,
        dueDate,
      }),
    );
    return response.data;
  }

  async generateInvoices(scheduleId: number): Promise<FeeInvoice[]> {
    const response = await firstValueFrom(
      this.http.post<ApiResponse<FeeInvoice[]>>(
        `${environment.apiUrl}/school-fees/schedules/${scheduleId}/generate-invoices`,
        {},
      ),
    );
    return response.data;
  }

  async recordPayment(invoiceId: number, amountCents: number, method: string): Promise<void> {
    await firstValueFrom(
      this.http.post(`${environment.apiUrl}/school-fees/invoices/${invoiceId}/payments`, {
        amountCents,
        method,
        reference: null,
      }),
    );
  }

  async reporting(schoolClassId: number, from: string, to: string): Promise<FeeReporting> {
    const response = await firstValueFrom(
      this.http.get<ApiResponse<FeeReporting>>(`${environment.apiUrl}/school-fees/reporting`, {
        params: { schoolClassId, from, to },
      }),
    );
    return response.data;
  }
}
