import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { environment } from '../../environments/environment';
import { AuthTokenService } from '../auth/auth-token.service';
import { RegistrationPage } from './registration.page';

describe('RegistrationPage', () => {
  let fixture: ComponentFixture<RegistrationPage>;
  let component: RegistrationPage;
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    sessionStorage.clear();
    await TestBed.configureTestingModule({
      imports: [RegistrationPage],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();

    fixture = TestBed.createComponent(RegistrationPage);
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
    sessionStorage.clear();
  });

  it('does not submit an invalid form', async () => {
    await component.submit();
    expect(component['form'].invalid).toBe(true);
    httpMock.expectNone(`${environment.apiUrl}/tenants/register`);
  });

  it('registers the establishment and stores the returned tokens on success', async () => {
    component['form'].setValue({
      schoolName: 'École Test',
      subdomain: 'ecole-test',
      adminFirstName: 'Ada',
      adminLastName: 'Lovelace',
      adminEmail: 'admin@ecole-test.example',
      adminPassword: 'Sup3rSecret!',
    });

    const submitPromise = component.submit();
    const req = httpMock.expectOne(`${environment.apiUrl}/tenants/register`);
    req.flush({
      data: {
        tenantId: 1,
        subdomain: 'ecole-test',
        tokens: { accessToken: 'access', refreshToken: 'refresh', expiresIn: 900 },
      },
    });
    await submitPromise;

    expect(component['success']()).toBe(true);
    expect(TestBed.inject(AuthTokenService).read()).toEqual({
      accessToken: 'access',
      refreshToken: 'refresh',
      expiresIn: 900,
    });
  });

  it('shows the server error message when registration fails', async () => {
    component['form'].setValue({
      schoolName: 'École Test',
      subdomain: 'ecole-deja-prise',
      adminFirstName: 'Ada',
      adminLastName: 'Lovelace',
      adminEmail: 'admin@ecole-test.example',
      adminPassword: 'Sup3rSecret!',
    });

    const submitPromise = component.submit();
    const req = httpMock.expectOne(`${environment.apiUrl}/tenants/register`);
    req.flush(
      { error: { code: 'SUBDOMAIN_ALREADY_TAKEN', message: 'Ce sous-domaine est déjà utilisé' } },
      { status: 409, statusText: 'Conflict' },
    );
    await submitPromise;

    expect(component['errorMessage']()).toBe('Ce sous-domaine est déjà utilisé');
    expect(component['success']()).toBe(false);
  });
});
