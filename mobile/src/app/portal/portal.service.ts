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
}
