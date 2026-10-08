import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../environments/environment';
import { ApiResponse } from '../core/api-response.model';

const BASE_URL = `${environment.apiUrl}/portal`;

/** Élève consultable par le compte connecté : un enfant pour un parent, soi-même pour un élève. */
export interface PortalChild {
  id: number;
  firstName: string;
  lastName: string;
  studentNumber: string;
  schoolClassId: number | null;
}

export interface PortalGrade {
  examId: number;
  label: string;
  examDate: string;
  score: number | null;
  maxScore: number;
  coefficient: number;
  absent: boolean;
}

export interface PortalAttendance {
  date: string;
  status: string;
  reason: string | null;
  justified: boolean;
}

/** Une échéance de frais, telle que la famille la lit. */
export interface PortalFeeLine {
  label: string;
  dueDate: string | null;
  amountDueCents: number;
  amountPaidCents: number;
  amountRemainingCents: number;
  status: string;
  overdue: boolean;
}

export interface PortalFeeSummary {
  totalDueCents: number;
  totalPaidCents: number;
  totalRemainingCents: number;
  overdueCents: number;
  currency: string;
  lines: PortalFeeLine[];
}

/**
 * Un règlement encaissé. La scolarité se paie souvent en espèces au guichet, parfois par un
 * proche : le parent qui ne s'est pas déplacé n'avait aucune trace de ce qui a été versé en
 * son nom, ni de ce qui restait dû.
 */
export interface PortalReceipt {
  paymentId: number;
  paidOn: string;
  label: string;
  amountCents: number;
  method: string;
  reference: string | null;
}

export interface PortalTimetableSlot {
  dayOfWeek: string;
  startTime: string;
  endTime: string;
  subject: string;
  teacher: string;
  room: string;
}

@Injectable({ providedIn: 'root' })
export class PortalService {
  private readonly http = inject(HttpClient);

  async children(): Promise<PortalChild[]> {
    const response = await firstValueFrom(
      this.http.get<ApiResponse<PortalChild[]>>(`${BASE_URL}/children`),
    );
    return response.data;
  }

  async grades(studentId: number): Promise<PortalGrade[]> {
    const response = await firstValueFrom(
      this.http.get<ApiResponse<PortalGrade[]>>(`${BASE_URL}/students/${studentId}/grades`),
    );
    return response.data;
  }

  async fees(studentId: number): Promise<PortalFeeSummary> {
    const response = await firstValueFrom(
      this.http.get<ApiResponse<PortalFeeSummary>>(`${BASE_URL}/students/${studentId}/fees`),
    );
    return response.data;
  }

  async timetable(studentId: number): Promise<PortalTimetableSlot[]> {
    const response = await firstValueFrom(
      this.http.get<ApiResponse<PortalTimetableSlot[]>>(
        `${BASE_URL}/students/${studentId}/timetable`,
      ),
    );
    return response.data;
  }

  async attendance(studentId: number, from: string, to: string): Promise<PortalAttendance[]> {
    const response = await firstValueFrom(
      this.http.get<ApiResponse<PortalAttendance[]>>(
        `${BASE_URL}/students/${studentId}/attendance`,
        { params: { from, to } },
      ),
    );
    return response.data;
  }

  async receipts(studentId: number): Promise<PortalReceipt[]> {
    const response = await firstValueFrom(
      this.http.get<ApiResponse<PortalReceipt[]>>(`${BASE_URL}/students/${studentId}/receipts`),
    );
    return response.data;
  }

  /**
   * Ouvre le reçu dans le lecteur PDF du téléphone.
   *
   * <p>Le fichier est récupéré avec le jeton d'authentification puis ouvert depuis la
   * mémoire : donner l'URL directement au navigateur ferait une requête sans en-tête
   * Authorization, donc un refus — et la famille verrait une page de connexion à la place
   * de son reçu.
   */
  async openReceipt(paymentId: number): Promise<void> {
    const blob = await firstValueFrom(
      this.http.get(`${BASE_URL}/payments/${paymentId}/receipt.pdf`, { responseType: 'blob' }),
    );
    const url = URL.createObjectURL(blob);
    window.open(url, '_blank');
    // Révocation différée : révoquer tout de suite couperait le chargement du lecteur PDF.
    setTimeout(() => URL.revokeObjectURL(url), 60_000);
  }
}
