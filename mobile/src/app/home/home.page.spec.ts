import { provideHttpClient } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { AuthTokenService } from '../auth/auth-token.service';
import { HomePage } from './home.page';

function jwtWithRole(role: string): string {
  return `h.${btoa(JSON.stringify({ role, email: `${role.toLowerCase()}@ecole.example` }))}.s`;
}

describe('HomePage', () => {
  let authTokenService: AuthTokenService;
  let router: Router;

  beforeEach(async () => {
    sessionStorage.clear();
    await TestBed.configureTestingModule({
      imports: [HomePage],
      providers: [provideHttpClient(), provideRouter([])],
    }).compileComponents();

    authTokenService = TestBed.inject(AuthTokenService);
    router = TestBed.inject(Router);
    spyOn(router, 'navigateByUrl').and.resolveTo(true);
  });

  afterEach(() => sessionStorage.clear());

  function render(role: string): ComponentFixture<HomePage> {
    authTokenService.store({
      accessToken: jwtWithRole(role),
      refreshToken: 'def',
      expiresIn: 900,
    });
    const fixture = TestBed.createComponent(HomePage);
    fixture.detectChanges();
    return fixture;
  }

  it('clears the session and navigates to /login on logout', async () => {
    const fixture = render('TEACHER');
    await fixture.componentInstance.logout();

    expect(authTokenService.read()).toBeNull();
    expect(router.navigateByUrl).toHaveBeenCalledWith('/login');
  });

  it('shows roll-call for a teacher', () => {
    const compiled = render('TEACHER').nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('Espace enseignant');
    expect(compiled.textContent).toContain("Feuille d'appel");
  });

  it('shows children follow-up for a parent, not roll-call', () => {
    const compiled = render('PARENT').nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('Espace parent');
    expect(compiled.textContent).toContain('Mes enfants');
    expect(compiled.textContent).not.toContain("Feuille d'appel");
  });

  it('shows a consultation home for a student', () => {
    const compiled = render('STUDENT').nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('Espace élève');
    expect(compiled.textContent).toContain('Mon suivi');
    expect(compiled.textContent).not.toContain("Feuille d'appel");
  });
});
