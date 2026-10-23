import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { environment } from '../../environments/environment';
import { AuthTokenService } from '../auth/auth-token.service';
import { TeachingScope, TeachingScopeService } from './teaching-scope.service';

/** Masquages par rôle (ADR-030) : seule la donnée d'affichage est testée ici, le serveur reste juge. */
describe('TeachingScopeService', () => {
  let httpMock: HttpTestingController;
  let role: string | null;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: AuthTokenService, useValue: { role: () => role } },
      ],
    });
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('n’appelle pas l’API hors enseignant : direction non restreinte', async () => {
    role = 'DIRECTION';
    const scope = await TestBed.inject(TeachingScopeService).current();

    expect(scope.restricted).toBeFalse();
    expect(scope.canManageReportCards(42)).toBeTrue();
    expect(scope.teaches(42, 7)).toBeTrue();
  });

  it('refuse la gestion des bulletins au secrétariat, sans le restreindre ailleurs', async () => {
    role = 'SECRETARY';
    const scope = await TestBed.inject(TeachingScopeService).current();

    expect(scope.canManageReportCards(42)).toBeFalse();
  });

  it('lit le périmètre de l’enseignant connecté', async () => {
    role = 'TEACHER';
    const pending = TestBed.inject(TeachingScopeService).current();
    httpMock.expectOne(`${environment.apiUrl}/me/teaching-scope`).flush({
      data: {
        restricted: true,
        classIds: [1, 2],
        headClassIds: [2],
        classSubjects: [{ classId: 1, subjectId: 10 }],
      },
    });
    const scope = await pending;

    expect(scope.canManageReportCards(2)).toBeTrue();
    expect(scope.canManageReportCards(1)).toBeFalse();
    expect(scope.teaches(1, 10)).toBeTrue();
    expect(scope.teaches(1, 11)).toBeFalse();
    expect(scope.teaches(2, 10)).toBeFalse();
    expect(scope.subjectsFor(1, [{ id: 10 }, { id: 11 }])).toEqual([{ id: 10 }]);
    expect(scope.classesWithSubjects([{ id: 1 }, { id: 2 }])).toEqual([{ id: 1 }]);
  });

  it('masque tout si le périmètre ne se charge pas (fermé par défaut)', async () => {
    role = 'TEACHER';
    const pending = TestBed.inject(TeachingScopeService).current();
    httpMock
      .expectOne(`${environment.apiUrl}/me/teaching-scope`)
      .flush({}, { status: 500, statusText: 'Server Error' });
    const scope = await pending;

    expect(scope.canManageReportCards(1)).toBeFalse();
    expect(scope.teaches(1, 10)).toBeFalse();
  });

  it('un périmètre fermé ne propose aucune matière sans classe choisie', () => {
    const scope = new TeachingScope('TEACHER', true, [], [{ classId: 1, subjectId: 10 }]);
    expect(scope.subjectsFor(null, [{ id: 10 }])).toEqual([]);
  });
});
