import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatTableModule } from '@angular/material/table';
import { extractErrorMessage } from '../core/http-error.util';
import { SchoolClass } from '../schoolclass/school-class.model';
import { SchoolClassService } from '../schoolclass/school-class.service';
import { TermReport, TermReportService } from './term-report.service';

/**
 * Rapport trimestriel d'une classe, à adresser à la tutelle.
 *
 * <p>Les chiffres sont affichés avant d'être exportés : un document qui part au ministère
 * se relit, et un directeur doit pouvoir constater qu'une discipline est sous la moyenne
 * avant d'envoyer le PDF, pas après.
 */
@Component({
  selector: 'app-term-report-page',
  imports: [
    FormsModule,
    MatFormFieldModule,
    MatSelectModule,
    MatInputModule,
    MatButtonModule,
    MatTableModule,
    MatIconModule,
  ],
  template: `
    <div class="page-header">
      <h1>
        <mat-icon class="page-icon" aria-hidden="true">assignment</mat-icon>Rapport trimestriel
      </h1>
    </div>
    <p class="page-intro">
      Synthèse d'une classe sur un trimestre, destinée à la tutelle. Elle reprend les
      bulletins déjà générés pour la période : les deux documents disent donc la même chose.
    </p>

    <div class="filters-row">
      <mat-form-field appearance="outline">
        <mat-label>Classe</mat-label>
        <mat-select [(ngModel)]="schoolClassId">
          @for (schoolClass of classes(); track schoolClass.id) {
            <mat-option [value]="schoolClass.id">{{ schoolClass.name }}</mat-option>
          }
        </mat-select>
      </mat-form-field>
      <mat-form-field appearance="outline">
        <mat-label>Période</mat-label>
        <input matInput [(ngModel)]="periodLabel" placeholder="1er trimestre" />
        <mat-hint>Le libellé doit être celui des bulletins générés.</mat-hint>
      </mat-form-field>
      <button mat-flat-button [disabled]="loading()" (click)="load()">
        <mat-icon>search</mat-icon> Calculer
      </button>
      <button mat-stroked-button [disabled]="!report() || exporting()" (click)="exportPdf()">
        <mat-icon>picture_as_pdf</mat-icon>
        {{ exporting() ? 'Préparation…' : 'Télécharger le rapport' }}
      </button>
    </div>

    @if (errorMessage()) {
      <p class="flash-error">{{ errorMessage() }}</p>
    }

    @if (report(); as rapport) {
      <section class="stat-grid">
        <div class="stat"><span class="stat-label">Effectif</span><span class="stat-value">{{ rapport.effectif.total }}</span></div>
        <div class="stat"><span class="stat-label">Filles / garçons</span><span class="stat-value">{{ rapport.effectif.filles }} / {{ rapport.effectif.garcons }}</span></div>
        <div class="stat"><span class="stat-label">Moyenne de classe</span><span class="stat-value">{{ rapport.resultats.moyenneClasse ?? '—' }} / 20</span></div>
        <div class="stat"><span class="stat-label">Taux de réussite</span><span class="stat-value">{{ pourcent(rapport.resultats.tauxReussite) }}</span></div>
        <div class="stat"><span class="stat-label">Taux de présence</span><span class="stat-value">{{ pourcent(rapport.assiduite.tauxPresence) }}</span></div>
        <div class="stat"><span class="stat-label">Incidents</span><span class="stat-value">{{ rapport.discipline.incidents }}</span></div>
      </section>

      @if (rapport.resultats.evaluesSansNote > 0) {
        <p class="flash-info">
          {{ rapport.resultats.evaluesSansNote }} élève(s) sans aucune note sur la période.
          Ils ne comptent pas dans le taux de réussite : un élève non évalué n'a pas échoué.
        </p>
      }

      <h2 class="section-label">Résultats par discipline</h2>
      <div class="table-scroll">
        <table mat-table [dataSource]="rapport.matieres" class="data-table">
          <ng-container matColumnDef="matiere">
            <th mat-header-cell *matHeaderCellDef>Discipline</th>
            <td mat-cell *matCellDef="let ligne">{{ ligne.matiere }}</td>
          </ng-container>
          <ng-container matColumnDef="coefficient">
            <th mat-header-cell *matHeaderCellDef>Coef</th>
            <td mat-cell *matCellDef="let ligne">{{ ligne.coefficient }}</td>
          </ng-container>
          <ng-container matColumnDef="moyenne">
            <th mat-header-cell *matHeaderCellDef>Moyenne</th>
            <td mat-cell *matCellDef="let ligne" [class.cell-alert]="ligne.moyenne < 10">
              {{ ligne.moyenne ?? '—' }}
            </td>
          </ng-container>
          <ng-container matColumnDef="etendue">
            <th mat-header-cell *matHeaderCellDef>Max / min</th>
            <td mat-cell *matCellDef="let ligne">{{ ligne.note_max }} / {{ ligne.note_min }}</td>
          </ng-container>
          <ng-container matColumnDef="reussite">
            <th mat-header-cell *matHeaderCellDef>Réussite</th>
            <td mat-cell *matCellDef="let ligne">
              {{ ligne.atteignentLaMoyenne }}/{{ ligne.notes }} · {{ pourcent(ligne.tauxReussite) }}
            </td>
          </ng-container>
          <tr mat-header-row *matHeaderRowDef="subjectColumns"></tr>
          <tr mat-row *matRowDef="let row; columns: subjectColumns"></tr>
        </table>
      </div>

      <h2 class="section-label">Répartition des moyennes</h2>
      <div class="repartition">
        @for (tranche of rapport.repartition; track tranche.libelle) {
          <div class="tranche">
            <span class="tranche-label">{{ tranche.libelle }}</span>
            <span class="tranche-count">{{ tranche.effectif }}</span>
            <span class="tranche-part">{{ pourcent(tranche.part) }}</span>
          </div>
        }
      </div>
    } @else if (!loading() && !errorMessage()) {
      <p class="empty-state">
        Choisissez une classe et la période, puis calculez. Les bulletins de la période
        doivent avoir été générés au préalable.
      </p>
    }
  `,
  styles: `
    .page-intro {
      max-width: 60ch;
      margin-bottom: var(--space-3);
      color: var(--color-text-muted, #666);
    }

    .stat-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
      gap: var(--space-2);
      margin-bottom: var(--space-4);
    }

    .stat {
      display: flex;
      flex-direction: column;
      gap: 0.2rem;
      padding: var(--space-2);
      border: 1px solid var(--color-border);
      border-radius: var(--radius-sm);
    }

    .stat-label {
      color: var(--color-text-muted, #666);
      font-size: 0.75rem;
      text-transform: uppercase;
    }

    .stat-value {
      font-size: 1.25rem;
      font-weight: 600;
    }

    .repartition {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(90px, 1fr));
      gap: var(--space-1);
    }

    .tranche {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 0.15rem;
      padding: var(--space-2) var(--space-1);
      border: 1px solid var(--color-border);
      border-radius: var(--radius-sm);
    }

    .tranche-label {
      color: var(--color-text-muted, #666);
      font-size: 0.7rem;
    }

    .tranche-count {
      font-size: 1.2rem;
      font-weight: 600;
    }

    .tranche-part {
      color: var(--color-text-muted, #666);
      font-size: 0.7rem;
    }

    /* Une discipline sous la moyenne est ce sur quoi la tutelle demandera un plan. */
    .cell-alert {
      color: #b02a1f;
      font-weight: 600;
    }
  `,
})
export class TermReportPage {
  private readonly termReportService = inject(TermReportService);
  private readonly schoolClassService = inject(SchoolClassService);

