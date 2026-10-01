import { provideHttpClient } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { NavController } from '@ionic/angular';
import { AuthTokenService } from '../auth/auth-token.service';
import { HomePage } from './home.page';

function jwtWithRole(role: string): string {
  return `h.${btoa(JSON.stringify({ role, email: `${role.toLowerCase()}@ecole.example` }))}.s`;
}

describe('HomePage', () => {
  let authTokenService: AuthTokenService;
  let navController: NavController;

  beforeEach(async () => {
    localStorage.clear();
    await TestBed.configureTestingModule({
      imports: [HomePage],
      providers: [provideHttpClient(), provideRouter([])],
    }).compileComponents();

    authTokenService = TestBed.inject(AuthTokenService);
    navController = TestBed.inject(NavController);
    spyOn(navController, 'navigateRoot').and.resolveTo(true);
  });

  afterEach(() => localStorage.clear());

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

  it('clears the session and resets the navigation stack on logout', async () => {
    const fixture = render('TEACHER');
    await fixture.componentInstance.logout();

    expect(authTokenService.read()).toBeNull();
    // navigateRoot et non navigateByUrl : la pile d'Ionic doit être vidée, sans quoi les
    // écrans du compte précédent restent montés derrière l'écran de connexion.
    expect(navController.navigateRoot).toHaveBeenCalledWith('/login');
  });

  /**
   * Le cas constaté sur émulateur : `ion-router-outlet` garde l'accueil monté dans sa pile,
   * si bien qu'une reconnexion réutilise l'instance précédente. L'accueil présentait alors au
   * parent l'adresse de l'enseignant et ses tuiles de saisie, alors que le jeton stocké était
   * bien celui du parent. On réutilise ici volontairement la même instance.
   */
  it('follows the account change without being rebuilt', () => {
    const fixture = render('TEACHER');
    expect((fixture.nativeElement as HTMLElement).textContent).toContain("Feuille d'appel");

    authTokenService.store({ accessToken: jwtWithRole('PARENT'), refreshToken: 'def', expiresIn: 900 });
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('parent@ecole.example');
    expect(compiled.textContent).toContain('Espace parent');
    expect(compiled.textContent).not.toContain('teacher@ecole.example');
    expect(compiled.textContent).not.toContain("Feuille d'appel");
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
