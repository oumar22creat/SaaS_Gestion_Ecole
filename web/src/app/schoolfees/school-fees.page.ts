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
import { SchoolClass } from '../schoolclass/school-class.model';
import { SchoolClassService } from '../schoolclass/school-class.service';
import { FeeInvoice, FeeReporting, FeeSchedule, SchoolFeesService } from './school-fees.service';

@Component({
  selector: 'app-school-fees-page',
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
      <h1><mat-icon class="page-icon" aria-hidden="true">payments</mat-icon>Frais scolaires</h1>
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
    <form class="stack-form" (submit)="create($event)">
      <mat-form-field appearance="outline">
        <mat-label>Intitulé</mat-label>
        <input matInput [(ngModel)]="label" name="label" />
      </mat-form-field>
      <mat-form-field appearance="outline">
        <mat-label>Montant (XOF)</mat-label>
        <input matInput type="number" [(ngModel)]="amountCents" name="amountCents" />
      </mat-form-field>
      <mat-form-field appearance="outline">
        <mat-label>Échéance</mat-label>
        <input matInput type="date" [(ngModel)]="dueDate" name="dueDate" />
      </mat-form-field>
      <button mat-flat-button type="submit"><mat-icon>add</mat-icon> Ajouter une échéance</button>
    </form>
    @if (errorMessage()) {
      <p class="flash-error">{{ errorMessage() }}</p>
    }
    @if (reporting()) {
      <p>
        Dû {{ money(reporting()!.totalDueCents) }} · Payé {{ money(reporting()!.totalPaidCents) }} ·
        Impayé {{ money(reporting()!.totalOutstandingCents) }}
      </p>
    }
    <div class="table-scroll">
      <table mat-table [dataSource]="schedules()" class="data-table">
        <ng-container matColumnDef="label">
          <th mat-header-cell *matHeaderCellDef>Échéance</th>
          <td mat-cell *matCellDef="let schedule">{{ schedule.label }}</td>
        </ng-container>
        <ng-container matColumnDef="amount">
          <th mat-header-cell *matHeaderCellDef>Montant</th>
          <td mat-cell *matCellDef="let schedule">{{ money(schedule.amountCents) }}</td>
        </ng-container>
        <ng-container matColumnDef="due">
          <th mat-header-cell *matHeaderCellDef>Date</th>
          <td mat-cell *matCellDef="let schedule">{{ schedule.dueDate }}</td>
        </ng-container>
        <ng-container matColumnDef="actions">
          <th mat-header-cell *matHeaderCellDef></th>
          <td mat-cell *matCellDef="let schedule">
            <button mat-button (click)="generate(schedule)">
              <mat-icon>receipt_long</mat-icon> Facturer
            </button>
          </td>
        </ng-container>
        <tr mat-header-row *matHeaderRowDef="columns"></tr>
        <tr mat-row *matRowDef="let row; columns: columns"></tr>
      </table>
    </div>
    <h2>Impayés</h2>
    <div class="table-scroll">
      <table mat-table [dataSource]="unpaid()" class="data-table">
        <ng-container matColumnDef="student">
          <th mat-header-cell *matHeaderCellDef>Élève</th>
          <td mat-cell *matCellDef="let invoice">#{{ invoice.studentId }}</td>
        </ng-container>
        <ng-container matColumnDef="dueAmount">
          <th mat-header-cell *matHeaderCellDef>Reste</th>
          <td mat-cell *matCellDef="let invoice">
            {{ money(invoice.amountDueCents - invoice.amountPaidCents) }}
          </td>
        </ng-container>
        <ng-container matColumnDef="pay">
          <th mat-header-cell *matHeaderCellDef></th>
          <td mat-cell *matCellDef="let invoice">
            <button mat-button (click)="pay(invoice)">
              <mat-icon>payments</mat-icon> Encaisser le solde
            </button>
          </td>
        </ng-container>
        <tr mat-header-row *matHeaderRowDef="unpaidColumns"></tr>
        <tr mat-row *matRowDef="let row; columns: unpaidColumns"></tr>
      </table>
    </div>
  `,
})
export class SchoolFeesPage {
  private readonly schoolFeesService = inject(SchoolFeesService);
  private readonly schoolClassService = inject(SchoolClassService);

  protected readonly classes = signal<SchoolClass[]>([]);
  protected readonly schedules = signal<FeeSchedule[]>([]);
  protected readonly reporting = signal<FeeReporting | null>(null);
  protected readonly unpaid = signal<FeeInvoice[]>([]);
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly columns = ['label', 'amount', 'due', 'actions'];
  protected readonly unpaidColumns = ['student', 'dueAmount', 'pay'];
  protected readonly money = formatMoney;
  protected schoolClassId: number | null = null;
  protected label = 'Scolarité trimestre 1';
  protected amountCents = 50000;
  protected dueDate = toIsoDate(new Date());

  constructor() {
    void this.schoolClassService.list().then((classes) => this.classes.set(classes));
  }

  async load(): Promise<void> {
    if (this.schoolClassId === null) {
      return;
    }
    const from = toIsoDate(new Date(new Date().getFullYear(), 0, 1));
    const to = toIsoDate(new Date());
    this.schedules.set(await this.schoolFeesService.listSchedules(this.schoolClassId));
    const reporting = await this.schoolFeesService.reporting(this.schoolClassId, from, to);
    this.reporting.set(reporting);
    this.unpaid.set(reporting.unpaidInvoices);
  }

  async create(event: Event): Promise<void> {
    event.preventDefault();
    if (this.schoolClassId === null) {
      return;
    }
    try {
      await this.schoolFeesService.createSchedule(
        this.schoolClassId,
        this.label,
        this.amountCents,
        this.dueDate,
      );
      await this.load();
    } catch (error) {
      this.errorMessage.set(extractErrorMessage(error));
    }
  }

  async generate(schedule: FeeSchedule): Promise<void> {
    await this.schoolFeesService.generateInvoices(schedule.id);
    await this.load();
  }

  async pay(invoice: FeeInvoice): Promise<void> {
    const remaining = invoice.amountDueCents - invoice.amountPaidCents;
    if (remaining <= 0) {
      return;
    }
    await this.schoolFeesService.recordPayment(invoice.id, remaining, 'CASH');
    await this.load();
  }
}
