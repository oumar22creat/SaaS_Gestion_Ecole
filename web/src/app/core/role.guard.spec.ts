import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, provideRouter } from '@angular/router';
import { AuthTokenService } from '../auth/auth-token.service';
import { roleGuard } from './role.guard';

function jwtWithRole(role: string): string {
  return `h.${btoa(JSON.stringify({ role }))}.s`;
}

function routeWithRoles(roles: string[]): ActivatedRouteSnapshot {
  return { data: { roles } } as unknown as ActivatedRouteSnapshot;
}

describe('roleGuard', () => {
  beforeEach(() => {
    sessionStorage.clear();
    TestBed.configureTestingModule({ providers: [provideRouter([])] });
  });

  afterEach(() => sessionStorage.clear());

  it('allows ADMIN on an administration route', () => {
    TestBed.inject(AuthTokenService).store({
      accessToken: jwtWithRole('ADMIN'),
      refreshToken: 'def',
      expiresIn: 900,
    });

    const result = TestBed.runInInjectionContext(() =>
      roleGuard(routeWithRoles(['ADMIN', 'DIRECTION']), {} as never),
    );

    expect(result).toBe(true);
  });

  it('sends a teacher away from the administration dashboard', () => {
    TestBed.inject(AuthTokenService).store({
      accessToken: jwtWithRole('TEACHER'),
      refreshToken: 'def',
      expiresIn: 900,
    });

    const result = TestBed.runInInjectionContext(() =>
      roleGuard(routeWithRoles(['ADMIN', 'DIRECTION']), {} as never),
    );

    expect(String(result)).toBe('/attendance');
  });
});
