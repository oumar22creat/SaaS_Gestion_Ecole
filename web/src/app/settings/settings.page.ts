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
    <section class="logo-section">
      <h2 class="section-label">Logo de l'établissement</h2>
      <p class="logo-hint">
        Apparaît en tête des bulletins, certificats de scolarité et reçus de règlement.
        PNG ou JPEG, 2 Mo maximum.
      </p>
      <div class="logo-row">
        @if (logoPreview(); as preview) {
          <img [src]="preview" alt="Logo de l'établissement" class="logo-preview" />
        } @else {
          <span class="logo-empty">Aucun logo</span>
        }
        <input
          #logoInput
          type="file"
          accept="image/png,image/jpeg"
          hidden
          (change)="onLogoChosen($event)"
        />
        <button mat-stroked-button type="button" [disabled]="uploading()" (click)="logoInput.click()">
          <mat-icon>upload</mat-icon> Choisir un logo
        </button>
        <button mat-button type="button" [disabled]="uploading()" (click)="removeLogo()">
          <mat-icon>delete</mat-icon> Retirer
        </button>
      </div>
    </section>

    <form class="stack-form" (submit)="save($event)">
      <mat-form-field appearance="outline">
        <mat-label>URL du logo affiché dans l'application</mat-label>
        <input matInput [(ngModel)]="logoUrl" name="logoUrl" />
        <mat-hint>Facultatif, et distinct du logo des documents ci-dessus.</mat-hint>
      </mat-form-field>
      <mat-form-field appearance="outline"
        ><mat-label>Couleur primaire</mat-label
        ><input matInput [(ngModel)]="primaryColor" name="primaryColor"
      /></mat-form-field>
      <mat-form-field appearance="outline"
        ><mat-label>Couleur secondaire</mat-label
        ><input matInput [(ngModel)]="secondaryColor" name="secondaryColor"
      /></mat-form-field>
      <mat-form-field appearance="outline"
        ><mat-label>Domaine personnalisé (Premium)</mat-label
        ><input matInput [(ngModel)]="customDomain" name="customDomain"
      /></mat-form-field>
      <button mat-flat-button type="submit">Enregistrer</button>
    </form>

    <section class="bulletin-section">
      <h2 class="section-label">Modèle de bulletin</h2>
      <p class="logo-hint">
        Ces mentions s'impriment en tête du bulletin et du certificat de scolarité, et le
        directeur en signe le bas. Saisissez-les telles qu'elles doivent paraître, libellé
        compris. Laissez vide ce qui ne s'applique pas à votre établissement.
      </p>
      <form class="stack-form" (submit)="saveTemplate($event)">
        <mat-form-field appearance="outline">
          <mat-label>Mention de l'État</mat-label>
          <input matInput [(ngModel)]="officialAuthority" name="officialAuthority" />
          <mat-hint>Par exemple : RÉPUBLIQUE DU MALI</mat-hint>
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Académie ou direction régionale</mat-label>
          <input matInput [(ngModel)]="academyLabel" name="academyLabel" />
          <mat-hint>Par exemple : AE : BAMAKO RIVE DROITE</mat-hint>
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Inspection ou circonscription</mat-label>
          <input matInput [(ngModel)]="inspectionLabel" name="inspectionLabel" />
          <mat-hint>Par exemple : CAP : KALABAN COURA</mat-hint>
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Nom du directeur</mat-label>
          <input matInput [(ngModel)]="directorName" name="directorName" />
          <mat-hint>Signataire du bulletin, imprimé sous « LE DIRECTEUR GÉNÉRAL »</mat-hint>
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Ville de signature</mat-label>
          <input matInput [(ngModel)]="headOfficeCity" name="headOfficeCity" />
          <mat-hint>Apparaît en bas de page : « Bamako, le 08/10/2026 »</mat-hint>
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Adresse de l'établissement</mat-label>
          <input matInput [(ngModel)]="postalAddress" name="postalAddress" />
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Mentions légales</mat-label>
          <input matInput [(ngModel)]="reportCardLegalMentions" name="reportCardLegalMentions" />
        </mat-form-field>
        <button mat-flat-button type="submit">Enregistrer le modèle</button>
      </form>
    </section>
    @if (errorMessage()) {
      <p class="flash-error">{{ errorMessage() }}</p>
    }
    @if (success()) {
      <p class="flash-success">Paramètres enregistrés. Rechargez pour voir le branding.</p>
    }
  `,
  styles: `
    .bulletin-section {
      margin-top: var(--space-5);
    }

    .logo-section {
      margin-bottom: var(--space-5);
    }

    .logo-hint {
      margin: 0 0 var(--space-3);
      color: var(--color-text-secondary);
      font-size: var(--font-size-small);
    }

    .logo-row {
      display: flex;
      align-items: center;
      gap: var(--space-3);
    }

    /* Fond clair imposé : un logo à fond transparent serait invisible sur un panneau sombre,
     * alors qu'il sera imprimé sur du papier blanc. */
    .logo-empty {
      color: var(--color-text-secondary);
      font-size: var(--font-size-small);
    }

    .logo-preview {
      max-width: 160px;
      max-height: 64px;
      padding: var(--space-2);
      background: #fff;
      border: 1px solid var(--color-border);
      border-radius: var(--radius-sm);
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
  protected officialAuthority = '';
  protected academyLabel = '';
  protected inspectionLabel = '';
  protected directorName = '';
  protected headOfficeCity = '';
  protected reportCardLegalMentions = '';
  protected postalAddress = '';
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly success = signal(false);
  protected readonly uploading = signal(false);
  protected readonly logoPreview = signal<string | null>(null);

  /** Recharge l'aperçu et libère l'URL précédente, qui resterait sinon en mémoire. */
  private async refreshLogoPreview(): Promise<void> {
    const previous = this.logoPreview();
    this.logoPreview.set(await this.settingsService.loadLogoPreview());
    if (previous) {
      URL.revokeObjectURL(previous);
    }
  }

  async onLogoChosen(event: Event): Promise<void> {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) {
      return;
    }
    this.uploading.set(true);
    this.errorMessage.set(null);
    this.success.set(false);
    try {
      await this.settingsService.uploadLogo(file);
      await this.refreshLogoPreview();
      this.success.set(true);
    } catch (error) {
      this.errorMessage.set(extractErrorMessage(error));
    } finally {
      this.uploading.set(false);
      // Sans cela, re-choisir le même fichier après une erreur ne déclencherait rien.
      input.value = '';
    }
  }

  async removeLogo(): Promise<void> {
    this.errorMessage.set(null);
    try {
      await this.settingsService.removeLogo();
      await this.refreshLogoPreview();
    } catch (error) {
      this.errorMessage.set(extractErrorMessage(error));
    }
  }

  constructor() {
    const branding = this.brandingService.branding();
    this.logoUrl = branding.logoUrl ?? '';
    this.primaryColor = branding.primaryColor;
    this.secondaryColor = branding.secondaryColor;
    void this.settingsService.customDomain().then((domain) => (this.customDomain = domain ?? ''));
    void this.refreshLogoPreview();
    void this.settingsService.reportCardTemplate().then((template) => {
      this.officialAuthority = template.officialAuthority ?? '';
      this.academyLabel = template.academyLabel ?? '';
      this.inspectionLabel = template.inspectionLabel ?? '';
      this.directorName = template.directorName ?? '';
      this.headOfficeCity = template.headOfficeCity ?? '';
      this.postalAddress = template.postalAddress ?? '';
      this.reportCardLegalMentions = template.reportCardLegalMentions ?? '';
      this.reportCardHeader = template.reportCardHeader;
    });
  }

  /** Conservé tel quel : ce champ n'est pas éditable ici, mais l'écraser l'effacerait. */
  private reportCardHeader: string | null = null;

  async saveTemplate(event: Event): Promise<void> {
    event.preventDefault();
    this.errorMessage.set(null);
    this.success.set(false);
    try {
      await this.settingsService.updateReportCardTemplate({
        reportCardHeader: this.reportCardHeader,
        reportCardLegalMentions: this.reportCardLegalMentions || null,
        officialAuthority: this.officialAuthority || null,
        academyLabel: this.academyLabel || null,
        inspectionLabel: this.inspectionLabel || null,
        directorName: this.directorName || null,
        headOfficeCity: this.headOfficeCity || null,
        postalAddress: this.postalAddress || null,
      });
      this.success.set(true);
    } catch (error) {
      this.errorMessage.set(extractErrorMessage(error));
    }
  }

  async save(event: Event): Promise<void> {
    event.preventDefault();
    this.errorMessage.set(null);
    try {
      await this.settingsService.updateBranding(
        this.logoUrl || null,
        this.primaryColor,
        this.secondaryColor,
      );
      if (this.customDomain) {
        await this.settingsService.updateCustomDomain(this.customDomain);
      }
      this.success.set(true);
    } catch (error) {
      this.errorMessage.set(extractErrorMessage(error));
    }
  }
}
