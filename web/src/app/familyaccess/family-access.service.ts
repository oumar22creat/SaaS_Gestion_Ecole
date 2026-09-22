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

  /**
   * Réinitialise le mot de passe de l'accès. L'appel part de la fiche et non du compte : c'est
   * depuis la fiche que le secrétariat travaille, et la liste des élèves n'a ainsi jamais
   * besoin de porter l'identifiant du compte.
   */
  async resetPassword(
    target: FamilyAccessTarget,
    recordId: number,
    password: string,
  ): Promise<void> {
    const path = target === 'student' ? 'students' : 'parents';
    await firstValueFrom(
      this.http.put(`${environment.apiUrl}/${path}/${recordId}/account/password`, { password }),
    );
  }

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
