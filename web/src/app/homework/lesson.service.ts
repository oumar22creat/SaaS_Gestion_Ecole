import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../environments/environment';
import { ApiResponse } from '../core/api-response.model';

export interface Lesson {
  id: number;
  schoolClassId: number;
  subjectId: number;
  sessionDate: string;
  content: string;
  homework: string | null;
  homeworkDueDate: string | null;
  attachmentDocumentId: number | null;
}

export interface LessonRequest {
  schoolClassId: number;
  subjectId: number;
  sessionDate: string;
  content: string;
  homework: string | null;
  homeworkDueDate: string | null;
  attachmentDocumentId: number | null;
}

const BASE_URL = `${environment.apiUrl}/lessons`;

@Injectable({ providedIn: 'root' })
export class LessonService {
  private readonly http = inject(HttpClient);

  async list(schoolClassId: number, from: string, to: string): Promise<Lesson[]> {
    const response = await firstValueFrom(
      this.http.get<ApiResponse<Lesson[]>>(BASE_URL, { params: { schoolClassId, from, to } }),
    );
    return response.data;
  }

  async create(request: LessonRequest): Promise<Lesson> {
    const response = await firstValueFrom(this.http.post<ApiResponse<Lesson>>(BASE_URL, request));
    return response.data;
  }

  async remove(id: number): Promise<void> {
    await firstValueFrom(this.http.delete(`${BASE_URL}/${id}`));
  }
}
