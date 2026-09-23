import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../environments/environment';
import { ApiResponse } from '../core/api-response.model';

const BASE_URL = `${environment.apiUrl}/school-fees`;

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

/** Une facture non soldée, telle que l'écran la présente : un élève nommé, une dette datée. */
export interface FeeOutstanding {
  invoiceId: number;
  studentId: number;
  studentName: string;
  studentNumber: string;
  schoolClassId: number | null;
  className: string;
  scheduleLabel: string;
  dueDate: string | null;
  amountDueCents: number;
  amountPaidCents: number;
  amountRemainingCents: number;
  status: string;
  overdue: boolean;
  daysLate: number;
}

export interface FeeCollectionByMethod {
  method: string;
  amountCents: number;
  paymentCount: number;
}

export interface FeeSummary {
  invoicedCents: number;
  collectedCents: number;
  outstandingCents: number;
  overdueCents: number;
  collectionRate: number | null;
  invoiceCount: number;
  settledInvoiceCount: number;
  overdueInvoiceCount: number;
  lateStudentCount: number;
  currency: string;
  collectionByMethod: FeeCollectionByMethod[];
}

export interface FeePaymentJournalEntry {
  paymentId: number;
  invoiceId: number;
  studentId: number | null;
  studentName: string;
  className: string;
  scheduleLabel: string;
  amountCents: number;
  method: string;
  reference: string | null;
  recordedByUserId: number | null;
  paidAt: string;
}

/** Moyens de paiement acceptés à la saisie — miroir de l'énumération FeePaymentMethod. */
export const PAYMENT_METHODS: { value: string; label: string }[] = [
  { value: 'CASH', label: 'Espèces' },
  { value: 'MOBILE_MONEY', label: 'Mobile Money' },
  { value: 'BANK_TRANSFER', label: 'Virement bancaire' },
  { value: 'OTHER', label: 'Autre' },
];

export function paymentMethodLabel(method: string): string {
  return PAYMENT_METHODS.find((entry) => entry.value === method)?.label ?? method;
}

@Injectable({ providedIn: 'root' })
export class SchoolFeesService {
  private readonly http = inject(HttpClient);

  async listSchedules(schoolClassId: number): Promise<FeeSchedule[]> {
    const response = await firstValueFrom(
      this.http.get<ApiResponse<FeeSchedule[]>>(`${BASE_URL}/schedules`, {
        params: { schoolClassId },
      }),
    );
    return response.data;
  }

  async createSchedule(
    schoolClassId: number,
    label: string,
    amountCents: number,
    dueDate: string,
  ): Promise<FeeSchedule> {
    const response = await firstValueFrom(
      this.http.post<ApiResponse<FeeSchedule>>(`${BASE_URL}/schedules`, {
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
        `${BASE_URL}/schedules/${scheduleId}/generate-invoices`,
        {},
      ),
    );
    return response.data;
  }

  async recordPayment(
    invoiceId: number,
    amountCents: number,
    method: string,
    reference: string | null,
  ): Promise<void> {
    await firstValueFrom(
      this.http.post(`${BASE_URL}/invoices/${invoiceId}/payments`, {
        amountCents,
        method,
        reference,
      }),
    );
  }

  /** Sans `schoolClassId`, la vue porte sur tout l'établissement. */
  async outstanding(schoolClassId: number | null, onlyOverdue: boolean): Promise<FeeOutstanding[]> {
    const params: Record<string, string> = { onlyOverdue: String(onlyOverdue) };
    if (schoolClassId !== null) {
      params['schoolClassId'] = String(schoolClassId);
    }
    const response = await firstValueFrom(
      this.http.get<ApiResponse<FeeOutstanding[]>>(`${BASE_URL}/outstanding`, { params }),
    );
    return response.data;
  }

  /** Relance par SMS ; renvoie le nombre de familles réellement notifiées. */
  async remindOverdueFamilies(schoolClassId: number | null): Promise<number> {
    const params: Record<string, string> =
      schoolClassId === null ? {} : { schoolClassId: String(schoolClassId) };
    const response = await firstValueFrom(
      this.http.post<ApiResponse<number>>(`${BASE_URL}/reminders`, {}, { params }),
    );
    return response.data;
  }

  async summary(schoolClassId: number | null): Promise<FeeSummary> {
    const params: Record<string, string> =
      schoolClassId === null ? {} : { schoolClassId: String(schoolClassId) };
    const response = await firstValueFrom(
      this.http.get<ApiResponse<FeeSummary>>(`${BASE_URL}/summary`, { params }),
    );
    return response.data;
  }

  async paymentJournal(from: string, to: string): Promise<FeePaymentJournalEntry[]> {
    const response = await firstValueFrom(
      this.http.get<ApiResponse<FeePaymentJournalEntry[]>>(`${BASE_URL}/payments`, {
        params: { from, to },
      }),
    );
    return response.data;
  }
}
