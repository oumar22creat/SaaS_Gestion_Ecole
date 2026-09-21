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
import { Student } from '../student/student.model';
import { StudentService } from '../student/student.service';
import { Book, BookLoan, LibraryService } from './library.service';

@Component({
  selector: 'app-library-page',
  imports: [
    FormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatTableModule,
    MatIconModule,
  ],
  template: `
    <div class="page-header">
      <h1><mat-icon class="page-icon" aria-hidden="true">local_library</mat-icon>Bibliothèque</h1>
    </div>
    <div class="filters-row">
      <mat-form-field appearance="outline">
        <mat-label>Recherche</mat-label>
        <input matInput [(ngModel)]="query" />
      </mat-form-field>
      <button mat-stroked-button (click)="search()"><mat-icon>search</mat-icon> Chercher</button>
    </div>
    <form class="stack-form" (submit)="create($event)">
      <mat-form-field appearance="outline"
        ><mat-label>Code-barres</mat-label><input matInput [(ngModel)]="barcode" name="barcode"
      /></mat-form-field>
      <mat-form-field appearance="outline"
        ><mat-label>Titre</mat-label><input matInput [(ngModel)]="title" name="title"
      /></mat-form-field>
      <mat-form-field appearance="outline"
        ><mat-label>Auteur</mat-label><input matInput [(ngModel)]="author" name="author"
      /></mat-form-field>
      <button mat-flat-button type="submit"><mat-icon>add</mat-icon> Ajouter un ouvrage</button>
    </form>
    @if (errorMessage()) {
      <p class="flash-error">{{ errorMessage() }}</p>
    }
    <div class="table-scroll">
      <table mat-table [dataSource]="books()" class="data-table">
        <ng-container matColumnDef="title"
          ><th mat-header-cell *matHeaderCellDef>Titre</th>
          <td mat-cell *matCellDef="let book">{{ book.title }}</td></ng-container
        >
        <ng-container matColumnDef="author"
          ><th mat-header-cell *matHeaderCellDef>Auteur</th>
          <td mat-cell *matCellDef="let book">{{ book.author }}</td></ng-container
        >
        <ng-container matColumnDef="copies"
          ><th mat-header-cell *matHeaderCellDef>Dispo</th>
          <td mat-cell *matCellDef="let book">
            {{ book.availableCopies }}/{{ book.totalCopies }}
          </td></ng-container
        >
        <ng-container matColumnDef="loan">
          <th mat-header-cell *matHeaderCellDef></th>
          <td mat-cell *matCellDef="let book">
            <button mat-button (click)="borrow(book)"><mat-icon>upload</mat-icon> Prêter</button>
          </td>
        </ng-container>
        <tr mat-header-row *matHeaderRowDef="columns"></tr>
        <tr mat-row *matRowDef="let row; columns: columns"></tr>
      </table>
    </div>
    <mat-form-field appearance="outline">
      <mat-label>Élève pour le prêt</mat-label>
      <mat-select [(ngModel)]="studentId">
        @for (student of students(); track student.id) {
          <mat-option [value]="student.id"
            >{{ student.lastName }} {{ student.firstName }}</mat-option
          >
        }
      </mat-select>
    </mat-form-field>
    <h2>Retards</h2>
    @for (loan of overdue(); track loan.id) {
      <p>
        Élève #{{ loan.studentId }} — rendu {{ loan.dueDate }}
        <button mat-button (click)="returnLoan(loan)"><mat-icon>undo</mat-icon> Retour</button>
      </p>
    }
  `,
})
export class LibraryPage {
  private readonly libraryService = inject(LibraryService);
  private readonly studentService = inject(StudentService);

  protected readonly books = signal<Book[]>([]);
  protected readonly overdue = signal<BookLoan[]>([]);
  protected readonly students = signal<Student[]>([]);
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly columns = ['title', 'author', 'copies', 'loan'];
  protected query = '';
  protected barcode = '';
  protected title = '';
  protected author = '';
  protected studentId: number | null = null;

  constructor() {
    void this.studentService.list().then((students) => this.students.set(students));
    void this.refreshOverdue();
  }

  async search(): Promise<void> {
    this.books.set(await this.libraryService.search(this.query || 'a'));
  }

  async create(event: Event): Promise<void> {
    event.preventDefault();
    try {
      await this.libraryService.createBook(this.barcode, this.title, this.author, 1);
      await this.search();
    } catch (error) {
      this.errorMessage.set(extractErrorMessage(error));
    }
  }

  async borrow(book: Book): Promise<void> {
    if (this.studentId === null) {
      return;
    }
    const due = toIsoDate(new Date(Date.now() + 14 * 86400000));
    try {
      await this.libraryService.borrow(book.id, this.studentId, due);
      await this.search();
      await this.refreshOverdue();
    } catch (error) {
      this.errorMessage.set(extractErrorMessage(error));
    }
  }

  async returnLoan(loan: BookLoan): Promise<void> {
    await this.libraryService.returnLoan(loan.id);
    await this.refreshOverdue();
  }

  private async refreshOverdue(): Promise<void> {
    this.overdue.set(await this.libraryService.overdue());
  }
}
