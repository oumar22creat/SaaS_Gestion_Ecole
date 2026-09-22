import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../environments/environment';
import { ApiResponse } from '../core/api-response.model';
import { PageQuery, Paged, pageParams } from '../core/paged.model';
import { Parent, ParentRequest, StudentParentLink, StudentParentLinkRequest } from './parent.model';

const BASE_URL = `${environment.apiUrl}/parents`;
const STUDENTS_URL = `${environment.apiUrl}/students`;

@Injectable({ providedIn: 'root' })
export class ParentService {
  private readonly http = inject(HttpClient);

  /**
   * Liste paginée et filtrable, pour l'écran de gestion. `list()` reste utilisé par les
   * écrans qui ont besoin de tout le référentiel d'un coup (feuille d'appel, saisie de
   * notes) et n'affichent pas de tableau paginé.
   */
  async page(query: PageQuery): Promise<Paged<Parent>> {
    const response = await firstValueFrom(
      this.http.get<ApiResponse<Parent[]>>(BASE_URL, { params: pageParams(query) }),
    );
    return { items: response.data, total: response.meta?.total ?? response.data.length };
  }

  async list(): Promise<Parent[]> {
    const response = await firstValueFrom(
      this.http.get<ApiResponse<Parent[]>>(BASE_URL, { params: { pageSize: 200 } }),
    );
    return response.data;
  }

  async create(request: ParentRequest): Promise<Parent> {
    const response = await firstValueFrom(this.http.post<ApiResponse<Parent>>(BASE_URL, request));
    return response.data;
  }

  async update(id: number, request: ParentRequest): Promise<Parent> {
    const response = await firstValueFrom(
      this.http.put<ApiResponse<Parent>>(`${BASE_URL}/${id}`, request),
    );
    return response.data;
  }

  async remove(id: number): Promise<void> {
    await firstValueFrom(this.http.delete<void>(`${BASE_URL}/${id}`));
  }

  async childrenOf(parentId: number): Promise<number[]> {
    const response = await firstValueFrom(
      this.http.get<ApiResponse<number[]>>(`${BASE_URL}/${parentId}/students`),
    );
    return response.data;
  }

  async listLinks(studentId: number): Promise<StudentParentLink[]> {
    const response = await firstValueFrom(
      this.http.get<ApiResponse<StudentParentLink[]>>(`${STUDENTS_URL}/${studentId}/parents`),
    );
    return response.data;
  }

  async link(studentId: number, request: StudentParentLinkRequest): Promise<StudentParentLink> {
    const response = await firstValueFrom(
      this.http.post<ApiResponse<StudentParentLink>>(
        `${STUDENTS_URL}/${studentId}/parents`,
        request,
      ),
    );
    return response.data;
  }

  async unlink(studentId: number, parentId: number): Promise<void> {
    await firstValueFrom(
      this.http.delete<void>(`${STUDENTS_URL}/${studentId}/parents/${parentId}`),
    );
  }
}
