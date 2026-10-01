import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatSelectModule } from '@angular/material/select';
import { confirmAction } from '../core/confirm-dialog.component';
import { SchoolClass } from '../schoolclass/school-class.model';
import { SchoolClassService } from '../schoolclass/school-class.service';
import { Subject } from '../subject/subject.model';
import { SubjectService } from '../subject/subject.service';
import { Teacher } from '../teacher/teacher.model';
import { TeacherService } from '../teacher/teacher.service';
import { DayOfWeek, TimetableEntry } from './timetable-entry.model';
import { TimetableEntryFormDialog } from './timetable-entry-form.dialog';
import { TimetableEntryService } from './timetable-entry.service';
import { Room } from './room.model';
import { RoomService } from './room.service';

/** Une colonne de la grille. */
interface ColonneJour {
  value: DayOfWeek;
  label: string;
}

/*
 * Lundi à samedi. Le dimanche n'est pas une colonne par défaut — six colonnes laissent déjà
 * peu de largeur à chacune — mais il s'ajoute dès qu'un créneau y est posé : mieux vaut une
 * colonne inattendue qu'un cours invisible.
 */
const JOURS_OUVRES: ColonneJour[] = [
  { value: 'MONDAY', label: 'Lundi' },
  { value: 'TUESDAY', label: 'Mardi' },
  { value: 'WEDNESDAY', label: 'Mercredi' },
  { value: 'THURSDAY', label: 'Jeudi' },
  { value: 'FRIDAY', label: 'Vendredi' },
  { value: 'SATURDAY', label: 'Samedi' },
];

const DIMANCHE: ColonneJour = { value: 'SUNDAY', label: 'Dimanche' };

/** Une bande de cours : un horaire, et ce que chaque jour y place. */
interface BandeCours {
  genre: 'cours';
  debut: string;
  fin: string;
  parJour: Map<DayOfWeek, TimetableEntry[]>;
}

/** Une bande creuse : un intervalle qu'aucun cours n'occupe, quel que soit le jour. */
interface BandePause {
  genre: 'pause';
  debut: string;
  fin: string;
}

type BandeGrille = BandeCours | BandePause;

/**
 * Durée en deçà de laquelle un trou entre deux bandes reste un simple intercours.
 *
 * <p>Trente minutes : en dessous, c'est le temps de changer de salle, et une bande de plus
 * hacherait la grille sans rien apprendre. Au-delà, l'établissement a réellement ménagé une
 * interruption, et la lire d'un coup d'œil aide à placer les créneaux suivants.
 */
const PAUSE_MINIMALE_MINUTES = 30;

/** « 08:30 » → 510. Les horaires de l'API sont en HH:mm:ss, comparables tels quels. */
function enMinutes(heure: string): number {
  const [h, m] = heure.slice(0, 5).split(':');
  return Number(h) * 60 + Number(m);
}

@Component({
  selector: 'app-timetable-list-page',
  imports: [
    MatButtonModule,
    MatIconModule,
    MatDialogModule,
    MatFormFieldModule,
    MatSelectModule,
    RouterLink,
  ],
  templateUrl: './timetable-list.page.html',
  styleUrl: './timetable-list.page.scss',
})
export class TimetableListPage {
  private readonly timetableEntryService = inject(TimetableEntryService);
  private readonly dialog = inject(MatDialog);
  private readonly schoolClassService = inject(SchoolClassService);
  private readonly subjectService = inject(SubjectService);
  private readonly teacherService = inject(TeacherService);
  private readonly roomService = inject(RoomService);

  protected readonly entries = signal<TimetableEntry[]>([]);
  protected readonly classes = signal<SchoolClass[]>([]);
  protected readonly subjects = signal<Subject[]>([]);
  protected readonly teachers = signal<Teacher[]>([]);
  protected readonly rooms = signal<Room[]>([]);
  protected readonly loading = signal(false);

  /** {@code null} : toutes les classes à la fois, plusieurs cartes pouvant alors se suivre. */
  protected readonly classeChoisie = signal<number | null>(null);

  protected readonly jours = computed<ColonneJour[]>(() =>
    this.entries().some((entry) => entry.dayOfWeek === 'SUNDAY')
      ? [...JOURS_OUVRES, DIMANCHE]
      : JOURS_OUVRES,
  );

  private readonly creneauxVisibles = computed(() =>
    this.classeChoisie() === null
      ? this.entries()
      : this.entries().filter((entry) => entry.schoolClassId === this.classeChoisie()),
  );

