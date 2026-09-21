import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../environments/environment';
import { ApiResponse } from '../core/api-response.model';
import { TimetableEntry, TimetableEntryRequest } from './timetable-entry.model';

const BASE_URL = `${environment.apiUrl}/timetable-entries`;

@Injectable({ providedIn: 'root' })
export class TimetableEntryService {
  private readonly http = inject(HttpClient);

  async list(): Promise<TimetableEntry[]> {
    const response = await firstValueFrom(this.http.get<ApiResponse<TimetableEntry[]>>(BASE_URL));
    return response.data;
  }

  async create(request: TimetableEntryRequest): Promise<TimetableEntry> {
    const response = await firstValueFrom(
      this.http.post<ApiResponse<TimetableEntry>>(BASE_URL, request),
    );
    return response.data;
  }

  async update(id: number, request: TimetableEntryRequest): Promise<TimetableEntry> {
    const response = await firstValueFrom(
      this.http.put<ApiResponse<TimetableEntry>>(`${BASE_URL}/${id}`, request),
    );
    return response.data;
  }

  async remove(id: number): Promise<void> {
    await firstValueFrom(this.http.delete<void>(`${BASE_URL}/${id}`));
  }
}
