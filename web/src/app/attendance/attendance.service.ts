import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../environments/environment';
import { ApiResponse } from '../core/api-response.model';
import { AttendanceRecord, AttendanceRecordChange, RollCallRequest } from './attendance.model';

const BASE_URL = `${environment.apiUrl}/attendance`;

@Injectable({ providedIn: 'root' })
export class AttendanceService {
  private readonly http = inject(HttpClient);

  async submitRollCall(request: RollCallRequest): Promise<AttendanceRecord[]> {
    const response = await firstValueFrom(this.http.post<ApiResponse<AttendanceRecord[]>>(`${BASE_URL}/roll-call`, request));
    return response.data;
  }

  async listForClassAndDate(schoolClassId: number, date: string): Promise<AttendanceRecord[]> {
    const response = await firstValueFrom(
      this.http.get<ApiResponse<AttendanceRecord[]>>(BASE_URL, { params: { schoolClassId, date } }),
    );
    return response.data;
  }

  async history(recordId: number): Promise<AttendanceRecordChange[]> {
    const response = await firstValueFrom(
      this.http.get<ApiResponse<AttendanceRecordChange[]>>(`${BASE_URL}/${recordId}/history`),
    );
    return response.data;
  }
}
