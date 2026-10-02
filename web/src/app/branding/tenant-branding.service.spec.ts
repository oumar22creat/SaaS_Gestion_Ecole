import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { environment } from '../../environments/environment';
import { DEFAULT_BRANDING, TenantBranding } from './tenant-branding.model';
import { TenantBrandingService } from './tenant-branding.service';

describe('TenantBrandingService', () => {
  let service: TenantBrandingService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    sessionStorage.clear();
    document.documentElement.style.removeProperty('--tenant-primary');
    document.documentElement.style.removeProperty('--tenant-secondary');
    document.documentElement.style.removeProperty('--tenant-on-primary');

    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(TenantBrandingService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
    sessionStorage.clear();
    // Ces tests posent des propriétés CSS inline sur <html>, qui persisteraient sinon au-delà
    // de ce fichier de test (même document partagé par tous les specs dans Karma).
    document.documentElement.style.removeProperty('--tenant-primary');
    document.documentElement.style.removeProperty('--tenant-secondary');
    document.documentElement.style.removeProperty('--tenant-on-primary');
  });

  it('applies the neutral default branding immediately, before the API responds', () => {
    service.init();

    expect(service.branding()).toEqual(DEFAULT_BRANDING);
    expect(
      getComputedStyle(document.documentElement).getPropertyValue('--tenant-primary').trim(),
    ).toBe(DEFAULT_BRANDING.primaryColor);

    httpMock.expectOne(`${environment.apiUrl}/tenants/current/branding`).flush(DEFAULT_BRANDING);
  });

  it('applies and caches the branding returned by the API', () => {
    const branding: TenantBranding = {
      name: 'École Test',
      logoUrl: 'https://cdn.example/logo.png',
      primaryColor: '#123456',
      secondaryColor: '#abcdef',
    };

    service.init();
    httpMock.expectOne(`${environment.apiUrl}/tenants/current/branding`).flush(branding);

    expect(service.branding()).toEqual(branding);
    expect(document.documentElement.style.getPropertyValue('--tenant-primary')).toBe('#123456');
    expect(JSON.parse(sessionStorage.getItem('tenant-branding')!)).toEqual(branding);
  });

  it('keeps the neutral default branding if the API call fails', () => {
    service.init();
    httpMock
      .expectOne(`${environment.apiUrl}/tenants/current/branding`)
      .flush('error', { status: 404, statusText: 'Not Found' });

    expect(service.branding()).toEqual(DEFAULT_BRANDING);
  });

  it('applies the cached branding immediately on a later init (no flash)', () => {
    const cached: TenantBranding = {
      name: 'École Cache',
      logoUrl: null,
      primaryColor: '#654321',
      secondaryColor: '#fedcba',
    };
    sessionStorage.setItem('tenant-branding', JSON.stringify(cached));

    service.init();

    expect(service.branding()).toEqual(cached);
    httpMock.expectOne(`${environment.apiUrl}/tenants/current/branding`).flush(cached);
  });

  /**
   * Le serveur renvoie la marque neutre sans nom tant qu'aucun établissement n'est résolu —
   * écran de connexion, page d'inscription. Appliquée telle quelle, elle vidait l'en-tête de
   * l'application : le nom du produit y laissait place à une barre de titre muette.
   */
  it('complète une réponse partielle au lieu de vider la marque', () => {
    service.init();
    httpMock.expectOne(`${environment.apiUrl}/tenants/current/branding`).flush({
      name: null,
      logoUrl: null,
      primaryColor: '#0f5c4c',
      secondaryColor: '#c9a227',
    });

    expect(service.branding().name).toBe(DEFAULT_BRANDING.name);
    expect(service.branding().primaryColor).toBe('#0f5c4c');
  });

  /**
   * Le cache peut provenir d'une version anterieure de l'application, ou l'appel reseau
   * echouer : l'en-tete restait alors muet au lancement, avant toute reponse du serveur.
   */
  it('complète aussi la marque lue dans le cache', () => {
    localStorage.setItem(
      'tenant-branding',
      JSON.stringify({ name: null, logoUrl: null, primaryColor: '#0f5c4c', secondaryColor: '#c9a227' }),
    );

    service.init();

    expect(service.branding().name).toBe(DEFAULT_BRANDING.name);
    httpMock.expectOne(`${environment.apiUrl}/tenants/current/branding`).flush({}, { status: 503, statusText: 'x' });
    localStorage.removeItem('tenant-branding');
  });
});
