import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { environment } from '../../environments/environment';
import { TimetableListPage } from './timetable-list.page';

/**
 * La grille hebdomadaire ne lit aucune journée type : ses bandes sont déduites des créneaux
 * réellement posés, afin qu'une école qui commence à 7 h 30 et une autre à 8 h 00 voient
 * chacune la sienne. Ces tests fixent les deux déductions qui ne vont pas de soi : le creux
 * de la mi-journée, et la colonne du dimanche.
 */
describe('TimetableListPage', () => {
  let fixture: ComponentFixture<TimetableListPage>;
  let httpMock: HttpTestingController;

  function creneau(
    id: number,
    dayOfWeek: string,
    startTime: string,
    endTime: string,
    schoolClassId = 1,
  ) {
    return { id, schoolClassId, subjectId: 1, teacherId: 1, roomId: null, dayOfWeek, startTime, endTime };
  }

  /** Rend la page avec l'emploi du temps passé en argument et renvoie les bandes calculées. */
  async function render(entries: unknown[]): Promise<{ genre: string; debut: string; fin: string }[]> {
    fixture = TestBed.createComponent(TimetableListPage);
    httpMock.expectOne(`${environment.apiUrl}/timetable-entries`).flush({ data: entries });
    httpMock.expectOne((r) => r.url === `${environment.apiUrl}/classes`).flush({ data: [{ id: 1, name: '6e A' }] });
    httpMock.expectOne((r) => r.url === `${environment.apiUrl}/subjects`).flush({ data: [{ id: 1, name: 'Maths' }] });
    httpMock.expectOne((r) => r.url === `${environment.apiUrl}/teachers`).flush({ data: [] });
    httpMock.expectOne((r) => r.url === `${environment.apiUrl}/rooms`).flush({ data: [] });
    await fixture.whenStable();
    fixture.detectChanges();
    return (fixture.componentInstance as unknown as {
      bandes: () => { genre: string; debut: string; fin: string }[];
    }).bandes();
  }

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TimetableListPage],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    }).compileComponents();
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('ouvre une bande creuse sur le trou de la mi-journée', async () => {
    const bandes = await render([
      creneau(1, 'MONDAY', '08:00:00', '10:00:00'),
      creneau(2, 'MONDAY', '14:00:00', '15:30:00'),
    ]);

    expect(bandes.map((b) => b.genre)).toEqual(['cours', 'pause', 'cours']);
    // Le creux est celui des données : de la fin du dernier cours au début du suivant.
    expect(bandes[1]).toEqual(jasmine.objectContaining({ debut: '10:00', fin: '14:00' }));
  });

  it('laisse un intercours court sans bande', async () => {
    const bandes = await render([
      creneau(1, 'MONDAY', '08:00:00', '09:00:00'),
      creneau(2, 'MONDAY', '09:15:00', '10:00:00'),
    ]);

    // Quinze minutes : le temps de changer de salle, pas une interruption à signaler.
    expect(bandes.map((b) => b.genre)).toEqual(['cours', 'cours']);
  });

  it('mesure le creux depuis la fin la plus tardive, pas la dernière lue', async () => {
    const bandes = await render([
      creneau(1, 'MONDAY', '08:00:00', '12:00:00'),
      creneau(2, 'TUESDAY', '08:00:00', '09:00:00'),
      creneau(3, 'MONDAY', '12:15:00', '13:00:00'),
    ]);

    // 08:00–09:00 se termine tôt, mais 08:00–12:00 occupe encore l'établissement : aucun creux.
    expect(bandes.map((b) => b.genre)).toEqual(['cours', 'cours', 'cours']);
  });

  it('ne montre pas le dimanche tant qu’aucun cours ne s’y tient', async () => {
    await render([creneau(1, 'MONDAY', '08:00:00', '10:00:00')]);

    const jours = (fixture.componentInstance as unknown as { jours: () => { value: string }[] }).jours();
    expect(jours.map((j) => j.value)).toEqual([
      'MONDAY',
      'TUESDAY',
      'WEDNESDAY',
      'THURSDAY',
      'FRIDAY',
      'SATURDAY',
    ]);
  });

  it('ajoute la colonne du dimanche dès qu’un cours y est placé', async () => {
    await render([creneau(1, 'SUNDAY', '08:00:00', '10:00:00')]);

    const jours = (fixture.componentInstance as unknown as { jours: () => { value: string }[] }).jours();
    expect(jours.map((j) => j.value)).toContain('SUNDAY');
  });
});
