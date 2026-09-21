import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, provideRouter } from '@angular/router';
import { AuthTokenService } from '../auth/auth-token.service';
import { roleGuard } from './role.guard';

function jwtWithRole(role: string): string {
  return `h.${btoa(JSON.stringify({ role }))}.s`;
}

function route(path: string): ActivatedRouteSnapshot {
  return { routeConfig: { path } } as unknown as ActivatedRouteSnapshot;
}

describe('roleGuard', () => {
  beforeEach(() => {
    sessionStorage.clear();
    TestBed.configureTestingModule({ providers: [provideRouter([])] });
  });

  afterEach(() => sessionStorage.clear());

  it('allows a teacher on the roll-call screen', () => {
    TestBed.inject(AuthTokenService).store({
      accessToken: jwtWithRole('TEACHER'),
      refreshToken: 'def',
      expiresIn: 900,
    });

    const result = TestBed.runInInjectionContext(() => roleGuard(route('attendance'), {} as never));

    expect(result).toBe(true);
  });

  it('sends a parent back home instead of roll-call', () => {
    TestBed.inject(AuthTokenService).store({
      accessToken: jwtWithRole('PARENT'),
      refreshToken: 'def',
      expiresIn: 900,
    });

    const result = TestBed.runInInjectionContext(() => roleGuard(route('attendance'), {} as never));

    expect(String(result)).toBe('/home');
  });

  it('sends a student back home instead of grade entry', () => {
    TestBed.inject(AuthTokenService).store({
      accessToken: jwtWithRole('STUDENT'),
      refreshToken: 'def',
      expiresIn: 900,
    });

    const result = TestBed.runInInjectionContext(() => roleGuard(route('exams'), {} as never));

    expect(String(result)).toBe('/home');
  });
});
