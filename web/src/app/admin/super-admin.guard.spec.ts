import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { AuthTokenService } from '../auth/auth-token.service';
import { superAdminGuard } from './super-admin.guard';

function jwtWithRole(role: string): string {
  return `h.${btoa(JSON.stringify({ role }))}.s`;
}

describe('superAdminGuard', () => {
  beforeEach(() => {
    sessionStorage.clear();
    TestBed.configureTestingModule({ providers: [provideRouter([])] });
  });

  afterEach(() => sessionStorage.clear());

  it('allows SUPER_ADMIN', () => {
    TestBed.inject(AuthTokenService).store({
      accessToken: jwtWithRole('SUPER_ADMIN'),
      refreshToken: 'def',
      expiresIn: 900,
    });

    const result = TestBed.runInInjectionContext(() => superAdminGuard({} as never, {} as never));

    expect(result).toBe(true);
  });

  it('redirects an establishment ADMIN to /admin/login', () => {
    TestBed.inject(AuthTokenService).store({
      accessToken: jwtWithRole('ADMIN'),
      refreshToken: 'def',
      expiresIn: 900,
    });

    const result = TestBed.runInInjectionContext(() => superAdminGuard({} as never, {} as never));

    expect(String(result)).toBe('/admin/login');
  });

  it('redirects others to /admin/login', () => {
    const result = TestBed.runInInjectionContext(() => superAdminGuard({} as never, {} as never));

    expect(String(result)).toBe('/admin/login');
  });
});
