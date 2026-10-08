import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../environments/environment';
import { ApiResponse } from '../core/api-response.model';
import { withReadableBody } from '../core/http-error.util';

export interface TermReportSubjectLine {
  matiere: string;
  coefficient: number;
  moyenne: number | null;
  note_max: number | null;
  note_min: number | null;
  notes: number;
  atteignentLaMoyenne: number;
  tauxReussite: number | null;
}

/**
 * Rapport trimestriel d'une classe, tel qu'il part à la tutelle. Des agrégats et non des
 * noms : un rapport nominatif serait inutile à l'administration et diffuserait des données
 * d'enfants hors de l'école.
 */
export interface TermReport {
  className: string;
  headTeacherName: string;
  periodLabel: string;
  schoolYear: string;
  periodFrom: string;
  periodTo: string;
  effectif: { total: number; filles: number; garcons: number; evalues: number };
  resultats: {
    moyenneClasse: number | null;
    meilleureMoyenne: number | null;
    plusFaibleMoyenne: number | null;
    admis: number;
    tauxReussite: number | null;
    evaluesSansNote: number;
  };
  matieres: TermReportSubjectLine[];
  repartition: { libelle: string; effectif: number; part: number | null }[];
  assiduite: {
    absences: number;
    retards: number;
    absencesJustifiees: number;
    tauxPresence: number | null;
  };
  discipline: { incidents: number; sanctions: number; observationsPositives: number };
}

const BASE_URL = `${environment.apiUrl}/term-reports`;

@Injectable({ providedIn: 'root' })
export class TermReportService {
  private readonly http = inject(HttpClient);

  async preview(schoolClassId: number, periodLabel: string): Promise<TermReport> {
    const response = await firstValueFrom(
      this.http.get<ApiResponse<TermReport>>(`${BASE_URL}/classes/${schoolClassId}`, {
        params: { periodLabel },
      }),
    );
    return response.data;
  }

  async downloadPdf(schoolClassId: number, className: string, periodLabel: string): Promise<void> {
    let blob: Blob;
    try {
      blob = await firstValueFrom(
        this.http.get(`${BASE_URL}/classes/${schoolClassId}/pdf`, {
          params: { periodLabel },
          responseType: 'blob',
        }),
      );
    } catch (error) {
      throw await withReadableBody(error);
    }
    const url = URL.createObjectURL(blob);
    const anchor = document.createElement('a');
    anchor.href = url;
    anchor.download = `rapport-${className}-${periodLabel}.pdf`;
    anchor.click();
    URL.revokeObjectURL(url);
  }
}
