import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { environment } from '../../environments/environment';
import { AuthTokenService } from '../auth/auth-token.service';
import { authInterceptor } from './auth.interceptor';

/** Laisse la chaîne de promesses du rafraîchissement se dérouler entièrement. */
const laisserPasserLesPromesses = () => new Promise((resolve) => setTimeout(resolve, 0));

describe('authInterceptor', () => {
  let httpClient: HttpClient;
  let httpMock: HttpTestingController;
  let authTokenService: AuthTokenService;
  let router: Router;

  beforeEach(() => {
    sessionStorage.clear();
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        provideRouter([]),
      ],
    });
    httpClient = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);
    authTokenService = TestBed.inject(AuthTokenService);
    router = TestBed.inject(Router);
    spyOn(router, 'navigateByUrl').and.resolveTo(true);
  });

  afterEach(() => {
    httpMock.verify();
    sessionStorage.clear();
  });

  it('attaches the bearer token to API requests when authenticated', () => {
    authTokenService.store({ accessToken: 'abc', refreshToken: 'def', expiresIn: 900 });

    httpClient.get(`${environment.apiUrl}/students`).subscribe();
    const req = httpMock.expectOne(`${environment.apiUrl}/students`);
    expect(req.request.headers.get('Authorization')).toBe('Bearer abc');
    req.flush({ data: [] });
  });

  it('does not attach a header nor redirect on a 401 from an anonymous call', () => {
    httpClient
      .get(`${environment.apiUrl}/tenants/current/branding`)
      .subscribe({ error: () => undefined });
    const req = httpMock.expectOne(`${environment.apiUrl}/tenants/current/branding`);
    expect(req.request.headers.has('Authorization')).toBe(false);
    req.flush('unauthorized', { status: 401, statusText: 'Unauthorized' });

    expect(router.navigateByUrl).not.toHaveBeenCalled();
  });

  /** Le cas courant : le jeton d'accès a quinze minutes, l'utilisateur travaille encore. */
  it('renews the session silently and replays the request on a 401', async () => {
    authTokenService.store({ accessToken: 'perime', refreshToken: 'long', expiresIn: 900 });

    let recu: unknown = null;
    httpClient.get(`${environment.apiUrl}/students`).subscribe((r) => (recu = r));

    httpMock
      .expectOne((r) => r.url.endsWith('/students') && r.headers.get('Authorization') === 'Bearer perime')
      .flush('expired', { status: 401, statusText: 'Unauthorized' });

    const renouvellement = httpMock.expectOne(`${environment.apiUrl}/auth/refresh`);
    expect(renouvellement.request.body).toEqual({ refreshToken: 'long' });
    renouvellement.flush({
      data: { accessToken: 'neuf', refreshToken: 'long2', expiresIn: 900 },
    });

    await laisserPasserLesPromesses();

    // La requête d'origine est rejouée avec le nouveau jeton, l'utilisateur n'a rien vu.
    httpMock
      .expectOne((r) => r.url.endsWith('/students') && r.headers.get('Authorization') === 'Bearer neuf')
      .flush({ data: [] });

    expect(recu).toEqual({ data: [] });
    expect(router.navigateByUrl).not.toHaveBeenCalled();
    expect(authTokenService.read()?.accessToken).toBe('neuf');
  });

  it('gives up and redirects to /login when the renewal fails too', async () => {
    authTokenService.store({ accessToken: 'perime', refreshToken: 'revoque', expiresIn: 900 });

    httpClient.get(`${environment.apiUrl}/students`).subscribe({ error: () => undefined });
    httpMock.expectOne(`${environment.apiUrl}/students`).flush('expired', { status: 401, statusText: 'Unauthorized' });
    httpMock
      .expectOne(`${environment.apiUrl}/auth/refresh`)
      .flush('revoked', { status: 401, statusText: 'Unauthorized' });

    await laisserPasserLesPromesses();

    expect(authTokenService.read()).toBeNull();
    expect(router.navigateByUrl).toHaveBeenCalledWith('/login');
  });

  it('redirects SUPER_ADMIN to /admin/login when the renewal fails', async () => {
    const payload = btoa(JSON.stringify({ role: 'SUPER_ADMIN' }));
    authTokenService.store({ accessToken: `h.${payload}.s`, refreshToken: 'revoque', expiresIn: 900 });

    httpClient
      .get(`${environment.apiUrl}/admin/dashboard/summary`)
      .subscribe({ error: () => undefined });
    httpMock
      .expectOne(`${environment.apiUrl}/admin/dashboard/summary`)
      .flush('expired', { status: 401, statusText: 'Unauthorized' });
    httpMock
      .expectOne(`${environment.apiUrl}/auth/refresh`)
      .flush('revoked', { status: 401, statusText: 'Unauthorized' });

    await laisserPasserLesPromesses();

    expect(router.navigateByUrl).toHaveBeenCalledWith('/admin/login');
  });

  /**
   * Le serveur révoque l'ancien jeton en émettant le nouveau. Plusieurs rafraîchissements
   * concurrents feraient donc réussir le premier et échouer les suivants sur un jeton déjà
   * révoqué — l'utilisateur serait déconnecté malgré le renouvellement. Un écran qui charge
   * trois listes à la fois produit exactement cette situation.
   */
  it('renews only once when several requests expire together', async () => {
    authTokenService.store({ accessToken: 'perime', refreshToken: 'long', expiresIn: 900 });

    httpClient.get(`${environment.apiUrl}/students`).subscribe({ error: () => undefined });
    httpClient.get(`${environment.apiUrl}/parents`).subscribe({ error: () => undefined });
    httpClient.get(`${environment.apiUrl}/teachers`).subscribe({ error: () => undefined });

    for (const chemin of ['/students', '/parents', '/teachers']) {
      httpMock
        .expectOne(`${environment.apiUrl}${chemin}`)
        .flush('expired', { status: 401, statusText: 'Unauthorized' });
    }

    // Un seul appel de rafraîchissement, partagé par les trois requêtes.
    const renouvellements = httpMock.match(`${environment.apiUrl}/auth/refresh`);
    expect(renouvellements.length).toBe(1);
    renouvellements[0].flush({
      data: { accessToken: 'neuf', refreshToken: 'long2', expiresIn: 900 },
    });

    await laisserPasserLesPromesses();

    for (const chemin of ['/students', '/parents', '/teachers']) {
      httpMock
        .expectOne((r) => r.url.endsWith(chemin) && r.headers.get('Authorization') === 'Bearer neuf')
        .flush({ data: [] });
    }
    expect(router.navigateByUrl).not.toHaveBeenCalled();
  });

  /**
   * Abonnement échu : le serveur ferme l'accès. Sans cette redirection, l'établissement
   * enchaîne les messages d'erreur écran par écran sans apprendre que c'est son abonnement
   * qu'il faut renouveler.
   */
  it('sends a suspended establishment to the blocked screen without losing its session', () => {
    authTokenService.store({ accessToken: 'abc', refreshToken: 'def', expiresIn: 900 });

    httpClient.get(`${environment.apiUrl}/students`).subscribe({ error: () => undefined });
    httpMock
      .expectOne(`${environment.apiUrl}/students`)
      .flush(
        { error: { code: 'TENANT_SUSPENDED', message: 'Abonnement échu', details: [] } },
        { status: 403, statusText: 'Forbidden' },
      );

    expect(router.navigateByUrl).toHaveBeenCalledWith('/abonnement-echu');
    // La session reste ouverte : c'est l'établissement qui est fermé, pas le compte.
    expect(authTokenService.read()).not.toBeNull();
  });

  /** Un 403 ordinaire reste un défaut de droits, pas une histoire d'abonnement. */
  it('leaves an ordinary 403 alone', () => {
    authTokenService.store({ accessToken: 'abc', refreshToken: 'def', expiresIn: 900 });

    httpClient.get(`${environment.apiUrl}/students`).subscribe({ error: () => undefined });
    httpMock
      .expectOne(`${environment.apiUrl}/students`)
      .flush(
        { error: { code: 'ACCESS_DENIED', message: 'Accès refusé', details: [] } },
        { status: 403, statusText: 'Forbidden' },
      );

    expect(router.navigateByUrl).not.toHaveBeenCalled();
  });
});
