import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { environment } from '../../environments/environment';
import { AuthTokenService } from './auth-token.service';
import { LoginPage } from './login.page';

describe('LoginPage', () => {
  let fixture: ComponentFixture<LoginPage>;
  let component: LoginPage;
  let httpMock: HttpTestingController;
  let router: Router;

  beforeEach(async () => {
    sessionStorage.clear();
    await TestBed.configureTestingModule({
      imports: [LoginPage],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();

    fixture = TestBed.createComponent(LoginPage);
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
    spyOn(router, 'navigateByUrl').and.resolveTo(true);
  });

  afterEach(() => {
    httpMock.verify();
    sessionStorage.clear();
  });

  it('does not submit an invalid form', async () => {
    await component.submit();
    expect(component['form'].invalid).toBe(true);
    httpMock.expectNone(`${environment.apiUrl}/auth/login`);
  });

  it('stores the tokens and navigates to home on success', async () => {
    component['form'].setValue({
      subdomain: 'ecole-test',
      email: 'admin@ecole.example',
      password: 'Sup3rSecret!',
    });

    const submitPromise = component.submit();
    httpMock.expectOne(`${environment.apiUrl}/auth/login`).flush({
      data: { accessToken: 'access', refreshToken: 'refresh', expiresIn: 900 },
    });
    await submitPromise;

    expect(TestBed.inject(AuthTokenService).read()).toEqual({
      accessToken: 'access',
      refreshToken: 'refresh',
      expiresIn: 900,
    });
    expect(router.navigateByUrl).toHaveBeenCalledWith('/home');
  });

  it('shows an error message when the credentials are invalid', async () => {
    component['form'].setValue({
      subdomain: 'ecole-test',
      email: 'admin@ecole.example',
      password: 'wrong',
    });

    const submitPromise = component.submit();
    httpMock
      .expectOne(`${environment.apiUrl}/auth/login`)
      .flush(
        { error: { code: 'INVALID_CREDENTIALS', message: 'Identifiants invalides' } },
        { status: 401, statusText: 'Unauthorized' },
      );
    await submitPromise;

    expect(component['errorMessage']()).toBe('Identifiants invalides');
    expect(TestBed.inject(AuthTokenService).read()).toBeNull();
  });
});
