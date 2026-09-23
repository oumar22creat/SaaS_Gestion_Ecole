import { DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatSelectModule } from '@angular/material/select';
import { MatTableModule } from '@angular/material/table';
import { ListSearchComponent } from '../core/list-search.component';
import { extractErrorMessage } from '../core/http-error.util';
import { formatMoney } from '../core/money.util';
import { FRENCH_PAGINATOR } from '../core/paginator-intl.provider';
import { TenantActionDialog, TenantActionKind } from './tenant-action.dialog';
import {
  PlanAdmin,
  PlatformAdminService,
  TENANT_STATUSES,
  TenantAdmin,
  TenantStatus,
  tenantStatusLabel,
} from './platform-admin.service';

/**
 * Les établissements clients de la plateforme.
 *
 * <p>C'est l'écran qui manquait : la console ne montrait que cinq chiffres agrégés, sans
 * jamais dire qui sont les clients ni permettre d'agir sur eux. Aucun effectif d'élèves n'y
 * figure — les données scolaires sont protégées par Row-Level Security et restent invisibles
 * à l'éditeur, ce qui est une garantie faite aux établissements, pas une lacune.
 */
@Component({
  selector: 'app-platform-tenants-page',
  imports: [
    DatePipe,
    FormsModule,
    MatTableModule,
    MatButtonModule,
    MatIconModule,
    MatMenuModule,
    MatFormFieldModule,
    MatSelectModule,
    MatPaginatorModule,
    MatDialogModule,
    ListSearchComponent,
  ],
  providers: [FRENCH_PAGINATOR],
  templateUrl: './platform-tenants.page.html',
  styles: `
    /* Les champs Material occupent 100 % de leur conteneur par défaut : dans une barre de
     * filtres, recherche et statut se retrouvaient empilés au lieu d'être côte à côte. */
    .status-filter {
      width: 220px;
      flex: 0 0 auto;
    }

    /* Une échéance passée n'est pas une information neutre : c'est une école coupée. */
    .cell-expired {
      display: block;
      color: var(--color-danger, #b3261e);
      font-size: var(--font-size-small);
      font-weight: var(--font-weight-semibold);
    }
  `,
})
export class PlatformTenantsPage {
  private readonly platformAdminService = inject(PlatformAdminService);
  private readonly dialog = inject(MatDialog);

  protected readonly money = formatMoney;
  protected readonly statusLabel = tenantStatusLabel;
  protected readonly statuses = TENANT_STATUSES;

  protected readonly tenants = signal<TenantAdmin[]>([]);
  protected readonly plans = signal<PlanAdmin[]>([]);
  protected readonly total = signal(0);
  protected readonly loading = signal(false);
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly search = signal('');
  protected readonly pageIndex = signal(0);
  protected readonly pageSize = signal(25);
  protected readonly columns = ['name', 'status', 'plan', 'dates', 'actions'];

  /** 'ALL' plutôt que null : Material n'affiche aucun libellé pour une valeur nulle. */
  protected statusFilter: TenantStatus | 'ALL' = 'ALL';

  constructor() {
    void this.refresh();
    void this.platformAdminService.listPlans().then((plans) => this.plans.set(plans));
  }

  async refresh(): Promise<void> {
    this.loading.set(true);
    this.errorMessage.set(null);
    try {
      const page = await this.platformAdminService.listTenants(
        { page: this.pageIndex(), pageSize: this.pageSize(), search: this.search() },
        this.statusFilter === 'ALL' ? null : this.statusFilter,
      );
      this.tenants.set(page.items);
      this.total.set(page.total);
    } catch (error) {
      // Une liste vide se lirait comme « aucun client », l'inverse du problème.
      this.tenants.set([]);
      this.errorMessage.set(extractErrorMessage(error));
    } finally {
      this.loading.set(false);
    }
  }

  protected onSearch(term: string): void {
    this.search.set(term);
    this.pageIndex.set(0);
    void this.refresh();
  }

  protected onFilter(): void {
    this.pageIndex.set(0);
    void this.refresh();
  }

  protected onPage(event: PageEvent): void {
    this.pageIndex.set(event.pageIndex);
    this.pageSize.set(event.pageSize);
    void this.refresh();
  }

  /** Signale un abonnement à surveiller : essai qui s'achève, ou paiement en échec. */
  protected needsAttention(tenant: TenantAdmin): boolean {
    if (tenant.paymentFailedAt) {
      return true;
    }
    if (tenant.status !== 'TRIAL' || !tenant.trialEndsAt) {
      return false;
    }
    const daysLeft = (new Date(tenant.trialEndsAt).getTime() - Date.now()) / 86_400_000;
    return daysLeft < 7;
  }

  /**
   * Échéance dépassée : l'établissement est (ou sera à la prochaine exécution du cycle
   * horaire) coupé. Signalé dans la liste pour que l'opérateur repère d'un coup d'œil qui
   * rappeler.
   */
  protected expired(tenant: TenantAdmin): boolean {
    return !!tenant.currentPeriodEnd && new Date(tenant.currentPeriodEnd).getTime() < Date.now();
  }

  openAction(tenant: TenantAdmin, kind: TenantActionKind): void {
    this.dialog
      .open(TenantActionDialog, { width: '480px', data: { kind, tenant, plans: this.plans() } })
      .afterClosed()
      .subscribe((done) => done && void this.refresh());
  }
}
