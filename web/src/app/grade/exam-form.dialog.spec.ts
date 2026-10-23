import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatDialogRef } from '@angular/material/dialog';
import { environment } from '../../environments/environment';
import { AuthTokenService } from '../auth/auth-token.service';
import { ExamFormDialog } from './exam-form.dialog';

/** ADR-030 : un enseignant ne crée une évaluation que pour un couple (classe, matière) qu'il enseigne. */
describe('ExamFormDialog', () => {
  let fixture: ComponentFixture<ExamFormDialog>;
  let httpMock: HttpTestingController;
  let role: string;

  type Page = {
    classes: () => { id: number }[];
    subjects: () => { id: number }[];
    form: { controls: { schoolClassId: { setValue: (v: number) => void }; subjectId: { value: number | null; setValue: (v: number) => void } } };
  };

  async function render(): Promise<Page> {
    await TestBed.configureTestingModule({
      imports: [ExamFormDialog],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: MatDialogRef, useValue: { close: () => undefined } },
        { provide: AuthTokenService, useValue: { role: () => role } },
      ],
    }).compileComponents();
    httpMock = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(ExamFormDialog);
    httpMock.expectOne((r) => r.url === `${environment.apiUrl}/classes`).flush({
      data: [
        { id: 1, name: '6e A', headTeacherId: null },
        { id: 2, name: '6e B', headTeacherId: null },
      ],
    });
    httpMock.expectOne((r) => r.url === `${environment.apiUrl}/subjects`).flush({
      data: [
        { id: 10, name: 'Maths' },
        { id: 11, name: 'Français' },
      ],
    });
    if (role === 'TEACHER') {
      httpMock.expectOne(`${environment.apiUrl}/me/teaching-scope`).flush({
        data: {
          restricted: true,
          classIds: [1, 2],
          headClassIds: [2],
          classSubjects: [{ classId: 1, subjectId: 10 }],
        },
      });
    }
    await fixture.whenStable();
    fixture.detectChanges();
    return fixture.componentInstance as unknown as Page;
  }

  afterEach(() => httpMock.verify());

  it('enseignant : ses classes enseignées, puis ses matières de la classe choisie', async () => {
    role = 'TEACHER';
    const page = await render();

    // 6e B : professeur principal sans matière, donc pas d'évaluation à y créer.
    expect(page.classes().map((c) => c.id)).toEqual([1]);
    expect(page.subjects()).toEqual([]);
    page.form.controls.schoolClassId.setValue(1);
    expect(page.subjects().map((s) => s.id)).toEqual([10]);
  });

  it('enseignant : une matière choisie pour une autre classe est retirée', async () => {
    role = 'TEACHER';
    const page = await render();
    page.form.controls.schoolClassId.setValue(1);
    page.form.controls.subjectId.setValue(10);

    page.form.controls.schoolClassId.setValue(2);

    expect(page.form.controls.subjectId.value).toBeNull();
  });

  it('direction : toutes les classes et matières', async () => {
    role = 'DIRECTION';
    const page = await render();

    expect(page.classes().length).toBe(2);
    expect(page.subjects().length).toBe(2);
  });
});
