import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatTableModule } from '@angular/material/table';
import { MatIconModule } from '@angular/material/icon';
import { extractErrorMessage } from '../core/http-error.util';
import { SchoolClass } from '../schoolclass/school-class.model';
import { SchoolClassService } from '../schoolclass/school-class.service';
import { DocumentService, SchoolDocument } from './document.service';

@Component({
  selector: 'app-document-page',
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
      <h1><mat-icon class="page-icon" aria-hidden="true">folder_open</mat-icon>Documents</h1>
    </div>
    <div class="filters-row">
      <mat-form-field appearance="outline">
        <mat-label>Classe</mat-label>
        <mat-select [(ngModel)]="schoolClassId" (ngModelChange)="load()">
          @for (schoolClass of classes(); track schoolClass.id) {
            <mat-option [value]="schoolClass.id">{{ schoolClass.name }}</mat-option>
          }
        </mat-select>
      </mat-form-field>
    </div>
    <form class="stack-form" (submit)="upload($event)">
      <mat-form-field appearance="outline">
        <mat-label>Titre</mat-label>
        <input matInput [(ngModel)]="title" name="title" required />
      </mat-form-field>
      <input type="file" (change)="onFile($event)" />
      <button mat-flat-button type="submit"><mat-icon>upload</mat-icon> Déposer</button>
    </form>
    @if (errorMessage()) {
      <p class="flash-error">{{ errorMessage() }}</p>
    }
    <div class="table-scroll">
      <table mat-table [dataSource]="documents()" class="data-table">
        <ng-container matColumnDef="title">
          <th mat-header-cell *matHeaderCellDef>Titre</th>
          <td mat-cell *matCellDef="let doc">{{ doc.title }}</td>
        </ng-container>
        <ng-container matColumnDef="file">
          <th mat-header-cell *matHeaderCellDef>Fichier</th>
          <td mat-cell *matCellDef="let doc">{{ doc.fileName }}</td>
        </ng-container>
        <ng-container matColumnDef="actions">
          <th mat-header-cell *matHeaderCellDef></th>
          <td mat-cell *matCellDef="let doc">
            <button mat-button (click)="download(doc)"><mat-icon>download</mat-icon></button>
            <button mat-button (click)="archive(doc)">
              <mat-icon>inventory_2</mat-icon> Archiver
            </button>
          </td>
        </ng-container>
        <tr mat-header-row *matHeaderRowDef="columns"></tr>
        <tr mat-row *matRowDef="let row; columns: columns"></tr>
      </table>
    </div>
    @if (documents().length === 0) {
      <p class="empty-state">Aucun document pour cette classe.</p>
    }
  `,
})
export class DocumentPage {
  private readonly documentService = inject(DocumentService);
  private readonly schoolClassService = inject(SchoolClassService);

  protected readonly classes = signal<SchoolClass[]>([]);
  protected readonly documents = signal<SchoolDocument[]>([]);
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly columns = ['title', 'file', 'actions'];
  protected schoolClassId: number | null = null;
  protected title = '';
  private file: File | null = null;

  constructor() {
    void this.schoolClassService.list().then((classes) => this.classes.set(classes));
  }

  onFile(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.file = input.files?.[0] ?? null;
  }

  async load(): Promise<void> {
    if (this.schoolClassId === null) {
      return;
    }
    try {
      this.documents.set(await this.documentService.listByClass(this.schoolClassId));
    } catch (error) {
      this.errorMessage.set(extractErrorMessage(error));
    }
  }

  async upload(event: Event): Promise<void> {
    event.preventDefault();
    if (!this.file || this.schoolClassId === null) {
      return;
    }
    const form = new FormData();
    form.append('title', this.title);
    form.append('scope', 'CLASS');
    form.append('schoolClassId', String(this.schoolClassId));
    form.append('file', this.file);
    try {
      await this.documentService.upload(form);
      this.title = '';
      await this.load();
    } catch (error) {
      this.errorMessage.set(extractErrorMessage(error));
    }
  }

  async download(doc: SchoolDocument): Promise<void> {
    await this.documentService.download(doc.id, doc.fileName);
  }

  async archive(doc: SchoolDocument): Promise<void> {
    await this.documentService.archive(doc.id);
    await this.load();
  }
}
