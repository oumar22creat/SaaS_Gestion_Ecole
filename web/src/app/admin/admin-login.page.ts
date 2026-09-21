import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatIconModule } from '@angular/material/icon';
import { extractErrorMessage } from '../core/http-error.util';
import { AuthTokenService } from '../auth/auth-token.service';
import { AuthService } from '../auth/auth.service';
import { fieldError } from '../core/form-error.util';

@Component({
  selector: 'app-admin-login-page',
  imports: [
    ReactiveFormsModule,
    RouterLink,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
  ],
  styleUrl: '../auth/login.page.scss',
  template: `
    <div class="auth-page">
      <aside class="auth-brand">
        <p class="auth-brand-icon" aria-hidden="true"><mat-icon>shield</mat-icon></p>
        <h2 class="auth-brand-title">Console de la plateforme</h2>
        <p class="auth-brand-lede">
          Suivi des établissements abonnés, des utilisateurs et du revenu récurrent. Accès réservé à
          l'équipe de la plateforme.
        </p>
        <ul class="auth-brand-list">
          <li>Établissements et abonnements</li>
          <li>MRR et comptes actifs</li>
        </ul>
      </aside>

      <section class="auth-form-panel">
        <mat-card class="auth-card">
          <p class="auth-eyebrow">Super-Admin</p>
          <h1>Console plateforme</h1>
          <p class="lede">Accès équipe plateforme uniquement.</p>
          <form [formGroup]="form" (ngSubmit)="submit()">
            <mat-form-field appearance="outline">
              <mat-label>E-mail</mat-label>
              <input matInput formControlName="email" autocomplete="username" />
              <mat-error>{{ fieldError(form.controls.email) }}</mat-error>
            </mat-form-field>
            <mat-form-field appearance="outline">
              <mat-label>Mot de passe</mat-label>
              <input
                matInput
                type="password"
                formControlName="password"
                autocomplete="current-password"
              />
              <mat-error>{{ fieldError(form.controls.password) }}</mat-error>
            </mat-form-field>
            @if (errorMessage()) {
              <p class="error-message">{{ errorMessage() }}</p>
            }
            <button mat-flat-button type="submit"><mat-icon>login</mat-icon> Entrer</button>
          </form>
          <p class="alt-links">
            <a routerLink="/login"><mat-icon>school</mat-icon> Retour espace établissement</a>
          </p>
        </mat-card>
      </section>
    </div>
  `,
})
export class AdminLoginPage {
  protected readonly fieldError = fieldError;
  private readonly formBuilder = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly authTokenService = inject(AuthTokenService);
  private readonly router = inject(Router);
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly form = this.formBuilder.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', Validators.required],
  });

  async submit(): Promise<void> {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    try {
      const { email, password } = this.form.getRawValue();
      this.authTokenService.store(await this.authService.adminLogin(email, password));
      await this.router.navigateByUrl('/admin');
    } catch (error) {
      this.errorMessage.set(extractErrorMessage(error, 'Identifiants invalides.'));
    }
  }
}
