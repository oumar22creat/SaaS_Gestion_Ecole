import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatTableModule } from '@angular/material/table';
import { MatIconModule } from '@angular/material/icon';
import { extractErrorMessage } from '../core/http-error.util';
import { toIsoDate } from '../core/iso-date.util';
import { TeachingScope, TeachingScopeService } from '../core/teaching-scope.service';
import { SchoolClass } from '../schoolclass/school-class.model';
import { SchoolClassService } from '../schoolclass/school-class.service';
import { Student } from '../student/student.model';
import { StudentService } from '../student/student.service';
import { ReportCard, ReportCardService } from './report-card.service';

@Component({
  selector: 'app-report-card-page',
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
      <h1><mat-icon class="page-icon" aria-hidden="true">description</mat-icon>Bulletins</h1>
    </div>
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
        <input matInput [(ngModel)]="periodLabel" placeholder="Trimestre 1" />
      </mat-form-field>
      <mat-form-field appearance="outline">
        <mat-label>Du</mat-label>
        <input matInput type="date" [(ngModel)]="periodFrom" />
      </mat-form-field>
      <mat-form-field appearance="outline">
        <mat-label>Au</mat-label>
        <input matInput type="date" [(ngModel)]="periodTo" />
      </mat-form-field>
      <button mat-stroked-button (click)="load()"><mat-icon>search</mat-icon> Afficher</button>
      @if (canManage()) {
        <button mat-flat-button (click)="generate()" data-testid="generate">
          <mat-icon>auto_awesome</mat-icon> {{ cards().length > 0 ? 'Régénérer' : 'Générer' }}
        </button>
      }
      <button
        mat-stroked-button
        [disabled]="cards().length === 0 || printingAll()"
        (click)="printAll()"
      >
        <mat-icon>print</mat-icon>
        {{ printingAll() ? 'Préparation…' : 'Imprimer toute la classe' }}
      </button>
    </div>
    @if (errorMessage()) {
      <p class="flash-error">{{ errorMessage() }}</p>
    }
    @if (successMessage()) {
      <p class="flash-success">{{ successMessage() }}</p>
    }
    <div class="table-scroll">
      <table mat-table [dataSource]="cards()" class="data-table">
        <ng-container matColumnDef="student">
          <th mat-header-cell *matHeaderCellDef>Élève</th>
          <td mat-cell *matCellDef="let card">{{ studentLabel(card.studentId) }}</td>
        </ng-container>
        <ng-container matColumnDef="rank">
          <th mat-header-cell *matHeaderCellDef>Rang</th>
          <td mat-cell *matCellDef="let card">
            @if (card.rankInClass) {
              <span class="cell-strong">{{ rankLabel(card.rankInClass) }}</span>
              <span class="cell-muted">sur {{ card.classSize }}</span>
            } @else {
              <span class="cell-muted">non classé</span>
            }
          </td>
        </ng-container>
        <ng-container matColumnDef="average">
          <th mat-header-cell *matHeaderCellDef>Moyenne</th>
          <td mat-cell *matCellDef="let card">{{ card.generalAverage ?? '—' }}</td>
        </ng-container>
        <ng-container matColumnDef="absences">
          <th mat-header-cell *matHeaderCellDef>Absences / retards</th>
          <td mat-cell *matCellDef="let card">{{ card.absenceCount }} / {{ card.lateCount }}</td>
        </ng-container>
        <ng-container matColumnDef="comment">
          <th mat-header-cell *matHeaderCellDef>Appréciation</th>
          <td mat-cell *matCellDef="let card">
            @if (canManage()) {
              <input [(ngModel)]="card.generalComment" aria-label="Appréciation générale" />
            } @else {
              <span class="cell-muted">{{ card.generalComment || '—' }}</span>
            }
          </td>
        </ng-container>
        <ng-container matColumnDef="actions">
          <th mat-header-cell *matHeaderCellDef></th>
          <td mat-cell *matCellDef="let card">
            @if (canManage()) {
              <button mat-button (click)="save(card)" data-testid="save-comment">
                <mat-icon>save</mat-icon> Enregistrer
              </button>
            }
            <button mat-button (click)="pdf(card)"><mat-icon>download</mat-icon> PDF</button>
          </td>
        </ng-container>
        <tr mat-header-row *matHeaderRowDef="columns"></tr>
        <tr mat-row *matRowDef="let row; columns: columns"></tr>
      </table>
    </div>
    @if (cards().length === 0) {
      <p class="empty-state">Aucun bulletin pour cette période.</p>
    }
    @if (schoolClassId !== null && !canManage()) {
      <p class="cell-muted" data-testid="head-teacher-only">
        La génération des bulletins, l’appréciation générale et la décision du conseil
        reviennent au professeur principal de la classe et à la direction.
      </p>
    }
  `,
})
export class ReportCardPage {
  private readonly reportCardService = inject(ReportCardService);
  private readonly schoolClassService = inject(SchoolClassService);
  private readonly studentService = inject(StudentService);
  private readonly teachingScopeService = inject(TeachingScopeService);

  /** Périmètre d'affichage (ADR-030) ; fermé tant qu'il n'est pas chargé. */
  protected readonly scope = signal(new TeachingScope(null, true));

  protected readonly classes = signal<SchoolClass[]>([]);
  protected readonly students = signal<Student[]>([]);
  protected readonly cards = signal<ReportCard[]>([]);
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly successMessage = signal<string | null>(null);
  /** Une liasse de cent bulletins met quelques secondes : le bouton doit le montrer. */
  protected readonly printingAll = signal(false);
  protected readonly columns = ['student', 'average', 'rank', 'absences', 'comment', 'actions'];
  protected schoolClassId: number | null = null;
  protected periodLabel = 'Trimestre 1';
  protected periodFrom = toIsoDate(new Date(new Date().getFullYear(), 8, 1));
  protected periodTo = toIsoDate(new Date());

  constructor() {
    void this.schoolClassService.list().then((classes) => this.classes.set(classes));
    void this.studentService.list().then((students) => this.students.set(students));
    void this.teachingScopeService.current().then((scope) => this.scope.set(scope));
  }

  /**
   * Génération, appréciation générale et décision du conseil : ADMIN, DIRECTION et
   * professeur principal de la classe choisie (ADR-030). Le serveur reste juge (403).
   */
  protected canManage(): boolean {
    return this.scope().canManageReportCards(this.schoolClassId);
  }

  studentLabel(id: number): string {
    const student = this.students().find((item) => item.id === id);
    return student ? `${student.lastName} ${student.firstName}` : `#${id}`;
  }

  async load(): Promise<void> {
    if (this.schoolClassId === null) {
      return;
    }
    this.errorMessage.set(null);
    try {
      this.cards.set(await this.reportCardService.list(this.schoolClassId, this.periodLabel));
    } catch (error) {
      this.errorMessage.set(extractErrorMessage(error));
    }
  }

  async generate(): Promise<void> {
    if (this.schoolClassId === null) {
      return;
    }
    this.errorMessage.set(null);
    this.successMessage.set(null);
    try {
      this.cards.set(
        await this.reportCardService.generate({
          schoolClassId: this.schoolClassId,
          periodLabel: this.periodLabel,
          periodFrom: this.periodFrom,
          periodTo: this.periodTo,
        }),
      );
      this.successMessage.set('Bulletins générés.');
    } catch (error) {
      this.errorMessage.set(extractErrorMessage(error));
    }
  }

  async save(card: ReportCard): Promise<void> {
    this.errorMessage.set(null);
    this.successMessage.set(null);
    try {
      await this.reportCardService.update(
        card.id,
        card.generalComment ?? '',
        card.councilDecision ?? '',
      );
      this.successMessage.set('Appréciation enregistrée.');
    } catch (error) {
      this.errorMessage.set(extractErrorMessage(error));
    }
  }

  /** « 1er » et non « 1e » : c'est la forme attendue sur un bulletin. */
  protected rankLabel(rank: number): string {
    return rank === 1 ? '1er' : `${rank}e`;
  }

  async pdf(card: ReportCard): Promise<void> {
    this.errorMessage.set(null);
    try {
      await this.reportCardService.downloadPdf(card.id);
    } catch (error) {
      this.errorMessage.set(extractErrorMessage(error));
    }
  }

  /** Un seul PDF pour la classe entière, un bulletin par page, en ordre alphabétique. */
  async printAll(): Promise<void> {
    if (this.schoolClassId === null) {
      return;
    }
    this.errorMessage.set(null);
    this.successMessage.set(null);
    this.printingAll.set(true);
    try {
      await this.reportCardService.downloadClassPdf(this.schoolClassId, this.periodLabel);
      this.successMessage.set(`${this.cards().length} bulletins téléchargés en un fichier.`);
    } catch (error) {
      this.errorMessage.set(extractErrorMessage(error));
    } finally {
      this.printingAll.set(false);
    }
  }
}
