import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatIconModule } from '@angular/material/icon';
import { extractErrorMessage } from '../core/http-error.util';
import { TenantBrandingService } from '../branding/tenant-branding.service';
import { TenantSettingsService } from './tenant-settings.service';

@Component({
  selector: 'app-settings-page',
  imports: [FormsModule, MatFormFieldModule, MatInputModule, MatButtonModule, MatIconModule],
  template: `
    <div class="page-header">
      <h1><mat-icon class="page-icon" aria-hidden="true">palette</mat-icon>Établissement</h1>
    </div>
    <form class="stack-form" (submit)="save($event)">
      <mat-form-field appearance="outline"><mat-label>URL du logo</mat-label><input matInput [(ngModel)]="logoUrl" name="logoUrl" /></mat-form-field>
      <mat-form-field appearance="outline"><mat-label>Couleur primaire</mat-label><input matInput [(ngModel)]="primaryColor" name="primaryColor" /></mat-form-field>
      <mat-form-field appearance="outline"><mat-label>Couleur secondaire</mat-label><input matInput [(ngModel)]="secondaryColor" name="secondaryColor" /></mat-form-field>
      <mat-form-field appearance="outline"><mat-label>Domaine personnalisé (Premium)</mat-label><input matInput [(ngModel)]="customDomain" name="customDomain" /></mat-form-field>
      <button mat-flat-button type="submit">Enregistrer</button>
    </form>
    @if (errorMessage()) {
      <p class="flash-error">{{ errorMessage() }}</p>
    }
    @if (success()) {
      <p class="flash-success">Paramètres enregistrés. Rechargez pour voir le branding.</p>
    }
  `,
})
export class SettingsPage {
  private readonly settingsService = inject(TenantSettingsService);
  private readonly brandingService = inject(TenantBrandingService);

  protected logoUrl = '';
  protected primaryColor = '#0f5c4c';
  protected secondaryColor = '#c9a227';
  protected customDomain = '';
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly success = signal(false);

  constructor() {
    const branding = this.brandingService.branding();
    this.logoUrl = branding.logoUrl ?? '';
    this.primaryColor = branding.primaryColor;
    this.secondaryColor = branding.secondaryColor;
    void this.settingsService.customDomain().then((domain) => (this.customDomain = domain ?? ''));
  }

  async save(event: Event): Promise<void> {
    event.preventDefault();
    this.errorMessage.set(null);
    try {
      await this.settingsService.updateBranding(this.logoUrl || null, this.primaryColor, this.secondaryColor);
      if (this.customDomain) {
        await this.settingsService.updateCustomDomain(this.customDomain);
      }
      this.success.set(true);
    } catch (error) {
      this.errorMessage.set(extractErrorMessage(error));
    }
  }
}
