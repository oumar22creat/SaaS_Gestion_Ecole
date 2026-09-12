import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { AuthTokenService } from '../auth/auth-token.service';
import { RegistrationService } from './registration.service';

interface ApiErrorBody {
  error?: { code?: string; message?: string };
}

@Component({
  selector: 'app-registration-page',
  imports: [
    ReactiveFormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './registration.page.html',
  styleUrl: './registration.page.scss',
})
export class RegistrationPage {
  private readonly formBuilder = inject(FormBuilder);
  private readonly registrationService = inject(RegistrationService);
  private readonly authTokenService = inject(AuthTokenService);

  protected readonly submitting = signal(false);
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly success = signal(false);

  protected readonly form = this.formBuilder.nonNullable.group({
    schoolName: ['', [Validators.required, Validators.maxLength(255)]],
    subdomain: ['', [Validators.required, Validators.pattern(/^[a-z0-9](?:[a-z0-9-]{1,61}[a-z0-9])?$/)]],
    adminFirstName: ['', [Validators.required, Validators.maxLength(100)]],
    adminLastName: ['', [Validators.required, Validators.maxLength(100)]],
    adminEmail: ['', [Validators.required, Validators.email]],
    adminPassword: ['', [Validators.required, Validators.minLength(8)]],
  });

  async submit(): Promise<void> {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.submitting.set(true);
    this.errorMessage.set(null);
    try {
      const response = await this.registrationService.register(this.form.getRawValue());
      this.authTokenService.store(response.tokens);
      this.success.set(true);
    } catch (error) {
      this.errorMessage.set(this.extractErrorMessage(error));
    } finally {
      this.submitting.set(false);
    }
  }

  private extractErrorMessage(error: unknown): string {
    if (error instanceof HttpErrorResponse) {
      const body = error.error as ApiErrorBody;
      if (body?.error?.message) {
        return body.error.message;
      }
    }
    return "Une erreur est survenue. Merci de réessayer.";
  }
}
