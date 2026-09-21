import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../environments/environment';
import { ApiResponse } from '../core/api-response.model';

export type DocumentScope = 'SUBJECT' | 'CLASS' | 'SERVICE';

export interface SchoolDocument {
  id: number;
  title: string;
  scope: DocumentScope;
  subjectId: number | null;
  schoolClassId: number | null;
  serviceLabel: string | null;
  fileName: string;
  contentType: string;
  sizeBytes: number;
  archived: boolean;
  createdAt: string;
  visibleRoles: string[];
}

const BASE_URL = `${environment.apiUrl}/documents`;

@Injectable({ providedIn: 'root' })
export class DocumentService {
  private readonly http = inject(HttpClient);

  async listByClass(schoolClassId: number): Promise<SchoolDocument[]> {
    const response = await firstValueFrom(
      this.http.get<ApiResponse<SchoolDocument[]>>(BASE_URL, { params: { schoolClassId } }),
    );
    return response.data;
  }

  async upload(formData: FormData): Promise<SchoolDocument> {
    const response = await firstValueFrom(
      this.http.post<ApiResponse<SchoolDocument>>(BASE_URL, formData),
    );
    return response.data;
  }

  async download(id: number, fileName: string): Promise<void> {
    const blob = await firstValueFrom(
      this.http.get(`${BASE_URL}/${id}/download`, { responseType: 'blob' }),
    );
    const url = URL.createObjectURL(blob);
    const anchor = document.createElement('a');
    anchor.href = url;
    anchor.download = fileName;
    anchor.click();
    URL.revokeObjectURL(url);
  }

  async archive(id: number): Promise<void> {
    await firstValueFrom(this.http.delete(`${BASE_URL}/${id}`));
  }
}
