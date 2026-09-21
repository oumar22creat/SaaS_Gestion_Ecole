import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../environments/environment';
import { ApiResponse } from '../core/api-response.model';
import { TimetableEntry } from './timetable-entry.model';

@Injectable({ providedIn: 'root' })
export class TimetableEntryService {
  private readonly http = inject(HttpClient);

  async list(): Promise<TimetableEntry[]> {
    const response = await firstValueFrom(
      this.http.get<ApiResponse<TimetableEntry[]>>(`${environment.apiUrl}/timetable-entries`),
    );
    return response.data;
  }
}
