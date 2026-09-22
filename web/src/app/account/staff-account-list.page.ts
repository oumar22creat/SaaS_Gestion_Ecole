import { Component, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatTableModule } from '@angular/material/table';
import { confirmAction } from '../core/confirm-dialog.component';
import { extractErrorMessage } from '../core/http-error.util';
import { ResetPasswordDialog } from './reset-password.dialog';
import { StaffAccountFormDialog } from './staff-account-form.dialog';
import { ASSIGNABLE_ROLES, StaffAccount, isFamilyAccount, roleLabelFor } from './staff-account.model';
import { StaffAccountService } from './staff-account.service';

@Component({
  selector: 'app-staff-account-list-page',
  imports: [MatTableModule, MatButtonModule, MatIconModule, MatMenuModule, MatDialogModule],
  templateUrl: './staff-account-list.page.html',
})
export class StaffAccountListPage {
  private readonly accountService = inject(StaffAccountService);
  private readonly dialog = inject(MatDialog);

  protected readonly accounts = signal<StaffAccount[]>([]);
  protected readonly loading = signal(false);
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly columns = ['name', 'email', 'role', 'status', 'actions'];
  protected readonly roles = ASSIGNABLE_ROLES;
  protected readonly roleLabelFor = roleLabelFor;
  protected readonly isFamilyAccount = isFamilyAccount;

  constructor() {
    void this.refresh();
  }

  async refresh(): Promise<void> {
    this.loading.set(true);
    try {
      this.accounts.set(await this.accountService.list());
    } finally {
      this.loading.set(false);
    }
  }

  openCreateDialog(): void {
    this.dialog
      .open(StaffAccountFormDialog, { width: '480px' })
      .afterClosed()
      .subscribe((result) => result && void this.refresh());
  }

  async toggleActive(account: StaffAccount): Promise<void> {
    if (account.active) {
      const confirmed = await confirmAction(this.dialog, {
        title: 'Désactiver ce compte ?',
        message: `${account.firstName} ${account.lastName} ne pourra plus se connecter, ni sur le web ni sur mobile. Ses données sont conservées et le compte peut être réactivé.`,
      });
      if (!confirmed) {
        return;
      }
    }
    await this.run(() => this.accountService.setActive(account.id, !account.active));
  }

  async changeRole(account: StaffAccount, role: string): Promise<void> {
    if (role === account.role) {
      return;
    }
    const confirmed = await confirmAction(this.dialog, {
      title: 'Changer le rôle ?',
      message: `${account.firstName} ${account.lastName} deviendra « ${roleLabelFor(role)} ». Le changement prend effet à sa prochaine connexion.`,
    });
    if (!confirmed) {
      return;
    }
    await this.run(() => this.accountService.changeRole(account.id, role));
  }

  openResetPassword(account: StaffAccount): void {
    this.dialog
      .open(ResetPasswordDialog, { data: { account }, width: '460px' })
      .afterClosed()
      .subscribe((done) => done && void this.refresh());
  }

  /** Les refus du serveur (auto-ciblage, rôle interdit) doivent rester visibles à l'écran. */
  private async run(operation: () => Promise<unknown>): Promise<void> {
    this.errorMessage.set(null);
    try {
      await operation();
      await this.refresh();
    } catch (error) {
      this.errorMessage.set(extractErrorMessage(error));
    }
  }
}
