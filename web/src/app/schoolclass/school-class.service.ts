import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../environments/environment';
import { ApiResponse } from '../core/api-response.model';
import { PARAMS_REFERENTIEL } from '../core/referentiel.params';
import {
  ClassSubjectAssignment,
  ClassSubjectAssignmentRequest,
  SchoolClass,
  SchoolClassRequest,
} from './school-class.model';

const BASE_URL = `${environment.apiUrl}/classes`;

@Injectable({ providedIn: 'root' })
export class SchoolClassService {
  private readonly http = inject(HttpClient);

  async list(): Promise<SchoolClass[]> {
    const response = await firstValueFrom(
      this.http.get<ApiResponse<SchoolClass[]>>(BASE_URL, { params: PARAMS_REFERENTIEL }),
    );
    return response.data;
  }

  async create(request: SchoolClassRequest): Promise<SchoolClass> {
    const response = await firstValueFrom(
      this.http.post<ApiResponse<SchoolClass>>(BASE_URL, request),
    );
    return response.data;
  }

  async update(id: number, request: SchoolClassRequest): Promise<SchoolClass> {
    const response = await firstValueFrom(
      this.http.put<ApiResponse<SchoolClass>>(`${BASE_URL}/${id}`, request),
    );
    return response.data;
  }

  async remove(id: number): Promise<void> {
    await firstValueFrom(this.http.delete<void>(`${BASE_URL}/${id}`));
  }

  async listAssignments(classId: number): Promise<ClassSubjectAssignment[]> {
    const response = await firstValueFrom(
      this.http.get<ApiResponse<ClassSubjectAssignment[]>>(`${BASE_URL}/${classId}/subjects`),
    );
    return response.data;
  }

  async assign(
    classId: number,
    request: ClassSubjectAssignmentRequest,
  ): Promise<ClassSubjectAssignment> {
    const response = await firstValueFrom(
      this.http.post<ApiResponse<ClassSubjectAssignment>>(
        `${BASE_URL}/${classId}/subjects`,
        request,
      ),
    );
    return response.data;
  }

  async unassign(classId: number, subjectId: number): Promise<void> {
    await firstValueFrom(this.http.delete<void>(`${BASE_URL}/${classId}/subjects/${subjectId}`));
  }

  /**
   * Les cartes d'identité de toute la classe, en une planche de huit par page A4.
   *
   * <p>Le PDF est fabriqué par le serveur, qui y pose les portraits : les élèves sans photo
   * obtiennent un cadre vide plutôt que de faire échouer le tirage de la classe entière.
   */
  async downloadIdCards(schoolClass: { id: number; name: string }): Promise<void> {
    const blob = await firstValueFrom(
      this.http.get(`${environment.apiUrl}/classes/${schoolClass.id}/id-cards.pdf`, {
        responseType: 'blob',
      }),
    );
    const url = URL.createObjectURL(blob);
    const anchor = document.createElement('a');
    anchor.href = url;
    anchor.download = `cartes-${schoolClass.name}.pdf`;
    anchor.click();
    URL.revokeObjectURL(url);
  }
}
