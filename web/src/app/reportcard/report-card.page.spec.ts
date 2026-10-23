import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { environment } from '../../environments/environment';
import { AuthTokenService } from '../auth/auth-token.service';
import { ReportCardPage } from './report-card.page';

/**
 * ADR-030 : génération, appréciation générale et décision du conseil réservées à ADMIN,
 * DIRECTION et au professeur principal de la classe. Les autres enseignants lisent.
 */
describe('ReportCardPage', () => {
  let fixture: ComponentFixture<ReportCardPage>;
  let httpMock: HttpTestingController;
  let role: string;

  const card = {
    id: 9,
    studentId: 1,
    schoolClassId: 1,
    periodLabel: 'Trimestre 1',
    periodFrom: '2026-09-01',
    periodTo: '2026-12-15',
    generalAverage: 12,
    generalComment: 'Bon trimestre',
    councilDecision: null,
    absenceCount: 0,
    lateCount: 0,
    entries: [],
    rankInClass: 1,
    classSize: 1,
  };

  async function render(scope?: { headClassIds: number[] }): Promise<HTMLElement> {
    await TestBed.configureTestingModule({
      imports: [ReportCardPage],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: AuthTokenService, useValue: { role: () => role } },
      ],
    }).compileComponents();
    httpMock = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(ReportCardPage);
    httpMock.expectOne((r) => r.url === `${environment.apiUrl}/classes`).flush({ data: [{ id: 1, name: '6e A', headTeacherId: 3 }] });
    httpMock.expectOne((r) => r.url === `${environment.apiUrl}/students`).flush({ data: [] });
    if (scope) {
      httpMock.expectOne(`${environment.apiUrl}/me/teaching-scope`).flush({
        data: { restricted: true, classIds: [1], headClassIds: scope.headClassIds, classSubjects: [] },
      });
    }
    await fixture.whenStable();
    const page = fixture.componentInstance as unknown as {
      schoolClassId: number | null;
      cards: { set: (cards: unknown[]) => void };
    };
    page.schoolClassId = 1;
    page.cards.set([card]);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    return fixture.nativeElement as HTMLElement;
  }

  afterEach(() => httpMock.verify());

  it('enseignant non principal : ni génération ni appréciation générale éditable', async () => {
    role = 'TEACHER';
    const el = await render({ headClassIds: [] });

    expect(el.querySelector('[data-testid="generate"]')).toBeNull();
    expect(el.querySelector('[data-testid="save-comment"]')).toBeNull();
    expect(el.querySelector('input[aria-label="Appréciation générale"]')).toBeNull();
    expect(el.textContent).toContain('Bon trimestre');
    expect(el.querySelector('[data-testid="head-teacher-only"]')).not.toBeNull();
  });

  it('professeur principal de la classe : régénère et saisit l’appréciation générale', async () => {
    role = 'TEACHER';
    const el = await render({ headClassIds: [1] });

    expect(el.querySelector('[data-testid="generate"]')?.textContent).toContain('Régénérer');
    expect(el.querySelector('[data-testid="save-comment"]')).not.toBeNull();
    expect(el.querySelector('input[aria-label="Appréciation générale"]')).not.toBeNull();
  });

  it('professeur principal d’une AUTRE classe : lecture seule ici', async () => {
    role = 'TEACHER';
    const el = await render({ headClassIds: [2] });

    expect(el.querySelector('[data-testid="generate"]')).toBeNull();
  });

  it('direction : tout est proposé, sans appel au périmètre', async () => {
    role = 'DIRECTION';
    const el = await render();

    expect(el.querySelector('[data-testid="generate"]')).not.toBeNull();
    expect(el.querySelector('[data-testid="save-comment"]')).not.toBeNull();
  });
});
