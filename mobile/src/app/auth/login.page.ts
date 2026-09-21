import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { IonButton, IonContent, IonIcon, IonInput, IonItem, IonSpinner } from '@ionic/angular';
import { extractErrorMessage } from '../core/http-error.util';
import { AuthTokenService } from './auth-token.service';
import { AuthService } from './auth.service';

/** Identique en logique à web/src/app/auth/login.page.ts — composants Ionic natifs (docs/DESIGN.md §3). */
@Component({
  selector: 'app-login-page',
  imports: [ReactiveFormsModule, IonContent, IonItem, IonInput, IonButton, IonSpinner, IonIcon],
  templateUrl: './login.page.html',
  styles: `
    .login-hero {
      margin: var(--space-4) 0 var(--space-6);
      text-align: center;
    }
    .login-hero-icon {
      display: grid;
      place-items: center;
      width: 64px;
      height: 64px;
      margin: 0 auto var(--space-4);
      border-radius: var(--radius-lg);
      background: var(--tenant-primary-soft);
      box-shadow: inset 0 0 0 1px var(--tenant-primary-ring);
      font-size: 32px;
      line-height: 1;
    }
    .login-hero-title {
      margin: 0 0 var(--space-1);
      font-size: var(--font-size-display);
      font-weight: 700;
    }
    .login-hero-lede {
      margin: 0;
      color: var(--color-text-secondary);
      font-size: var(--font-size-small);
    }
  `,
})
export class LoginPage {
  private readonly formBuilder = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly authTokenService = inject(AuthTokenService);
  private readonly router = inject(Router);

  protected readonly submitting = signal(false);
  protected readonly errorMessage = signal<string | null>(null);

  protected readonly form = this.formBuilder.nonNullable.group({
    subdomain: ['', [Validators.required]],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required]],
  });

  async submit(): Promise<void> {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.submitting.set(true);
    this.errorMessage.set(null);
    try {
      const { subdomain, email, password } = this.form.getRawValue();
      const tokens = await this.authService.login(subdomain, email, password);
      this.authTokenService.store(tokens);
      await this.router.navigateByUrl('/home');
    } catch (error) {
      this.errorMessage.set(extractErrorMessage(error, 'Identifiants invalides.'));
    } finally {
      this.submitting.set(false);
    }
  }
}