  protected readonly classes = signal<SchoolClass[]>([]);
  protected readonly report = signal<TermReport | null>(null);
  protected readonly loading = signal(false);
  protected readonly exporting = signal(false);
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly subjectColumns = ['matiere', 'coefficient', 'moyenne', 'etendue', 'reussite'];
  protected schoolClassId: number | null = null;
  protected periodLabel = '1er trimestre';

  constructor() {
    void this.schoolClassService.list().then((classes) => this.classes.set(classes));
  }

  protected pourcent(valeur: number | null): string {
    return valeur === null ? '—' : `${valeur} %`;
  }

  async load(): Promise<void> {
    if (this.schoolClassId === null) {
      return;
    }
    this.loading.set(true);
    this.errorMessage.set(null);
    try {
      this.report.set(await this.termReportService.preview(this.schoolClassId, this.periodLabel));
    } catch (error) {
      // Remis à null : garder l'ancien rapport à l'écran après un échec ferait croire
      // qu'il correspond à la classe qu'on vient de choisir.
      this.report.set(null);
      this.errorMessage.set(extractErrorMessage(error));
    } finally {
      this.loading.set(false);
    }
  }

  async exportPdf(): Promise<void> {
    const rapport = this.report();
    if (this.schoolClassId === null || rapport === null) {
      return;
    }
    this.exporting.set(true);
    this.errorMessage.set(null);
    try {
      await this.termReportService.downloadPdf(
        this.schoolClassId,
        rapport.className,
        this.periodLabel,
      );
    } catch (error) {
      this.errorMessage.set(extractErrorMessage(error));
    } finally {
      this.exporting.set(false);
    }
  }
}
