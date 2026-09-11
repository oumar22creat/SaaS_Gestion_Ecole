import { DOCUMENT } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { environment } from '../../environments/environment';
import { contrastColor } from './contrast-color';
import { DEFAULT_BRANDING, TenantBranding } from './tenant-branding.model';

const CACHE_KEY = 'tenant-branding';

@Injectable({ providedIn: 'root' })
export class TenantBrandingService {
  private readonly http = inject(HttpClient);
  private readonly document = inject(DOCUMENT);

  readonly branding = signal<TenantBranding>(DEFAULT_BRANDING);

  // Appelé une fois au démarrage de l'app (voir provideAppInitializer dans app.config.ts).
  // Ne bloque jamais le premier rendu : le branding en cache (ou neutre par défaut) est
  // appliqué immédiatement, l'appel réseau se fait en arrière-plan.
  init(): void {
    this.apply(this.readCache() ?? DEFAULT_BRANDING);

    this.http.get<TenantBranding>(`${environment.apiUrl}/tenants/current/branding`).subscribe({
      next: (branding) => {
        this.apply(branding);
        this.writeCache(branding);
      },
      error: () => {
        // Pas de tenant résolu, ou API indisponible : le branding neutre déjà appliqué
        // reste actif (voir docs/DESIGN.md §4, point 5 : jamais d'écran cassé).
      },
    });
  }

  private apply(branding: TenantBranding): void {
    this.branding.set(branding);
    const root = this.document.documentElement;
    root.style.setProperty('--tenant-primary', branding.primaryColor);
    root.style.setProperty('--tenant-secondary', branding.secondaryColor);
    root.style.setProperty('--tenant-on-primary', contrastColor(branding.primaryColor));
  }

  private readCache(): TenantBranding | null {
    const raw = sessionStorage.getItem(CACHE_KEY);
    return raw ? (JSON.parse(raw) as TenantBranding) : null;
  }

  private writeCache(branding: TenantBranding): void {
    sessionStorage.setItem(CACHE_KEY, JSON.stringify(branding));
  }
}
