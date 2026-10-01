import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap } from '@angular/router';
import { environment } from '../../environments/environment';
import { PortalStudentRecordPage } from './student-record.page';

/**
 * Le suivi que lisent les familles.
 *
 * L'onglet « Absences » est le piège de cet écran : l'API sert l'assiduité complète, jour
 * présent compris, parce qu'un bulletin en a besoin. Rendue telle quelle, elle noyait
 * l'absence unique d'un trimestre sous cent quatre-vingts lignes « Présent », chacune portant
 * la mention « Non justifiée ».
 */
describe('PortalStudentRecordPage', () => {
  let fixture: ComponentFixture<PortalStudentRecordPage>;
  let httpMock: HttpTestingController;

  const assiduite = [
    { date: '2026-09-28', status: 'PRESENT', reason: null, justified: false },
    { date: '2026-09-29', status: 'ABSENT', reason: 'Malade', justified: true },
    { date: '2026-09-30', status: 'LATE', reason: null, justified: false },
    { date: '2026-10-01', status: 'PRESENT', reason: null, justified: false },
  ];

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PortalStudentRecordPage],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: convertToParamMap({ studentId: '9' }) } },
        },
      ],
    }).compileComponents();
    httpMock = TestBed.inject(HttpTestingController);

    fixture = TestBed.createComponent(PortalStudentRecordPage);
    const base = `${environment.apiUrl}/portal/students/9`;
    httpMock.expectOne(`${base}/grades`).flush({ data: [] });
    httpMock.expectOne((r) => r.url === `${base}/attendance`).flush({ data: assiduite });
    httpMock.expectOne(`${base}/timetable`).flush({ data: [] });
    httpMock.expectOne(`${base}/fees`).flush({ data: null });
    await fixture.whenStable();
    fixture.detectChanges();
  });

  afterEach(() => httpMock.verify());

  /** Bascule sur l'onglet Absences et rend le texte affiché. */
  async function ouvrirAbsences(): Promise<string> {
    (fixture.componentInstance as unknown as { tab: { set(v: string): void } }).tab.set('attendance');
    await fixture.whenStable();
    fixture.detectChanges();
    return (fixture.nativeElement as HTMLElement).textContent ?? '';
  }

  it('ne liste que les journées manquées', async () => {
    const texte = await ouvrirAbsences();

    expect(texte).toContain('Absence');
    expect(texte).toContain('Retard');
    expect(texte).not.toContain('Présent');
  });

  it('conserve le motif et la justification de l’absence', async () => {
    const texte = await ouvrirAbsences();

    expect(texte).toContain('Malade');
    expect(texte).toContain('Justifiée');
  });

  it('annonce une année sans absence plutôt qu’une liste de présences', async () => {
    httpMock.verify();
    TestBed.resetTestingModule();
    await TestBed.configureTestingModule({
      imports: [PortalStudentRecordPage],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: convertToParamMap({ studentId: '9' }) } },
        },
      ],
    }).compileComponents();
    const mock = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(PortalStudentRecordPage);
    const base = `${environment.apiUrl}/portal/students/9`;
    mock.expectOne(`${base}/grades`).flush({ data: [] });
    mock
      .expectOne((r) => r.url === `${base}/attendance`)
      .flush({ data: [{ date: '2026-10-01', status: 'PRESENT', reason: null, justified: false }] });
    mock.expectOne(`${base}/timetable`).flush({ data: [] });
    mock.expectOne(`${base}/fees`).flush({ data: null });
    await fixture.whenStable();
    fixture.detectChanges();

    expect(await ouvrirAbsences()).toContain('Aucune absence');
    mock.verify();
  });
});
