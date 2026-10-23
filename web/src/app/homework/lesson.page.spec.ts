import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { environment } from '../../environments/environment';
import { AuthTokenService } from '../auth/auth-token.service';
import { LessonPage } from './lesson.page';

/**
 * ADR-030 : cahier de textes en lecture pour toute classe du périmètre, en écriture pour le
 * couple (classe, matière) enseigné.
 */
describe('LessonPage', () => {
  let fixture: ComponentFixture<LessonPage>;
  let httpMock: HttpTestingController;
  let role: string;

  type Page = {
    schoolClassId: number | null;
    subjectOptions: () => { id: number }[];
    lessons: { set: (l: unknown[]) => void };
  };

  function lesson(id: number, subjectId: number) {
    return { id, schoolClassId: 1, subjectId, sessionDate: '2026-10-01', content: `Séance ${id}`, homework: null, homeworkDueDate: null, attachmentDocumentId: null };
  }

  async function render(classSubjects: { classId: number; subjectId: number }[] = []): Promise<Page> {
    await TestBed.configureTestingModule({
      imports: [LessonPage],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: AuthTokenService, useValue: { role: () => role } },
      ],
    }).compileComponents();
    httpMock = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(LessonPage);
    httpMock.expectOne((r) => r.url === `${environment.apiUrl}/classes`).flush({ data: [{ id: 1, name: '6e A', headTeacherId: null }] });
    httpMock.expectOne((r) => r.url === `${environment.apiUrl}/subjects`).flush({
      data: [
        { id: 10, name: 'Maths' },
        { id: 11, name: 'Français' },
      ],
    });
    if (role === 'TEACHER') {
      httpMock.expectOne(`${environment.apiUrl}/me/teaching-scope`).flush({
        data: { restricted: true, classIds: [1], headClassIds: [], classSubjects },
      });
    }
    await fixture.whenStable();
    const page = fixture.componentInstance as unknown as Page;
    page.schoolClassId = 1;
    page.lessons.set([lesson(1, 10), lesson(2, 11)]);
    fixture.detectChanges();
    return page;
  }

  afterEach(() => httpMock.verify());

  it('enseignant : ses matières seulement, et suppression de ses seules séances', async () => {
    role = 'TEACHER';
    const page = await render([{ classId: 1, subjectId: 10 }]);

    expect(page.subjectOptions().map((s) => s.id)).toEqual([10]);
    const deletes = (fixture.nativeElement as HTMLElement).querySelectorAll('button[aria-label="Supprimer la séance"]');
    expect(deletes.length).toBe(1);
  });

  it('enseignant sans matière dans la classe : lecture seule expliquée', async () => {
    role = 'TEACHER';
    await render([]);
    const el = fixture.nativeElement as HTMLElement;

    expect(el.querySelector('[data-testid="no-subject-in-class"]')).not.toBeNull();
    expect(el.textContent).not.toContain('Ajouter la séance');
    expect(el.querySelectorAll('button[aria-label="Supprimer la séance"]').length).toBe(0);
  });

  it('direction : toutes les matières, toutes les séances supprimables', async () => {
    role = 'DIRECTION';
    const page = await render();

    expect(page.subjectOptions().length).toBe(2);
    expect((fixture.nativeElement as HTMLElement).querySelectorAll('button[aria-label="Supprimer la séance"]').length).toBe(2);
  });
});
