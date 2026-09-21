import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { AuthTokenService } from '../auth/auth-token.service';
import { tenantWebGuard } from './tenant-web.guard';

function jwtWithRole(role: string): string {
  return `h.${btoa(JSON.stringify({ role }))}.s`;
}

describe('tenantWebGuard', () => {
  beforeEach(() => {
    sessionStorage.clear();
    TestBed.configureTestingModule({ providers: [provideRouter([])] });
  });

  afterEach(() => sessionStorage.clear());

  it('lets an establishment ADMIN into the tenant shell', () => {
    TestBed.inject(AuthTokenService).store({
      accessToken: jwtWithRole('ADMIN'),
      refreshToken: 'def',
      expiresIn: 900,
    });

    const result = TestBed.runInInjectionContext(() => tenantWebGuard({} as never, {} as never));

    expect(result).toBe(true);
  });

  it('redirects SUPER_ADMIN to the platform console', () => {
    TestBed.inject(AuthTokenService).store({
      accessToken: jwtWithRole('SUPER_ADMIN'),
      refreshToken: 'def',
      expiresIn: 900,
    });

    const result = TestBed.runInInjectionContext(() => tenantWebGuard({} as never, {} as never));

    expect(String(result)).toBe('/admin');
  });
});
