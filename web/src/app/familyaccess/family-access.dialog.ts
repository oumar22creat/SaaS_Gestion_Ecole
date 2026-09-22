import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { fieldError } from '../core/form-error.util';
import { extractErrorMessage } from '../core/http-error.util';
import { FamilyAccessService, FamilyAccessTarget } from './family-access.service';

export interface FamilyAccessDialogData {
  target: FamilyAccessTarget;
  recordId: number;
  /** Nom affiché de la personne concernée, repris de sa fiche. */
  personName: string;
  /** E-mail déjà connu sur la fiche, proposé par défaut. */
  suggestedEmail: string | null;
}

/**
 * Ouverture d'un accès au portail mobile.
 *
 * <p>Seuls l'e-mail et un mot de passe provisoire sont demandés : le nom est repris de la
 * fiche, sinon la même personne finirait avec deux orthographes selon l'écran où on la
 * regarde.
 *
 * <p>Aucun e-mail n'est envoyé — le mot de passe est remis à la famille par l'établissement.
 * L'écran le dit, sans quoi le secrétariat attend une notification qui n'arrivera jamais.
 */
@Component({
  selector: 'app-family-access-dialog',
  imports: [
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
  ],
  template: `
    <h2 mat-dialog-title>Ouvrir l'accès mobile</h2>
    <form [formGroup]="form" (ngSubmit)="submit()">
      <mat-dialog-content>
        <p class="access-context">
          <strong>{{ data.personName }}</strong> pourra consulter
          @if (data.target === 'student') {
            ses notes, ses absences, son emploi du temps et ses frais de scolarité
          } @else {
            les notes, absences, emploi du temps et frais de ses enfants
          }
          depuis l'application mobile.
        </p>

        <mat-form-field appearance="outline">
          <mat-label>Adresse e-mail</mat-label>
          <input matInput type="email" formControlName="email" autocomplete="off" />
          <mat-hint>Sert d'identifiant de connexion, avec le sous-domaine de l'établissement.</mat-hint>
          <mat-error>{{ fieldError(form.controls.email) }}</mat-error>
        </mat-form-field>

        <mat-form-field appearance="outline">
          <mat-label>Mot de passe provisoire</mat-label>
          <input matInput type="text" formControlName="password" autocomplete="new-password" />
          <mat-hint>8 caractères minimum. À remettre à la famille.</mat-hint>
          <mat-error>{{ fieldError(form.controls.password) }}</mat-error>
        </mat-form-field>

        <p class="access-note">
          <mat-icon aria-hidden="true">info</mat-icon>
          Aucun message n'est envoyé : notez ce mot de passe et communiquez-le vous-même.
        </p>

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
            Ouvrir l'accès
          }
        </button>
      </mat-dialog-actions>
    </form>
  `,
  styles: `
    .access-context {
      margin: 0 0 var(--space-4);
      line-height: 1.6;
    }

    /* Le mot de passe est en clair et ne sera plus jamais affiché : l'avertissement doit se
     * voir avant de valider, pas après. */
    .access-note {
      display: flex;
      align-items: flex-start;
      gap: var(--space-2);
      margin: var(--space-2) 0 0;
      color: var(--color-text-secondary);
      font-size: var(--font-size-small);
      line-height: 1.5;
    }

    .access-note mat-icon {
      flex: 0 0 auto;
      font-size: 18px;
      width: 18px;
      height: 18px;
    }
  `,
})
export class FamilyAccessDialog {
  protected readonly fieldError = fieldError;

  private readonly formBuilder = inject(FormBuilder);
  private readonly familyAccessService = inject(FamilyAccessService);
  private readonly dialogRef = inject(MatDialogRef<FamilyAccessDialog, boolean>);
  protected readonly data = inject<FamilyAccessDialogData>(MAT_DIALOG_DATA);

  protected readonly submitting = signal(false);
  protected readonly errorMessage = signal<string | null>(null);

  protected readonly form = this.formBuilder.nonNullable.group({
    email: [
      this.data.suggestedEmail ?? '',
      [Validators.required, Validators.email, Validators.maxLength(255)],
    ],
    password: ['', [Validators.required, Validators.minLength(8), Validators.maxLength(100)]],
  });

  async submit(): Promise<void> {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.submitting.set(true);
    this.errorMessage.set(null);
    try {
      const value = this.form.getRawValue();
      await this.familyAccessService.openAccess(
        this.data.target,
        this.data.recordId,
        value.email,
        value.password,
      );
      this.dialogRef.close(true);
    } catch (error) {
      this.errorMessage.set(extractErrorMessage(error));
    } finally {
      this.submitting.set(false);
    }
  }

  cancel(): void {
    this.dialogRef.close();
  }
}
