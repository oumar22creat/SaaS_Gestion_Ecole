import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import {
  IonButton,
  IonCard,
  IonCardContent,
  IonCardHeader,
  IonCardTitle,
  IonContent,
  IonInput,
  IonItem,
  IonSpinner,
  IonText,
} from '@ionic/angular';
import { extractErrorMessage } from '../core/http-error.util';
import { AuthTokenService } from './auth-token.service';
import { AuthService } from './auth.service';

/** Identique en logique à web/src/app/auth/login.page.ts — composants Ionic natifs (docs/DESIGN.md §3). */
@Component({
  selector: 'app-login-page',
  imports: [
    ReactiveFormsModule,
    IonContent,
    IonCard,
    IonCardHeader,
    IonCardTitle,
    IonCardContent,
    IonItem,
    IonInput,
    IonButton,
    IonSpinner,
    IonText,
  ],
  templateUrl: './login.page.html',
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
