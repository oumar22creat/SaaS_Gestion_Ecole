import { DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatTableModule } from '@angular/material/table';
import { extractErrorMessage } from '../core/http-error.util';
import { FRENCH_PAGINATOR } from '../core/paginator-intl.provider';
import { PlatformAction, PlatformAdminService } from './platform-admin.service';

const ACTION_LABELS: Record<string, string> = {
  TENANT_STATUS_CHANGED: "Changement de statut d'un établissement",
  TRIAL_EXTENDED: "Prolongation d'essai",
  PLAN_CHANGED: 'Changement de plan',
  PLATFORM_ADMIN_CREATED: 'Création d’un compte plateforme',
};

/**
 * Journal des décisions prises depuis la console.
 *
 * <p>Suspendre une école, prolonger un essai ou changer un plan engage l'éditeur vis-à-vis
 * d'un client. Sans cet écran, ces gestes restaient invisibles : personne ne pouvait répondre
 * six mois plus tard à « qui a coupé cette école, et pourquoi ».
 */
@Component({
  selector: 'app-platform-journal-page',
  imports: [DatePipe, MatTableModule, MatIconModule, MatPaginatorModule],
  providers: [FRENCH_PAGINATOR],
  template: `
    <div class="page-header">
      <h1><mat-icon class="page-icon" aria-hidden="true">history</mat-icon>Journal</h1>
    </div>

    <p class="page-subtitle">
      Toutes les décisions prises depuis cette console, la plus récente d'abord.
    </p>

    @if (errorMessage()) {
      <p class="flash-error">{{ errorMessage() }}</p>
    }

    @if (actions().length === 0) {
      <div class="empty-state">
        <span class="empty-title">Aucune décision enregistrée</span>
        <span class="empty-hint">
          Les suspensions, prolongations d'essai et changements de plan apparaîtront ici avec
          leur auteur et leur motif.
        </span>
      </div>
    } @else {
      <div class="table-scroll">
        <table mat-table [dataSource]="actions()" class="data-table">
          <ng-container matColumnDef="createdAt">
            <th mat-header-cell *matHeaderCellDef>Date</th>
            <td mat-cell *matCellDef="let action">
              {{ action.createdAt | date: 'dd/MM/yyyy HH:mm' }}
            </td>
          </ng-container>
          <ng-container matColumnDef="action">
            <th mat-header-cell *matHeaderCellDef>Décision</th>
            <td mat-cell *matCellDef="let action">
              <span class="cell-strong">{{ label(action.action) }}</span>
              @if (action.detail) {
                <span class="cell-muted">{{ action.detail }}</span>
              }
            </td>
          </ng-container>
          <ng-container matColumnDef="tenant">
            <th mat-header-cell *matHeaderCellDef>Établissement</th>
            <td mat-cell *matCellDef="let action">
              @if (action.tenantName) {
                <span class="cell-strong">{{ action.tenantName }}</span>
              } @else if (action.tenantId) {
                <span class="cell-muted">Établissement supprimé (#{{ action.tenantId }})</span>
              } @else {
                <span class="cell-muted">—</span>
              }
            </td>
          </ng-container>
          <ng-container matColumnDef="reason">
            <th mat-header-cell *matHeaderCellDef>Motif</th>
            <td mat-cell *matCellDef="let action">{{ action.reason || '—' }}</td>
          </ng-container>
          <tr mat-header-row *matHeaderRowDef="columns"></tr>
          <tr mat-row *matRowDef="let row; columns: columns"></tr>
        </table>
      </div>

      <mat-paginator
        [length]="total()"
        [pageIndex]="pageIndex()"
        [pageSize]="pageSize()"
        [pageSizeOptions]="[25, 50, 100]"
        (page)="onPage($event)"
        aria-label="Pages du journal"
      />
    }
  `,
})
export class PlatformJournalPage {
  private readonly platformAdminService = inject(PlatformAdminService);

  protected readonly actions = signal<PlatformAction[]>([]);
  protected readonly total = signal(0);
  protected readonly pageIndex = signal(0);
  protected readonly pageSize = signal(25);
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly columns = ['createdAt', 'action', 'tenant', 'reason'];

  constructor() {
    void this.refresh();
  }

  protected label(action: string): string {
    return ACTION_LABELS[action] ?? action;
  }

  protected onPage(event: PageEvent): void {
    this.pageIndex.set(event.pageIndex);
    this.pageSize.set(event.pageSize);
    void this.refresh();
  }

  async refresh(): Promise<void> {
    try {
      const page = await this.platformAdminService.listActions({
        page: this.pageIndex(),
        pageSize: this.pageSize(),
      });
      this.actions.set(page.items);
      this.total.set(page.total);
    } catch (error) {
      this.actions.set([]);
      this.errorMessage.set(extractErrorMessage(error));
    }
  }
}