  /**
   * Les bandes de la grille, déduites des créneaux réellement posés.
   *
   * <p>Aucune journée type n'est codée en dur : une école qui commence à 7 h 30 et une autre
   * à 8 h 00 lisent toutes deux leur propre grille. Les bandes sont donc les horaires
   * distincts rencontrés, dans l'ordre, et un trou commun à tous les jours devient une bande
   * creuse — c'est un constat sur les données, non une pause déclarée quelque part.
   */
  protected readonly bandes = computed<BandeGrille[]>(() => {
    const parHoraire = new Map<string, BandeCours>();
    for (const entry of this.creneauxVisibles()) {
      const debut = entry.startTime.slice(0, 5);
      const fin = entry.endTime.slice(0, 5);
      const cle = `${debut}-${fin}`;
      let bande = parHoraire.get(cle);
      if (!bande) {
        bande = { genre: 'cours', debut, fin, parJour: new Map() };
        parHoraire.set(cle, bande);
      }
      const duJour = bande.parJour.get(entry.dayOfWeek) ?? [];
      duJour.push(entry);
      bande.parJour.set(entry.dayOfWeek, duJour);
    }

    const triees = [...parHoraire.values()].sort(
      (a, b) => enMinutes(a.debut) - enMinutes(b.debut) || enMinutes(a.fin) - enMinutes(b.fin),
    );

    const grille: BandeGrille[] = [];
    // Fin la plus tardive rencontrée jusqu'ici : deux classes peuvent se chevaucher, et c'est
    // la dernière à libérer l'horaire qui détermine le début du trou.
    let finMax = 0;
    for (const bande of triees) {
      if (grille.length > 0 && enMinutes(bande.debut) - finMax >= PAUSE_MINIMALE_MINUTES) {
        grille.push({
          genre: 'pause',
          debut: `${String(Math.floor(finMax / 60)).padStart(2, '0')}:${String(finMax % 60).padStart(2, '0')}`,
          fin: bande.debut,
        });
      }
      grille.push(bande);
      finMax = Math.max(finMax, enMinutes(bande.fin));
    }
    return grille;
  });

  /** Gabarit de colonnes : la colonne des horaires, puis un jour par colonne. */
  protected readonly colonnesCss = computed(
    () => `120px repeat(${this.jours().length}, minmax(140px, 1fr))`,
  );

  constructor() {
    void this.refresh();
    void this.schoolClassService.list().then((classes) => {
      this.classes.set(classes);
      /*
       * On ouvre sur une classe, pas sur l'établissement entier : une grille hebdomadaire se
       * lit classe par classe, une case ne tenant qu'un cours à la fois. Toutes classes
       * confondues, deux horaires qui se chevauchent produisent deux bandes distinctes et la
       * grille se hache. « Toutes les classes » reste à un clic pour repérer un conflit.
       */
      if (this.classeChoisie() === null && classes.length > 0) {
        this.classeChoisie.set(classes[0].id);
      }
    });
    void this.subjectService.list().then((subjects) => this.subjects.set(subjects));
    void this.teacherService.list().then((teachers) => this.teachers.set(teachers));
    void this.roomService.list().then((rooms) => this.rooms.set(rooms));
  }

  async refresh(): Promise<void> {
    this.loading.set(true);
    try {
      this.entries.set(await this.timetableEntryService.list());
    } finally {
      this.loading.set(false);
    }
  }

  protected creneauxDe(bande: BandeCours, jour: DayOfWeek): TimetableEntry[] {
    return bande.parJour.get(jour) ?? [];
  }

  /**
   * Teinte de la carte, attachée à la matière.
   *
   * <p>La couleur n'ajoute aucune information : elle sert à repérer d'un coup d'œil qu'une
   * même matière revient trois fois dans la semaine. Elle est tirée de l'identifiant pour
   * rester la même d'une consultation à l'autre, et reste volontairement pâle — le texte doit
   * passer le contraste, et six matières criardes côte à côte deviennent illisibles.
   */
  protected teinte(subjectId: number): number {
    return subjectId % 6;
  }

  className(id: number): string {
    return this.classes().find((c) => c.id === id)?.name ?? `#${id}`;
  }

  subjectName(id: number): string {
    return this.subjects().find((s) => s.id === id)?.name ?? `#${id}`;
  }

  teacherName(id: number): string {
    const teacher = this.teachers().find((t) => t.id === id);
    return teacher ? `${teacher.firstName} ${teacher.lastName}` : `#${id}`;
  }

  /** Le tiret cadratin plutôt qu'un vide : la ligne reste lisible, l'absence est explicite. */
  roomName(id: number | null): string {
    if (id === null) {
      return '—';
    }
    return this.rooms().find((r) => r.id === id)?.name ?? `#${id}`;
  }

  openCreateDialog(): void {
    this.dialog
      .open(TimetableEntryFormDialog, { data: {}, width: '480px' })
      .afterClosed()
      .subscribe((result) => result && void this.refresh());
  }

  openEditDialog(entry: TimetableEntry): void {
    this.dialog
      .open(TimetableEntryFormDialog, { data: { entry }, width: '480px' })
      .afterClosed()
      .subscribe((result) => result && void this.refresh());
  }

  async remove(entry: TimetableEntry): Promise<void> {
    const confirmed = await confirmAction(this.dialog, {
      title: 'Supprimer ce créneau ?',
      message: `${this.subjectName(entry.subjectId)} — ${this.className(entry.schoolClassId)} sera retiré de l'emploi du temps.`,
    });
    if (!confirmed) {
      return;
    }
    await this.timetableEntryService.remove(entry.id);
    await this.refresh();
  }
}
