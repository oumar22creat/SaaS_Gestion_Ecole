import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { environment } from '../../environments/environment';
import { AuthTokenService } from '../auth/auth-token.service';
import { authInterceptor } from './auth.interceptor';

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

  it('clears the session and redirects to /login on a 401 for an authenticated call', () => {
    authTokenService.store({ accessToken: 'abc', refreshToken: 'def', expiresIn: 900 });

    httpClient.get(`${environment.apiUrl}/students`).subscribe({ error: () => undefined });
    const req = httpMock.expectOne(`${environment.apiUrl}/students`);
    req.flush('unauthorized', { status: 401, statusText: 'Unauthorized' });

    expect(authTokenService.read()).toBeNull();
    expect(router.navigateByUrl).toHaveBeenCalledWith('/login');
  });
});
