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
import { formatMoney } from '../core/money.util';
import { Student } from '../student/student.model';
import { StudentService } from '../student/student.service';
import { CanteenInvoice, CanteenService, Menu } from './canteen.service';

@Component({
  selector: 'app-canteen-page',
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
      <h1><mat-icon class="page-icon" aria-hidden="true">restaurant</mat-icon>Cantine</h1>
    </div>
    <form class="stack-form" (submit)="saveMenu($event)">
      <mat-form-field appearance="outline">
        <mat-label>Date</mat-label>
        <input matInput type="date" [(ngModel)]="date" name="date" />
      </mat-form-field>
      <mat-form-field appearance="outline">
        <mat-label>Menu du jour</mat-label>
        <input matInput [(ngModel)]="mainDescription" name="mainDescription" />
      </mat-form-field>
      <button mat-flat-button type="submit">Enregistrer le menu</button>
    </form>
    <form class="stack-form" (submit)="reserve($event)">
      <mat-form-field appearance="outline">
        <mat-label>Élève</mat-label>
        <mat-select [(ngModel)]="studentId" name="studentId">
          @for (student of students(); track student.id) {
            <mat-option [value]="student.id"
              >{{ student.lastName }} {{ student.firstName }}</mat-option
            >
          }
        </mat-select>
      </mat-form-field>
      <button mat-stroked-button type="submit">Réserver un repas</button>
    </form>
    @if (errorMessage()) {
      <p class="flash-error">{{ errorMessage() }}</p>
    }
    <h2>Menus</h2>
    <div class="table-scroll">
      <table mat-table [dataSource]="menus()" class="data-table">
        <ng-container matColumnDef="date">
          <th mat-header-cell *matHeaderCellDef>Date</th>
          <td mat-cell *matCellDef="let menu">{{ menu.date }}</td>
        </ng-container>
        <ng-container matColumnDef="desc">
          <th mat-header-cell *matHeaderCellDef>Plat</th>
          <td mat-cell *matCellDef="let menu">{{ menu.mainDescription }}</td>
        </ng-container>
        <tr mat-header-row *matHeaderRowDef="menuColumns"></tr>
        <tr mat-row *matRowDef="let row; columns: menuColumns"></tr>
      </table>
    </div>
    <h2>Impayés</h2>
    <div class="table-scroll">
      <table mat-table [dataSource]="unpaid()" class="data-table">
        <ng-container matColumnDef="student">
          <th mat-header-cell *matHeaderCellDef>Élève</th>
          <td mat-cell *matCellDef="let invoice">#{{ invoice.studentId }}</td>
        </ng-container>
        <ng-container matColumnDef="amount">
          <th mat-header-cell *matHeaderCellDef>Dû</th>
          <td mat-cell *matCellDef="let invoice">
            {{ money(invoice.amountDueCents - invoice.amountPaidCents) }}
          </td>
        </ng-container>
        <tr mat-header-row *matHeaderRowDef="unpaidColumns"></tr>
        <tr mat-row *matRowDef="let row; columns: unpaidColumns"></tr>
      </table>
    </div>
  `,
})
export class CanteenPage {
  private readonly canteenService = inject(CanteenService);
  private readonly studentService = inject(StudentService);

  protected readonly students = signal<Student[]>([]);
  protected readonly menus = signal<Menu[]>([]);
  protected readonly unpaid = signal<CanteenInvoice[]>([]);
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly money = formatMoney;
  protected readonly menuColumns = ['date', 'desc'];
  protected readonly unpaidColumns = ['student', 'amount'];
  protected date = toIsoDate(new Date());
  protected mainDescription = '';
  protected studentId: number | null = null;

  constructor() {
    void this.studentService.list().then((students) => this.students.set(students));
    void this.refresh();
  }

  async refresh(): Promise<void> {
    const from = toIsoDate(new Date(Date.now() - 7 * 86400000));
    const to = toIsoDate(new Date(Date.now() + 7 * 86400000));
    this.menus.set(await this.canteenService.listMenus(from, to));
    try {
      this.unpaid.set(await this.canteenService.unpaid());
    } catch {
      this.unpaid.set([]);
    }
  }

  async saveMenu(event: Event): Promise<void> {
    event.preventDefault();
    try {
      await this.canteenService.upsertMenu(this.date, this.mainDescription, null);
      await this.refresh();
    } catch (error) {
      this.errorMessage.set(extractErrorMessage(error));
    }
  }

  async reserve(event: Event): Promise<void> {
    event.preventDefault();
    if (this.studentId === null) {
      return;
    }
    try {
      await this.canteenService.reserve(this.studentId, this.date, false);
      await this.refresh();
    } catch (error) {
      this.errorMessage.set(extractErrorMessage(error));
    }
  }
}
