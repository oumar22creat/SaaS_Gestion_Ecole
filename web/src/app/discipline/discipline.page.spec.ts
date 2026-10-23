import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { environment } from '../../environments/environment';
import { AuthTokenService } from '../auth/auth-token.service';
import { DisciplinePage } from './discipline.page';

/** ADR-030 : un enseignant ne déclare un incident que pour des élèves de la classe choisie. */
describe('DisciplinePage', () => {
  let fixture: ComponentFixture<DisciplinePage>;
  let httpMock: HttpTestingController;
  let role: string;

  type Page = { schoolClassId: number | null; studentOptions: () => { id: number }[] };

  function student(id: number, schoolClassId: number) {
    return { id, studentNumber: `E${id}`, firstName: 'A', lastName: `B${id}`, birthDate: null, gender: null, schoolClassId, active: true, portalEmail: null, hasPhoto: false };
  }

  async function render(): Promise<Page> {
    await TestBed.configureTestingModule({
      imports: [DisciplinePage],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: AuthTokenService, useValue: { role: () => role } },
      ],
    }).compileComponents();
    httpMock = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(DisciplinePage);
    httpMock.expectOne((r) => r.url === `${environment.apiUrl}/classes`).flush({ data: [] });
    httpMock.expectOne((r) => r.url === `${environment.apiUrl}/students`).flush({ data: [student(1, 1), student(2, 2)] });
    if (role === 'TEACHER') {
      httpMock.expectOne(`${environment.apiUrl}/me/teaching-scope`).flush({
        data: { restricted: true, classIds: [1, 2], headClassIds: [], classSubjects: [] },
      });
    }
    await fixture.whenStable();
    return fixture.componentInstance as unknown as Page;
  }

  afterEach(() => httpMock.verify());

  it('enseignant : seuls les élèves de la classe choisie sont proposés', async () => {
    role = 'TEACHER';
    const page = await render();

    expect(page.studentOptions()).toEqual([]);
    page.schoolClassId = 1;
    expect(page.studentOptions().map((s) => s.id)).toEqual([1]);
  });

  it('vie scolaire : liste complète, inchangée', async () => {
    role = 'VIE_SCOLAIRE';
    const page = await render();

    expect(page.studentOptions().length).toBe(2);
  });
});
