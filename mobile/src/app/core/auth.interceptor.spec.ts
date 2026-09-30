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

    httpClient.get(`${environment.apiUrl}/students`).subscribe({ error: () => undefined });
    httpMock.expectOne(`${environment.apiUrl}/students`).flush('expired', { status: 401, statusText: 'Unauthorized' });
    httpMock
      .expectOne(`${environment.apiUrl}/auth/refresh`)
      .flush('revoked', { status: 401, statusText: 'Unauthorized' });
    await laisserPasserLesPromesses();

    expect(authTokenService.read()).toBeNull();
    expect(router.navigateByUrl).toHaveBeenCalledWith('/login');
  });

  /** La session doit survivre à la fermeture de l'application : un téléphone a un seul porteur. */
  it('keeps the session across application restarts', () => {
    authTokenService.store({ accessToken: 'abc', refreshToken: 'def', expiresIn: 900 });

    expect(localStorage.getItem('auth-tokens')).not.toBeNull();
    expect(sessionStorage.getItem('auth-tokens')).toBeNull();
  });
});
