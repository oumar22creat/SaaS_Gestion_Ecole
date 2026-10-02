import { DOCUMENT } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { environment } from '../../environments/environment';
import { contrastColor } from './contrast-color';
import { DEFAULT_BRANDING, TenantBranding } from './tenant-branding.model';

const CACHE_KEY = 'tenant-branding';

/**
 * Complète une marque partielle par la marque neutre, champ par champ.
 *
 * <p>Le serveur renvoie un nom vide tant qu'aucun établissement n'est résolu — écran de
 * connexion, page d'inscription. Appliqué tel quel, il remplaçait le nom du produit par du
 * vide et l'application se lançait sur une barre de titre muette.
 *
 * <p>La même règle vaut pour la valeur en cache, et pas seulement pour la réponse réseau :
 * un cache écrit par une version antérieure contient encore un nom nul, et l'en-tête restait
 * muet au lancement — définitivement si l'appel échoue.
 */
function completer(branding: Partial<TenantBranding> | null): TenantBranding {
  return {
    name: branding?.name || DEFAULT_BRANDING.name,
    logoUrl: branding?.logoUrl ?? null,
    primaryColor: branding?.primaryColor || DEFAULT_BRANDING.primaryColor,
    secondaryColor: branding?.secondaryColor || DEFAULT_BRANDING.secondaryColor,
  };
}

@Injectable({ providedIn: 'root' })
export class TenantBrandingService {
  private readonly http = inject(HttpClient);
  private readonly document = inject(DOCUMENT);

  readonly branding = signal<TenantBranding>(DEFAULT_BRANDING);

  // Appelé une fois au démarrage de l'app (voir provideAppInitializer dans app.config.ts).
  // Ne bloque jamais le premier rendu : le branding en cache (ou neutre par défaut) est
  // appliqué immédiatement, l'appel réseau se fait en arrière-plan.
  init(): void {
    this.apply(completer(this.readCache()));

    this.http.get<TenantBranding>(`${environment.apiUrl}/tenants/current/branding`).subscribe({
      next: (branding) => {
        const complete = completer(branding);
        this.apply(complete);
        this.writeCache(complete);
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
