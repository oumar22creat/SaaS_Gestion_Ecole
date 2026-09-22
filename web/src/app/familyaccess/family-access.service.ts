import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../environments/environment';
import { ApiResponse } from '../core/api-response.model';

/** Qui est concerné par l'ouverture d'accès : l'élève lui-même, ou son parent. */
export type FamilyAccessTarget = 'student' | 'parent';

export interface FamilyAccount {
  id: number;
  email: string;
  firstName: string;
  lastName: string;
  role: string;
  active: boolean;
}

/**
 * Ouverture d'un accès au portail mobile pour une famille. Le point d'entrée dépend de la
 * fiche concernée : un accès part toujours d'un élève ou d'un parent déjà enregistré, jamais
 * d'un compte créé de zéro.
 */
@Injectable({ providedIn: 'root' })
export class FamilyAccessService {
  private readonly http = inject(HttpClient);

  async openAccess(
    target: FamilyAccessTarget,
    recordId: number,
    email: string,
    password: string,
  ): Promise<FamilyAccount> {
    const path = target === 'student' ? 'students' : 'parents';
    const response = await firstValueFrom(
      this.http.post<ApiResponse<FamilyAccount>>(
        `${environment.apiUrl}/${path}/${recordId}/account`,
        { email, password },
      ),
    );
    return response.data;
  }
}
