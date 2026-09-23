import { DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatTableModule } from '@angular/material/table';
import { fieldError } from '../core/form-error.util';
import { extractErrorMessage } from '../core/http-error.util';
import { PlatformAccount, PlatformAdminService } from './platform-admin.service';

/**
 * Comptes de la console plateforme.
 *
 * <p>Il n'existait aucun moyen d'en créer un second : le premier avait été inséré à la main en
 * base. Un éditeur à un seul compte perd l'accès à sa propre plateforme le jour où son
 * titulaire est indisponible.
 */
@Component({
  selector: 'app-platform-accounts-page',
  imports: [
    DatePipe,
    ReactiveFormsModule,
    MatTableModule,
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
  ],
  template: `
    <div class="page-header">
      <h1><mat-icon class="page-icon" aria-hidden="true">admin_panel_settings</mat-icon>Comptes plateforme</h1>
    </div>

    <p class="page-subtitle">
      Ces comptes voient tous les établissements clients. Ils n'ont pas le même profil de
      risque qu'un compte d'établissement : le mot de passe exigé est plus long.
    </p>

    @if (errorMessage()) {
      <p class="flash-error">{{ errorMessage() }}</p>
    }
    @if (success()) {
      <p class="flash-success">Compte créé. Communiquez le mot de passe hors ligne.</p>
    }

    <form class="stack-form" [formGroup]="form" (ngSubmit)="create()">
      <mat-form-field appearance="outline">
        <mat-label>Prénom</mat-label>
        <input matInput formControlName="firstName" />
        <mat-error>{{ fieldError(form.controls.firstName) }}</mat-error>
      </mat-form-field>
      <mat-form-field appearance="outline">
        <mat-label>Nom</mat-label>
        <input matInput formControlName="lastName" />
        <mat-error>{{ fieldError(form.controls.lastName) }}</mat-error>
      </mat-form-field>
      <mat-form-field appearance="outline">
        <mat-label>Adresse e-mail</mat-label>
        <input matInput type="email" formControlName="email" autocomplete="off" />
        <mat-error>{{ fieldError(form.controls.email) }}</mat-error>
      </mat-form-field>
      <mat-form-field appearance="outline">
        <mat-label>Mot de passe</mat-label>
        <input matInput type="text" formControlName="password" autocomplete="new-password" />
        <mat-hint>12 caractères minimum. Aucun message n'est envoyé.</mat-hint>
        <mat-error>{{ fieldError(form.controls.password) }}</mat-error>
      </mat-form-field>
      <button mat-flat-button type="submit" [disabled]="creating()">
        <mat-icon>person_add</mat-icon> Créer le compte
      </button>
    </form>

    <div class="table-scroll">
      <table mat-table [dataSource]="accounts()" class="data-table">
        <ng-container matColumnDef="name">
          <th mat-header-cell *matHeaderCellDef>Nom</th>
          <td mat-cell *matCellDef="let account">{{ account.firstName }} {{ account.lastName }}</td>
        </ng-container>
        <ng-container matColumnDef="email">
          <th mat-header-cell *matHeaderCellDef>Identifiant</th>
          <td mat-cell *matCellDef="let account">{{ account.email }}</td>
        </ng-container>
        <ng-container matColumnDef="createdAt">
          <th mat-header-cell *matHeaderCellDef>Créé le</th>
          <td mat-cell *matCellDef="let account">{{ account.createdAt | date: 'dd/MM/yyyy' }}</td>
        </ng-container>
        <tr mat-header-row *matHeaderRowDef="columns"></tr>
        <tr mat-row *matRowDef="let row; columns: columns"></tr>
      </table>
    </div>
  `,
})
export class PlatformAccountsPage {
  private readonly platformAdminService = inject(PlatformAdminService);
  private readonly formBuilder = inject(FormBuilder);

  protected readonly fieldError = fieldError;
  protected readonly accounts = signal<PlatformAccount[]>([]);
  protected readonly creating = signal(false);
  protected readonly success = signal(false);
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly columns = ['name', 'email', 'createdAt'];

  protected readonly form = this.formBuilder.nonNullable.group({
    firstName: ['', [Validators.required, Validators.maxLength(100)]],
    lastName: ['', [Validators.required, Validators.maxLength(100)]],
    email: ['', [Validators.required, Validators.email, Validators.maxLength(255)]],
    password: ['', [Validators.required, Validators.minLength(12), Validators.maxLength(100)]],
  });

  constructor() {
    void this.refresh();
  }

  async refresh(): Promise<void> {
    try {
      this.accounts.set(await this.platformAdminService.listAccounts());
    } catch (error) {
      this.accounts.set([]);
      this.errorMessage.set(extractErrorMessage(error));
    }
  }

  async create(): Promise<void> {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.creating.set(true);
    this.errorMessage.set(null);
    this.success.set(false);
    try {
      const value = this.form.getRawValue();
      await this.platformAdminService.createAccount(
        value.email,
        value.password,
        value.firstName,
        value.lastName,
      );
      this.form.reset({ firstName: '', lastName: '', email: '', password: '' });
      this.success.set(true);
      await this.refresh();
    } catch (error) {
      this.errorMessage.set(extractErrorMessage(error));
    } finally {
      this.creating.set(false);
    }
  }
}
