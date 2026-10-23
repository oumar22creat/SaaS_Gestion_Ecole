import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { environment } from '../../environments/environment';
import { TimetableEntry } from './timetable-entry.model';
import { TimetablePage } from './timetable.page';

/**
 * Emploi du temps mobile.
 *
 * Le piège corrigé ici : l'écran appelait GET /teachers pour les initiales de l'enseignant.
 * Ce référentiel est réservé à la direction ; pour un enseignant, le 403 faisait échouer tout
 * l'écran alors que ses créneaux étaient bien arrivés.
 */
describe('TimetablePage', () => {
  let fixture: ComponentFixture<TimetablePage>;
  let httpMock: HttpTestingController;

  const api = environment.apiUrl;

  /** Un créneau le lundi : l'écran s'ouvre sur le jour courant, le test choisit le lundi. */
  function creneau(overrides: Partial<TimetableEntry> = {}): TimetableEntry {
    return {
      id: 1,
      schoolClassId: 10,
      subjectId: 20,
      teacherId: 30,
      roomId: 40,
      dayOfWeek: 'MONDAY',
      startTime: '08:00:00',
      endTime: '09:00:00',
      ...overrides,
    };
  }

  const enrichi = creneau({
    schoolClassName: '6ème A',
    subjectName: 'Mathématiques',
    teacherName: 'Awa Traoré',
    roomName: 'Salle 3',
  });

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TimetablePage],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    }).compileComponents();
    httpMock = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(TimetablePage);
  });

  afterEach(() => httpMock.verify());

  /** Laisse les promesses se résoudre, passe au lundi et rend le texte affiché. */
  async function rendreLundi(): Promise<string> {
    // Un tour de boucle : les promesses des services (firstValueFrom) se résolvent après le flush.
    await new Promise((resolve) => setTimeout(resolve));
    await fixture.whenStable();
    (fixture.componentInstance as unknown as { jourChoisi: { set(v: string): void } }).jourChoisi.set('MONDAY');
    fixture.detectChanges();
    await fixture.whenStable();
    return (fixture.nativeElement as HTMLElement).textContent ?? '';
  }

  /** Répond à l'appel des créneaux, quels que soient ses paramètres de pagination. */
  function repondreCreneaux(entries: TimetableEntry[]): void {
    httpMock.expectOne((r) => r.url === `${api}/timetable-entries`).flush({ data: entries });
  }

  it('affiche les libellés joints par l’API sans appeler aucun référentiel, ni /teachers', async () => {
    repondreCreneaux([enrichi]);
    const texte = await rendreLundi();

    // Aucun appel annexe : ni /teachers (403 pour un enseignant), ni classes, matières, salles.
    httpMock.expectNone((r) => r.url.startsWith(`${api}/teachers`));
    httpMock.expectNone(`${api}/classes`);
    httpMock.expectNone(`${api}/subjects`);
    httpMock.expectNone(`${api}/rooms`);

    expect(texte).toContain('Mathématiques');
    expect(texte).toContain('(Salle 3)');
    expect(texte).toContain('6ème A');
    expect(texte).toContain('AT');
    expect(texte).not.toContain('Aucun cours');
    expect(fixture.nativeElement.querySelector('.flash-error')).toBeNull();
  });

  it('affiche quand même les créneaux si un référentiel de repli échoue (serveur antérieur)', async () => {
    // Réponse sans libellés, comme un serveur qui ne les joint pas encore.
    repondreCreneaux([creneau()]);
    await fixture.whenStable();

    httpMock.expectNone((r) => r.url.startsWith(`${api}/teachers`));
    httpMock
      .expectOne((r) => r.url === `${api}/classes`)
      .flush({ error: { code: 'FORBIDDEN' } }, { status: 403, statusText: 'Forbidden' });
    httpMock.expectOne((r) => r.url === `${api}/subjects`).flush({ data: [{ id: 20, name: 'Physique' }] });
    httpMock.expectOne((r) => r.url === `${api}/rooms`).error(new ProgressEvent('error'));

    const texte = await rendreLundi();

    // Les créneaux restent visibles, avec un libellé générique là où le repli a échoué.
    expect(texte).toContain('Physique');
    expect(texte).toContain('Classe #10');
    expect(texte).toContain('(Salle #40)');
    expect(texte).not.toContain('Aucun cours');
    expect(fixture.nativeElement.querySelector('.flash-error')).toBeNull();
    // Sans nom d'enseignant connu, pas de pastille « ? ».
    expect(fixture.nativeElement.querySelector('.carte-prof')).toBeNull();
  });

  it('signale une erreur quand ce sont les créneaux eux-mêmes qui échouent', async () => {
    httpMock
      .expectOne((r) => r.url === `${api}/timetable-entries`)
      .flush({ error: { code: 'INTERNAL', message: 'Panne' } }, { status: 500, statusText: 'Server Error' });
    await rendreLundi();

    expect(fixture.nativeElement.querySelector('.flash-error')).not.toBeNull();
  });

  it('propose en filtre les classes des créneaux, triées', async () => {
    repondreCreneaux([
      enrichi,
      creneau({
        id: 2,
        schoolClassId: 11,
        schoolClassName: '5ème B',
        subjectName: 'SVT',
        roomId: null,
        startTime: '10:00:00',
      }),
    ]);
    await rendreLundi();

    const puces = Array.from(fixture.nativeElement.querySelectorAll('.classe') as NodeListOf<HTMLElement>).map((b) =>
      b.textContent?.trim(),
    );
    expect(puces).toEqual(['Toutes', '5ème B', '6ème A']);
  });
});
