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
import { Subject } from '../subject/subject.model';
import { SubjectService } from '../subject/subject.service';
import { Lesson, LessonService } from './lesson.service';

@Component({
  selector: 'app-lesson-page',
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
      <h1><mat-icon class="page-icon" aria-hidden="true">history_edu</mat-icon>Cahier de textes</h1>
    </div>
    <div class="filters-row">
      <mat-form-field appearance="outline">
        <mat-label>Classe</mat-label>
        <mat-select [(ngModel)]="schoolClassId" (ngModelChange)="onClassChange()">
          @for (schoolClass of classes(); track schoolClass.id) {
            <mat-option [value]="schoolClass.id">{{ schoolClass.name }}</mat-option>
          }
        </mat-select>
      </mat-form-field>
    </div>
    @if (schoolClassId !== null && subjectOptions().length === 0) {
      <p class="empty-state" data-testid="no-subject-in-class">
        Vous n’enseignez aucune matière dans cette classe : le cahier de textes y est en lecture
        seule.
      </p>
    } @else {
      <form class="stack-form" (submit)="create($event)">
        <mat-form-field appearance="outline">
          <mat-label>Matière</mat-label>
          <mat-select [(ngModel)]="subjectId" name="subjectId">
            @for (subject of subjectOptions(); track subject.id) {
              <mat-option [value]="subject.id">{{ subject.name }}</mat-option>
            }
          </mat-select>
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Date de séance</mat-label>
          <input matInput type="date" [(ngModel)]="sessionDate" name="sessionDate" />
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Contenu</mat-label>
          <textarea matInput [(ngModel)]="content" name="content" rows="3"></textarea>
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Devoir</mat-label>
          <textarea matInput [(ngModel)]="homework" name="homework" rows="2"></textarea>
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>À rendre le</mat-label>
          <input matInput type="date" [(ngModel)]="homeworkDueDate" name="homeworkDueDate" />
        </mat-form-field>
        <button mat-flat-button type="submit"><mat-icon>add</mat-icon> Ajouter la séance</button>
      </form>
    }
    @if (errorMessage()) {
      <p class="flash-error">{{ errorMessage() }}</p>
    }
    <div class="table-scroll">
      <table mat-table [dataSource]="lessons()" class="data-table">
        <ng-container matColumnDef="date">
          <th mat-header-cell *matHeaderCellDef>Date</th>
          <td mat-cell *matCellDef="let lesson">{{ lesson.sessionDate }}</td>
        </ng-container>
        <ng-container matColumnDef="content">
          <th mat-header-cell *matHeaderCellDef>Cours</th>
          <td mat-cell *matCellDef="let lesson">{{ lesson.content }}</td>
        </ng-container>
        <ng-container matColumnDef="homework">
          <th mat-header-cell *matHeaderCellDef>Devoir</th>
          <td mat-cell *matCellDef="let lesson">{{ lesson.homework || '—' }}</td>
        </ng-container>
        <ng-container matColumnDef="actions">
          <th mat-header-cell *matHeaderCellDef></th>
          <td mat-cell *matCellDef="let lesson">
            @if (scope().teaches(lesson.schoolClassId, lesson.subjectId)) {
              <button mat-button (click)="remove(lesson)" aria-label="Supprimer la séance">
                <mat-icon>delete</mat-icon>
              </button>
            }
          </td>
        </ng-container>
        <tr mat-header-row *matHeaderRowDef="columns"></tr>
        <tr mat-row *matRowDef="let row; columns: columns"></tr>
      </table>
    </div>
  `,
})
export class LessonPage {
  private readonly lessonService = inject(LessonService);
  private readonly schoolClassService = inject(SchoolClassService);
  private readonly subjectService = inject(SubjectService);
  private readonly teachingScopeService = inject(TeachingScopeService);

  /** Périmètre d'affichage (ADR-030) ; fermé tant qu'il n'est pas chargé. */
  protected readonly scope = signal(new TeachingScope(null, true));

  protected readonly classes = signal<SchoolClass[]>([]);
  protected readonly subjects = signal<Subject[]>([]);
  protected readonly lessons = signal<Lesson[]>([]);
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly columns = ['date', 'content', 'homework', 'actions'];
  protected schoolClassId: number | null = null;
  protected subjectId: number | null = null;
  protected sessionDate = toIsoDate(new Date());
  protected content = '';
  protected homework = '';
  protected homeworkDueDate = '';

  constructor() {
    void this.schoolClassService.list().then((classes) => this.classes.set(classes));
    void this.subjectService.list().then((subjects) => this.subjects.set(subjects));
    void this.teachingScopeService.current().then((scope) => this.scope.set(scope));
  }

  /**
   * Écriture réservée au couple (classe, matière) enseigné (ADR-030) : un enseignant ne se
   * voit proposer que ses matières de la classe choisie. La lecture reste ouverte à toute
   * classe de son périmètre.
   */
  protected subjectOptions(): Subject[] {
    return this.scope().subjectsFor(this.schoolClassId, this.subjects());
  }

  async onClassChange(): Promise<void> {
    if (!this.subjectOptions().some((subject) => subject.id === this.subjectId)) {
      this.subjectId = null;
    }
    await this.load();
  }

  async load(): Promise<void> {
    if (this.schoolClassId === null) {
      return;
    }
    this.errorMessage.set(null);
    const from = toIsoDate(new Date(Date.now() - 30 * 86400000));
    const to = toIsoDate(new Date(Date.now() + 30 * 86400000));
    try {
      this.lessons.set(await this.lessonService.list(this.schoolClassId, from, to));
    } catch (error) {
      this.errorMessage.set(extractErrorMessage(error));
    }
  }

  async create(event: Event): Promise<void> {
    event.preventDefault();
    if (this.schoolClassId === null || this.subjectId === null) {
      return;
    }
    this.errorMessage.set(null);
    try {
      await this.lessonService.create({
        schoolClassId: this.schoolClassId,
        subjectId: this.subjectId,
        sessionDate: this.sessionDate,
        content: this.content,
        homework: this.homework || null,
        homeworkDueDate: this.homeworkDueDate || null,
        attachmentDocumentId: null,
      });
      this.content = '';
      this.homework = '';
      await this.load();
    } catch (error) {
      this.errorMessage.set(extractErrorMessage(error));
    }
  }

  async remove(lesson: Lesson): Promise<void> {
    this.errorMessage.set(null);
    try {
      await this.lessonService.remove(lesson.id);
      await this.load();
    } catch (error) {
      this.errorMessage.set(extractErrorMessage(error));
    }
  }
}
