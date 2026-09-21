import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../environments/environment';
import { ApiResponse } from '../core/api-response.model';

export interface Book {
  id: number;
  barcode: string;
  isbn: string | null;
  title: string;
  author: string;
  totalCopies: number;
  availableCopies: number;
}

export interface BookLoan {
  id: number;
  bookId: number;
  studentId: number;
  dueDate: string;
  overdue: boolean;
}

@Injectable({ providedIn: 'root' })
export class LibraryService {
  private readonly http = inject(HttpClient);

  async search(query: string): Promise<Book[]> {
    const response = await firstValueFrom(
      this.http.get<ApiResponse<Book[]>>(`${environment.apiUrl}/library/books`, {
        params: { query },
      }),
    );
    return response.data;
  }

  async createBook(
    barcode: string,
    title: string,
    author: string,
    totalCopies: number,
  ): Promise<Book> {
    const response = await firstValueFrom(
      this.http.post<ApiResponse<Book>>(`${environment.apiUrl}/library/books`, {
        barcode,
        isbn: null,
        title,
        author,
        totalCopies,
      }),
    );
    return response.data;
  }

  async borrow(bookId: number, studentId: number, dueDate: string): Promise<void> {
    await firstValueFrom(
      this.http.post(`${environment.apiUrl}/library/books/${bookId}/loans`, { studentId, dueDate }),
    );
  }

  async returnLoan(loanId: number): Promise<void> {
    await firstValueFrom(
      this.http.post(`${environment.apiUrl}/library/loans/${loanId}/return`, {}),
    );
  }

  async overdue(): Promise<BookLoan[]> {
    const response = await firstValueFrom(
      this.http.get<ApiResponse<BookLoan[]>>(`${environment.apiUrl}/library/loans/overdue`),
    );
    return response.data;
  }
}
