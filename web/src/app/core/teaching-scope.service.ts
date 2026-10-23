import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../environments/environment';
import { AuthTokenService } from '../auth/auth-token.service';
import { ApiResponse } from './api-response.model';

/** Réponse de `GET /api/v1/me/teaching-scope` (ADR-030). */
export interface TeachingScopeResponse {
  restricted: boolean;
  classIds: number[];
  headClassIds: number[];
  classSubjects: { classId: number; subjectId: number }[];
}

/** Rôles qui ont la main sur tout l'établissement pour les bulletins et les annonces. */
export const DIRECTION_ROLES = ['ADMIN', 'DIRECTION'];

/**
 * Périmètre pédagogique de l'utilisateur connecté, pour l'AFFICHAGE uniquement : masquer un
 * bouton que le serveur refuserait (403 `OUT_OF_TEACHER_SCOPE`). Aucune valeur de sécurité :
 * chaque action reste contrôlée par `TeacherScope` côté serveur, et les écrans gardent un
 * message d'erreur explicite si un 403 survient malgré tout.
 */
export class TeachingScope {
  private readonly headClasses: Set<number>;
  private readonly pairs: Set<string>;

  constructor(
    readonly role: string | null,
    readonly restricted: boolean,
    headClassIds: number[] = [],
    classSubjects: { classId: number; subjectId: number }[] = [],
  ) {
    this.headClasses = new Set(headClassIds);
    this.pairs = new Set(classSubjects.map((p) => `${p.classId}:${p.subjectId}`));
  }

  /** Génération des bulletins, appréciation générale, décision du conseil. */
  canManageReportCards(classId: number | null): boolean {
    if (classId === null) {
      return false;
    }
    if (DIRECTION_ROLES.includes(this.role ?? '')) {
      return true;
    }
    return this.restricted && this.headClasses.has(classId);
  }

  /** Évaluations, séances du cahier de textes, appréciation d'une matière. */
  teaches(classId: number | null, subjectId: number | null): boolean {
    if (classId === null || subjectId === null) {
      return false;
    }
    return !this.restricted || this.pairs.has(`${classId}:${subjectId}`);
  }

  /** Matières proposables pour une classe : toutes hors enseignant, les siennes sinon. */
  subjectsFor<T extends { id: number }>(classId: number | null, subjects: T[]): T[] {
    if (!this.restricted) {
      return subjects;
    }
    return subjects.filter((subject) => this.teaches(classId, subject.id));
  }

  /** Classes où l'enseignant a au moins une matière (un professeur principal peut ne rien y enseigner). */
  classesWithSubjects<T extends { id: number }>(classes: T[]): T[] {
    if (!this.restricted) {
      return classes;
    }
    const taught = new Set([...this.pairs].map((key) => Number(key.split(':')[0])));
    return classes.filter((schoolClass) => taught.has(schoolClass.id));
  }
}

@Injectable({ providedIn: 'root' })
export class TeachingScopeService {
  private readonly http = inject(HttpClient);
  private readonly authTokenService = inject(AuthTokenService);

  /**
   * Seul le rôle TEACHER est restreint : les autres n'appellent pas l'API. En cas d'échec pour
   * un enseignant, périmètre vide (on masque plutôt que d'afficher des actions refusées).
   */
  async current(): Promise<TeachingScope> {
    const role = this.authTokenService.role();
    if (role !== 'TEACHER') {
      return new TeachingScope(role, false);
    }
    try {
      const response = await firstValueFrom(
        this.http.get<ApiResponse<TeachingScopeResponse>>(
          `${environment.apiUrl}/me/teaching-scope`,
        ),
      );
      const scope = response.data;
      return new TeachingScope(role, scope.restricted, scope.headClassIds, scope.classSubjects);
    } catch {
      return new TeachingScope(role, true);
    }
  }
}
