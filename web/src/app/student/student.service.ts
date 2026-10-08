import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../environments/environment';
import { ApiResponse } from '../core/api-response.model';
import { PageQuery, Paged, pageParams } from '../core/paged.model';
import { PARAMS_REFERENTIEL } from '../core/referentiel.params';
import { Student, StudentImportResult, StudentRequest } from './student.model';

const BASE_URL = `${environment.apiUrl}/students`;

/** Déclenche le téléchargement d'un blob puis libère l'URL, qui resterait sinon en mémoire. */
export function telecharger(blob: Blob, nomFichier: string): void {
  const url = URL.createObjectURL(blob);
  const anchor = document.createElement('a');
  anchor.href = url;
  anchor.download = nomFichier;
  anchor.click();
  URL.revokeObjectURL(url);
}

@Injectable({ providedIn: 'root' })
export class StudentService {
  private readonly http = inject(HttpClient);

  /**
   * Liste paginée et filtrable, pour l'écran de gestion. `list()` reste utilisé par les
   * écrans qui ont besoin de tout le référentiel d'un coup (feuille d'appel, saisie de
   * notes) et n'affichent pas de tableau paginé.
   */
  async page(query: PageQuery): Promise<Paged<Student>> {
    const response = await firstValueFrom(
      this.http.get<ApiResponse<Student[]>>(BASE_URL, { params: pageParams(query) }),
    );
    return { items: response.data, total: response.meta?.total ?? response.data.length };
  }

  async list(): Promise<Student[]> {
    const response = await firstValueFrom(
      this.http.get<ApiResponse<Student[]>>(BASE_URL, { params: PARAMS_REFERENTIEL }),
    );
    return response.data;
  }

  /**
   * Portrait de l'élève, pour sa carte d'identité scolaire. Le fichier est téléversé et
   * stocké par le serveur : les PDF sont fabriqués côté serveur, qui ne va jamais chercher
   * une adresse fournie par le navigateur.
   */
  async uploadPhoto(id: number, file: File): Promise<Student> {
    const formData = new FormData();
    formData.append('file', file);
    const response = await firstValueFrom(
      this.http.post<ApiResponse<Student>>(`${BASE_URL}/${id}/photo`, formData),
    );
    return response.data;
  }

  async removePhoto(id: number): Promise<Student> {
    const response = await firstValueFrom(
      this.http.delete<ApiResponse<Student>>(`${BASE_URL}/${id}/photo`),
    );
    return response.data;
  }

  /** Le portrait lui-même, en objet URL — à libérer par l'appelant. Null s'il n'y en a pas. */
  async loadPhotoPreview(id: number): Promise<string | null> {
    try {
      const blob = await firstValueFrom(
        this.http.get(`${BASE_URL}/${id}/photo`, { responseType: 'blob' }),
      );
      return URL.createObjectURL(blob);
    } catch {
      // 404 : pas de portrait. Ce n'est pas une erreur à remonter à l'utilisateur.
      return null;
    }
  }

  async downloadIdCard(student: Student): Promise<void> {
    const blob = await firstValueFrom(
      this.http.get(`${BASE_URL}/${student.id}/id-card.pdf`, { responseType: 'blob' }),
    );
    telecharger(blob, `carte-${student.lastName}-${student.firstName}.pdf`);
  }

  async create(request: StudentRequest): Promise<Student> {
    const response = await firstValueFrom(this.http.post<ApiResponse<Student>>(BASE_URL, request));
    return response.data;
  }

  async update(id: number, request: StudentRequest): Promise<Student> {
    const response = await firstValueFrom(
      this.http.put<ApiResponse<Student>>(`${BASE_URL}/${id}`, request),
    );
    return response.data;
  }

  async deactivate(id: number): Promise<void> {
    await firstValueFrom(this.http.delete<void>(`${BASE_URL}/${id}`));
  }

  async importCsv(file: File): Promise<StudentImportResult> {
    const formData = new FormData();
    formData.append('file', file);
    const response = await firstValueFrom(
      this.http.post<ApiResponse<StudentImportResult>>(`${BASE_URL}/import`, formData),
    );
    return response.data;
  }
}
