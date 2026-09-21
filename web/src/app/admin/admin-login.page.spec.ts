import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { environment } from '../../environments/environment';
import { AuthTokenService } from '../auth/auth-token.service';
import { AdminLoginPage } from './admin-login.page';

function jwtWithRole(role: string): string {
  return `h.${btoa(JSON.stringify({ role, email: 'superadmin@platform.test' }))}.s`;
}

describe('AdminLoginPage', () => {
  let fixture: ComponentFixture<AdminLoginPage>;
  let component: AdminLoginPage;
  let httpMock: HttpTestingController;
  let router: Router;

  beforeEach(async () => {
    sessionStorage.clear();
    await TestBed.configureTestingModule({
      imports: [AdminLoginPage],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    }).compileComponents();

    fixture = TestBed.createComponent(AdminLoginPage);
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
    spyOn(router, 'navigateByUrl').and.resolveTo(true);
  });

  afterEach(() => {
    httpMock.verify();
    sessionStorage.clear();
  });

  it('stores the Super-Admin tokens and opens the platform console', async () => {
    component['form'].setValue({ email: 'superadmin@platform.test', password: 'SuperAdmin2026!' });

    const submitPromise = component.submit();
    httpMock.expectOne(`${environment.apiUrl}/admin/auth/login`).flush({
      data: { accessToken: jwtWithRole('SUPER_ADMIN'), refreshToken: 'refresh', expiresIn: 900 },
    });
    await submitPromise;

    expect(TestBed.inject(AuthTokenService).role()).toBe('SUPER_ADMIN');
    expect(router.navigateByUrl).toHaveBeenCalledWith('/admin');
  });
});
