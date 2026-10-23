import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { environment } from '../../environments/environment';
import { AuthTokenService } from '../auth/auth-token.service';
import { PushNotificationService } from '../push/push-notification.service';
import { authInterceptor } from './auth.interceptor';

/** Laisse la chaîne de promesses du rafraîchissement se dérouler entièrement. */
const laisserPasserLesPromesses = () => new Promise((resolve) => setTimeout(resolve, 0));

describe('authInterceptor', () => {
  let httpClient: HttpClient;
  let httpMock: HttpTestingController;
  let authTokenService: AuthTokenService;
  let router: Router;

  beforeEach(() => {
    localStorage.clear();
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
    localStorage.clear();
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

  /** Le cas courant : le jeton d'accès a quinze minutes, l'enseignant fait encore l'appel. */
  it('renews the session silently and replays the request on a 401', async () => {
    authTokenService.store({ accessToken: 'perime', refreshToken: 'long', expiresIn: 900 });

    let recu: unknown = null;
    httpClient.get(`${environment.apiUrl}/students`).subscribe((r) => (recu = r));

    httpMock
      .expectOne((r) => r.url.endsWith('/students') && r.headers.get('Authorization') === 'Bearer perime')
      .flush('expired', { status: 401, statusText: 'Unauthorized' });

    httpMock.expectOne(`${environment.apiUrl}/auth/refresh`).flush({
      data: { accessToken: 'neuf', refreshToken: 'long2', expiresIn: 900 },
    });
    await laisserPasserLesPromesses();

    httpMock
      .expectOne((r) => r.url.endsWith('/students') && r.headers.get('Authorization') === 'Bearer neuf')
      .flush({ data: [] });

    expect(recu).toEqual({ data: [] });
    expect(router.navigateByUrl).not.toHaveBeenCalled();
  });

  it('gives up and redirects to /login when the renewal fails too', async () => {
    authTokenService.store({ accessToken: 'perime', refreshToken: 'revoque', expiresIn: 900 });
    const push = TestBed.inject(PushNotificationService);
    spyOn(push, 'invaliderLocalement').and.resolveTo();

    httpClient.get(`${environment.apiUrl}/students`).subscribe({ error: () => undefined });
    httpMock.expectOne(`${environment.apiUrl}/students`).flush('expired', { status: 401, statusText: 'Unauthorized' });
    httpMock
      .expectOne(`${environment.apiUrl}/auth/refresh`)
      .flush('revoked', { status: 401, statusText: 'Unauthorized' });
    await laisserPasserLesPromesses();

    expect(authTokenService.read()).toBeNull();
    expect(router.navigateByUrl).toHaveBeenCalledWith('/login');
    // Plus de requête authentifiée possible : le jeton d'appareil est au moins invalidé chez
    // Firebase, pour que ce téléphone cesse de recevoir les notifications du compte perdu.
    expect(push.invaliderLocalement).toHaveBeenCalled();
  });

  /** Zone blanche : une coupure pendant le renouvellement ne dit rien de la session. */
  it('keeps the session when the renewal fails for lack of network', async () => {
    authTokenService.store({ accessToken: 'perime', refreshToken: 'valide', expiresIn: 900 });
    const push = TestBed.inject(PushNotificationService);
    spyOn(push, 'invaliderLocalement').and.resolveTo();

    httpClient.get(`${environment.apiUrl}/students`).subscribe({ error: () => undefined });
    httpMock.expectOne(`${environment.apiUrl}/students`).flush('expired', { status: 401, statusText: 'Unauthorized' });
    httpMock
      .expectOne(`${environment.apiUrl}/auth/refresh`)
      .error(new ProgressEvent('error'), { status: 0, statusText: 'Unknown Error' });
    await laisserPasserLesPromesses();

    expect(authTokenService.read()?.refreshToken).toBe('valide');
    expect(router.navigateByUrl).not.toHaveBeenCalled();
    expect(push.invaliderLocalement).not.toHaveBeenCalled();
  });

  /** Plusieurs requêtes expirent ensemble et partagent un même renouvellement refusé. */
  it('closes the session only once when several requests fail on the same renewal', async () => {
    authTokenService.store({ accessToken: 'perime', refreshToken: 'revoque', expiresIn: 900 });
    const push = TestBed.inject(PushNotificationService);
    spyOn(push, 'invaliderLocalement').and.resolveTo();

    httpClient.get(`${environment.apiUrl}/students`).subscribe({ error: () => undefined });
    httpClient.get(`${environment.apiUrl}/teachers`).subscribe({ error: () => undefined });
    httpMock.expectOne(`${environment.apiUrl}/students`).flush('expired', { status: 401, statusText: 'Unauthorized' });
    httpMock.expectOne(`${environment.apiUrl}/teachers`).flush('expired', { status: 401, statusText: 'Unauthorized' });
    httpMock
      .expectOne(`${environment.apiUrl}/auth/refresh`)
      .flush('revoked', { status: 401, statusText: 'Unauthorized' });
    await laisserPasserLesPromesses();

    expect(authTokenService.read()).toBeNull();
    expect(push.invaliderLocalement).toHaveBeenCalledTimes(1);
    expect(router.navigateByUrl).toHaveBeenCalledTimes(1);
  });

  /** Le renouvellement a réussi ; c'est la requête rejouée qui échoue, pour ses propres raisons. */
  it('keeps the renewed session when the replayed request is forbidden', async () => {
    authTokenService.store({ accessToken: 'perime', refreshToken: 'valide', expiresIn: 900 });

    httpClient.get(`${environment.apiUrl}/students`).subscribe({ error: () => undefined });
    httpMock.expectOne(`${environment.apiUrl}/students`).flush('expired', { status: 401, statusText: 'Unauthorized' });
    httpMock
      .expectOne(`${environment.apiUrl}/auth/refresh`)
      .flush({ data: { accessToken: 'neuf', refreshToken: 'neuf-r', expiresIn: 900 } });
    await laisserPasserLesPromesses();
    httpMock
      .expectOne((r) => r.url.endsWith('/students') && r.headers.get('Authorization') === 'Bearer neuf')
      .flush('forbidden', { status: 403, statusText: 'Forbidden' });
    await laisserPasserLesPromesses();

    expect(authTokenService.read()?.accessToken).toBe('neuf');
    expect(router.navigateByUrl).not.toHaveBeenCalled();
  });

  /**
   * Une déconnexion n'attend pas un renouvellement lent : sa réponse, arrivée après
   * l'effacement, ne doit pas réécrire la session — sinon la personne suivante rouvre
   * l'application sur le compte précédent.
   */
  it('does not store a renewal that completes after the session was cleared', async () => {
    authTokenService.store({ accessToken: 'perime', refreshToken: 'valide', expiresIn: 900 });

    httpClient.get(`${environment.apiUrl}/students`).subscribe({ error: () => undefined });
    httpMock.expectOne(`${environment.apiUrl}/students`).flush('expired', { status: 401, statusText: 'Unauthorized' });
    const renouvellement = httpMock.expectOne(`${environment.apiUrl}/auth/refresh`);
    authTokenService.clear();
    renouvellement.flush({ data: { accessToken: 'neuf', refreshToken: 'neuf-r', expiresIn: 900 } });
    await laisserPasserLesPromesses();

    expect(authTokenService.read()).toBeNull();
    expect(localStorage.getItem('auth-tokens')).toBeNull();
    httpMock.expectNone((r) => r.url.endsWith('/students'));
    // Session déjà fermée : pas de seconde fermeture ni de redirection.
    expect(router.navigateByUrl).not.toHaveBeenCalled();
  });

  /** Même chose si un autre compte s'est connecté entre-temps : sa session n'est pas écrasée. */
  it('does not overwrite the session of an account that logged in meanwhile', async () => {
    authTokenService.store({ accessToken: 'perime', refreshToken: 'ancien', expiresIn: 900 });

    httpClient.get(`${environment.apiUrl}/students`).subscribe({ error: () => undefined });
    httpMock.expectOne(`${environment.apiUrl}/students`).flush('expired', { status: 401, statusText: 'Unauthorized' });
    const renouvellement = httpMock.expectOne(`${environment.apiUrl}/auth/refresh`);
    authTokenService.clear();
    authTokenService.store({ accessToken: 'parent', refreshToken: 'parent-r', expiresIn: 900 });
    renouvellement.flush({ data: { accessToken: 'neuf', refreshToken: 'neuf-r', expiresIn: 900 } });
    await laisserPasserLesPromesses();

    expect(authTokenService.read()?.accessToken).toBe('parent');
  });

  /** La session doit survivre à la fermeture de l'application : un téléphone a un seul porteur. */
  it('keeps the session across application restarts', () => {
    authTokenService.store({ accessToken: 'abc', refreshToken: 'def', expiresIn: 900 });

    expect(localStorage.getItem('auth-tokens')).not.toBeNull();
    expect(sessionStorage.getItem('auth-tokens')).toBeNull();
  });
});
