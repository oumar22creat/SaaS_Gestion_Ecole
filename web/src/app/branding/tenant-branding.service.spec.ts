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
});
