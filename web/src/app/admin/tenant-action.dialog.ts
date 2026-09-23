import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { fieldError } from '../core/form-error.util';
import { extractErrorMessage } from '../core/http-error.util';
import {
  PlanAdmin,
  PlatformAdminService,
  TENANT_STATUSES,
  TenantAdmin,
  TenantStatus,
} from './platform-admin.service';

export type TenantActionKind = 'status' | 'trial' | 'plan';

export interface TenantActionDialogData {
  kind: TenantActionKind;
  tenant: TenantAdmin;
  plans: PlanAdmin[];
}

/**
 * Décisions prises sur un établissement client : suspension, prolongation d'essai, changement
 * de plan.
 *
 * <p>Les trois passent par le même écran parce qu'elles partagent l'essentiel : elles engagent
 * l'éditeur vis-à-vis d'un client, et demandent donc un motif qui sera conservé au journal.
 * Le motif est obligatoire dès que la décision restreint l'accès — le serveur le refuse
 * autrement, autant le dire avant l'aller-retour.
 */
@Component({
  selector: 'app-tenant-action-dialog',
  imports: [
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatProgressSpinnerModule,
  ],
  template: `
    <h2 mat-dialog-title>{{ title() }}</h2>
    <form [formGroup]="form" (ngSubmit)="submit()">
      <mat-dialog-content>
        <p class="action-context">
          <strong>{{ data.tenant.name }}</strong> — {{ data.tenant.subdomain }}
        </p>

        @if (data.kind === 'status') {
          <mat-form-field appearance="outline">
            <mat-label>Nouveau statut</mat-label>
            <mat-select formControlName="status">
              @for (status of statuses; track status.value) {
                <mat-option [value]="status.value">{{ status.label }}</mat-option>
              }
            </mat-select>
          </mat-form-field>
        }

        @if (data.kind === 'trial') {
          <mat-form-field appearance="outline">
            <mat-label>Jours supplémentaires</mat-label>
            <input matInput type="number" formControlName="days" />
            <mat-hint>Comptés à partir d'aujourd'hui si l'essai est déjà échu.</mat-hint>
            <mat-error>{{ fieldError(form.controls.days) }}</mat-error>
          </mat-form-field>
        }

        @if (data.kind === 'plan') {
          <mat-form-field appearance="outline">
            <mat-label>Nouveau plan</mat-label>
            <mat-select formControlName="planId">
              @for (plan of data.plans; track plan.id) {
                <mat-option [value]="plan.id">{{ plan.name }} ({{ plan.code }})</mat-option>
              }
            </mat-select>
            <mat-error>{{ fieldError(form.controls.planId) }}</mat-error>
          </mat-form-field>
        }

        <mat-form-field appearance="outline">
          <mat-label>Motif{{ reasonRequired() ? '' : ' (facultatif)' }}</mat-label>
          <input matInput formControlName="reason" />
          <mat-hint>Conservé au journal : c'est ce qu'on relit six mois plus tard.</mat-hint>
          <mat-error>{{ fieldError(form.controls.reason) }}</mat-error>
        </mat-form-field>

        @if (errorMessage()) {
          <p class="error-message">{{ errorMessage() }}</p>
        }
      </mat-dialog-content>

      <mat-dialog-actions align="end">
        <button mat-button type="button" (click)="cancel()">Annuler</button>
        <button mat-flat-button type="submit" [disabled]="submitting()">
          @if (submitting()) {
            <mat-spinner diameter="20" />
          } @else {
            Confirmer
          }
        </button>
      </mat-dialog-actions>
    </form>
  `,
  styles: `
    .action-context {
      margin: 0 0 var(--space-4);
      padding: var(--space-3) var(--space-4);
      border-radius: var(--radius);
      background: var(--color-surface);
      border: 1px solid var(--color-border);
    }
  `,
})
export class TenantActionDialog {
  protected readonly fieldError = fieldError;
  protected readonly statuses = TENANT_STATUSES;

  private readonly formBuilder = inject(FormBuilder);
  private readonly platformAdminService = inject(PlatformAdminService);
  private readonly dialogRef = inject(MatDialogRef<TenantActionDialog, boolean>);
  protected readonly data = inject<TenantActionDialogData>(MAT_DIALOG_DATA);

  protected readonly submitting = signal(false);
  protected readonly errorMessage = signal<string | null>(null);

  protected readonly form = this.formBuilder.nonNullable.group({
    status: [this.data.tenant.status as TenantStatus],
    days: [15, [Validators.min(1), Validators.max(180)]],
    planId: [null as number | null],
    reason: [''],
  });

  constructor() {
    // Le motif devient obligatoire dès que le statut choisi restreint l'accès. Recalculé à
    // chaque changement plutôt que fixé à l'ouverture : l'opérateur change d'avis dans l'écran.
    this.form.controls.status.valueChanges.subscribe(() => this.syncReasonValidator());
    this.syncReasonValidator();
  }

  protected title(): string {
    return {
      status: "Changer le statut de l'établissement",
      trial: "Prolonger l'essai",
      plan: 'Changer de plan',
    }[this.data.kind];
  }

  protected reasonRequired(): boolean {
    if (this.data.kind !== 'status') {
      return false;
    }
    const status = this.form.controls.status.value;
    return status === 'SUSPENDED' || status === 'CANCELLED' || status === 'READ_ONLY';
  }

  private syncReasonValidator(): void {
    const control = this.form.controls.reason;
    control.setValidators(this.reasonRequired() ? [Validators.required] : []);
    control.updateValueAndValidity({ emitEvent: false });
  }

  async submit(): Promise<void> {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.submitting.set(true);
    this.errorMessage.set(null);
    try {
      const value = this.form.getRawValue();
      const reason = value.reason.trim() || null;
      const id = this.data.tenant.id;
      if (this.data.kind === 'status') {
        await this.platformAdminService.updateStatus(id, value.status, reason);
      } else if (this.data.kind === 'trial') {
        await this.platformAdminService.extendTrial(id, value.days, reason);
      } else {
        if (value.planId === null) {
          this.errorMessage.set('Choisissez un plan.');
          return;
        }
        await this.platformAdminService.changePlan(id, value.planId, reason);
      }
      this.dialogRef.close(true);
    } catch (error) {
      this.errorMessage.set(extractErrorMessage(error));
    } finally {
      this.submitting.set(false);
    }
  }

  cancel(): void {
    this.dialogRef.close(false);
  }
}
