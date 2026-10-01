import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { IonButton, IonButtons, IonContent, IonHeader, IonIcon, IonTitle, IonToolbar } from '@ionic/angular';
import { extractErrorMessage } from '../core/http-error.util';
import { SchoolClass } from '../schoolclass/school-class.model';
import { SchoolClassService } from '../schoolclass/school-class.service';
import { Subject } from '../subject/subject.model';
import { SubjectService } from '../subject/subject.service';
import { Teacher } from '../teacher/teacher.model';
import { TeacherService } from '../teacher/teacher.service';
import { Room } from './room.model';
import { RoomService } from './room.service';
import { DayOfWeek, TimetableEntry } from './timetable-entry.model';
import { TimetableEntryService } from './timetable-entry.service';

/** Un jour tel que le présente le sélecteur : numéro de colonne et abrégé sur trois lettres. */
interface JourOnglet {
  value: DayOfWeek;
  numero: number;
  abrege: string;
}

/*
 * Lundi à samedi, dans cet ordre.
 *
 * Six colonnes et non sept : l'école malienne travaille le samedi matin et jamais le
 * dimanche. Un septième onglet toujours vide prendrait un sixième de la largeur sur un
 * téléphone. Le dimanche reste pourtant ajouté à la volée si l'emploi du temps en contient
 * un créneau — mieux vaut une colonne inattendue qu'un cours invisible.
 */
const JOURS_OUVRES: JourOnglet[] = [
  { value: 'MONDAY', numero: 1, abrege: 'LUN' },
  { value: 'TUESDAY', numero: 2, abrege: 'MAR' },
  { value: 'WEDNESDAY', numero: 3, abrege: 'MER' },
  { value: 'THURSDAY', numero: 4, abrege: 'JEU' },
  { value: 'FRIDAY', numero: 5, abrege: 'VEN' },
  { value: 'SATURDAY', numero: 6, abrege: 'SAM' },
];

const DIMANCHE: JourOnglet = { value: 'SUNDAY', numero: 7, abrege: 'DIM' };

/** Jour de la semaine d'aujourd'hui, pour ouvrir l'écran sur la journée en cours. */
function jourCourant(): DayOfWeek {
  const jours: DayOfWeek[] = [
    'SUNDAY',
    'MONDAY',
    'TUESDAY',
    'WEDNESDAY',
    'THURSDAY',
    'FRIDAY',
    'SATURDAY',
  ];
  return jours[new Date().getDay()];
}

/**
 * Emploi du temps consultable sur le terrain — docs/DESIGN.md §5 : lecture rapide
 * jour par jour, le créneau horaire étant l'information cherchée en premier.
 *
 * <p>Une journée à la fois, présentée en frise verticale. La semaine entière déroulée d'un
 * seul tenant demandait de faire défiler pour retrouver le cours suivant ; or on ouvre cet
 * écran entre deux sonneries, pour savoir où aller maintenant. Le jour du jour est donc
 * sélectionné d'office, et l'heure de début tient la colonne de gauche, alignée sur la frise.
 */
@Component({
  selector: 'app-timetable-page',
  imports: [RouterLink, IonHeader, IonToolbar, IonTitle, IonButtons, IonButton, IonContent, IonIcon],
  template: `
    <ion-header>
      <ion-toolbar>
        <ion-buttons slot="start">
          <ion-button routerLink="/home"><ion-icon aria-hidden="true" name="school-outline"></ion-icon> Accueil</ion-button>
        </ion-buttons>
        <ion-title>Emploi du temps</ion-title>
      </ion-toolbar>
    </ion-header>

    <!-- Bandeau des jours : prolonge la barre de navigation, d'où la couleur pleine. -->
    <div class="jours" role="tablist" aria-label="Jour de la semaine">
      @for (jour of jours(); track jour.value) {
        <button
          type="button"
          role="tab"
          class="jour"
          [class.jour-actif]="jour.value === jourChoisi()"
          [attr.aria-selected]="jour.value === jourChoisi()"
          (click)="jourChoisi.set(jour.value)"
        >
          <span class="jour-numero">{{ jour.numero }}</span>
          <span class="jour-abrege">{{ jour.abrege }}</span>
        </button>
      }
    </div>

    <ion-content>
      @if (classes().length > 0) {
        <div class="classes" role="tablist" aria-label="Classe">
          <button
            type="button"
            role="tab"
            class="classe"
            [class.classe-active]="classeChoisie() === null"
            [attr.aria-selected]="classeChoisie() === null"
            (click)="classeChoisie.set(null)"
          >
            Toutes
          </button>
          @for (classe of classes(); track classe.id) {
            <button
              type="button"
              role="tab"
              class="classe"
              [class.classe-active]="classeChoisie() === classe.id"
              [attr.aria-selected]="classeChoisie() === classe.id"
              (click)="classeChoisie.set(classe.id)"
            >
              {{ classe.name }}
            </button>
          }
        </div>
      }

      <div class="corps">
        @if (errorMessage()) {
          <p class="flash-error">{{ errorMessage() }}</p>
        }

        @if (creneaux().length > 0) {
          <div class="entetes" aria-hidden="true">
            <span class="entete-heure">Heure</span>
            <span class="entete-matiere">Matière</span>
          </div>

          <ol class="frise">
            @for (creneau of creneaux(); track creneau.id) {
              <li class="rangee">
                <span class="rangee-heure">{{ hourMinute(creneau.startTime) }}</span>
                <span class="rangee-puce" aria-hidden="true"></span>
                <article class="carte">
                  <p class="carte-titre">
                    {{ subjectName(creneau.subjectId) }}
                    @if (roomName(creneau.roomId); as salle) {
                      <span class="carte-salle">({{ salle }})</span>
                    }
                  </p>
                  <p class="carte-horaire">
                    <ion-icon aria-hidden="true" name="time-outline"></ion-icon>
                    {{ hourMinute(creneau.startTime) }} – {{ hourMinute(creneau.endTime) }}
                    @if (classeChoisie() === null) {
                      <span class="carte-classe">· {{ className(creneau.schoolClassId) }}</span>
                    }
                  </p>
                  <span class="carte-prof" [attr.title]="teacherName(creneau.teacherId)">
                    {{ teacherInitials(creneau.teacherId) }}
                  </span>
                </article>
              </li>
            }
          </ol>
        } @else {
          <p class="empty-state">Aucun cours {{ libelleJourChoisi() }}.</p>
        }
      </div>
    </ion-content>
  `,
  styles: `
    .jours {
      display: flex;
      gap: var(--space-2);
      padding: 0 var(--space-3) var(--space-3);
      background: var(--color-nav);
    }
    .jour {
      display: flex;
      flex: 1 1 0;
      flex-direction: column;
      align-items: center;
      gap: 2px;
      padding: var(--space-2) 0;
      border: 0;
      border-radius: var(--radius-sm);
      background: transparent;
      color: var(--color-nav-muted);
      font-family: inherit;
      cursor: pointer;
    }
    .jour-numero {
      font-family: var(--font-family-display);
      font-size: var(--font-size-title-3);
      font-weight: var(--font-weight-semibold);
      font-variant-numeric: tabular-nums;
    }
    .jour-abrege {
      font-size: 11px;
      letter-spacing: 0.06em;
    }
    /* Le jour retenu s'inverse : pastille claire sur le bandeau sombre. */
    .jour-actif {
      background: var(--color-surface);
      color: var(--tenant-primary);
      box-shadow: var(--shadow-1);
    }

    .classes {
      display: flex;
      gap: var(--space-2);
      padding: var(--space-4) var(--space-4) 0;
      overflow-x: auto;
      /* Le rail défile horizontalement quand les classes débordent, sans barre visible. */
      scrollbar-width: none;
    }
    .classes::-webkit-scrollbar {
      display: none;
    }
    .classe {
      flex: 0 0 auto;
      padding: var(--space-2) var(--space-4);
      border: 1px solid var(--color-border);
      border-radius: var(--radius-pill);
      background: var(--color-surface);
      color: var(--color-text-secondary);
      font-family: inherit;
      font-size: var(--font-size-small);
      font-weight: var(--font-weight-medium);
      white-space: nowrap;
      cursor: pointer;
    }
    .classe-active {
      border-color: transparent;
      background: var(--tenant-primary);
      color: #ffffff;
    }

    .corps {
      padding: var(--space-4);
    }
    .entetes {
      display: grid;
      grid-template-columns: 56px 12px 1fr;
      gap: var(--space-3);
      padding-bottom: var(--space-2);
      color: var(--color-text-secondary);
      font-size: var(--font-size-caption);
      font-weight: var(--font-weight-semibold);
      letter-spacing: 0.04em;
      text-transform: uppercase;
    }
    .entete-matiere {
      grid-column: 3;
    }

    .frise {
      margin: 0;
      padding: 0;
      list-style: none;
    }
    /*
     * Trois colonnes : l'heure, le trait de la frise, la carte. La puce est posée sur une
     * colonne à elle plutôt qu'en pseudo-élément de la carte, pour que le trait reste droit
     * quelle que soit la hauteur de la carte (un intitulé long passe sur deux lignes).
     */
    .rangee {
      display: grid;
      grid-template-columns: 56px 12px 1fr;
      align-items: stretch;
      gap: var(--space-3);
    }
    .rangee-heure {
      padding-top: var(--space-3);
      font-family: var(--font-family-display);
      font-size: var(--font-size-small);
      font-weight: var(--font-weight-semibold);
      font-variant-numeric: tabular-nums;
    }
    .rangee-puce {
      position: relative;
      align-self: stretch;
      justify-self: center;
      width: 2px;
      background: var(--tenant-primary-ring);
    }
    .rangee-puce::before {
      content: '';
      position: absolute;
      top: var(--space-4);
      left: 50%;
      width: 11px;
      height: 11px;
      transform: translateX(-50%);
      border-radius: 50%;
      background: var(--tenant-primary);
    }
    /* Le trait s'arrête au dernier créneau : prolongé, il annoncerait une suite. */
    .rangee:last-child .rangee-puce {
      align-self: start;
      height: calc(var(--space-4) + 11px);
    }

    /*
     * La marge basse appartient à la carte, pas à la rangée : la colonne du trait occupe
     * ainsi toute la hauteur de la rangée et la frise reste continue d'un créneau au suivant.
     */
    .carte {
      position: relative;
      margin-bottom: var(--space-3);
      padding: var(--space-3) var(--space-12) var(--space-3) var(--space-4);
      border-radius: var(--radius);
      background: var(--color-surface);
      box-shadow: var(--shadow-1);
    }
    .carte-titre {
      margin: 0;
      font-weight: var(--font-weight-semibold);
    }
    .carte-salle {
      color: var(--color-text-secondary);
      font-weight: var(--font-weight-medium);
    }
    .carte-horaire {
      display: flex;
      align-items: center;
      gap: var(--space-1);
      margin: var(--space-1) 0 0;
      color: var(--color-text-secondary);
      font-size: var(--font-size-caption);
    }
    .carte-classe {
      white-space: nowrap;
    }
    /*
     * Initiales plutôt qu'une photo : l'application ne stocke aucun portrait, et une pastille
     * vide ne dirait rien. Le nom complet reste accessible par le titre.
     */
    .carte-prof {
      position: absolute;
      top: 50%;
      right: var(--space-3);
      display: flex;
      width: 34px;
      height: 34px;
      align-items: center;
      justify-content: center;
      transform: translateY(-50%);
      border-radius: 50%;
      background: var(--tenant-primary-soft);
      color: var(--tenant-primary);
      font-size: var(--font-size-caption);
      font-weight: var(--font-weight-semibold);
    }
  `,
})
export class TimetablePage {
  private readonly timetableService = inject(TimetableEntryService);
  private readonly schoolClassService = inject(SchoolClassService);
  private readonly subjectService = inject(SubjectService);
  private readonly roomService = inject(RoomService);
  private readonly teacherService = inject(TeacherService);

  protected readonly classes = signal<SchoolClass[]>([]);
  protected readonly entries = signal<TimetableEntry[]>([]);
  protected readonly subjects = signal<Subject[]>([]);
  protected readonly rooms = signal<Room[]>([]);
  protected readonly teachers = signal<Teacher[]>([]);
  protected readonly errorMessage = signal<string | null>(null);

  protected readonly jourChoisi = signal<DayOfWeek>(jourCourant());
  protected readonly classeChoisie = signal<number | null>(null);

  /** Les six jours ouvrés, plus le dimanche si et seulement s'il porte un créneau. */
  protected readonly jours = computed<JourOnglet[]>(() =>
    this.entries().some((entry) => entry.dayOfWeek === 'SUNDAY')
      ? [...JOURS_OUVRES, DIMANCHE]
      : JOURS_OUVRES,
  );

  /** Les créneaux du jour et de la classe retenus, du plus matinal au plus tardif. */
  protected readonly creneaux = computed(() =>
    this.entries()
      .filter(
        (entry) =>
          entry.dayOfWeek === this.jourChoisi() &&
          (this.classeChoisie() === null || entry.schoolClassId === this.classeChoisie()),
      )
      .sort((a, b) => a.startTime.localeCompare(b.startTime)),
  );

  private readonly subjectsById = computed(
    () => new Map(this.subjects().map((subject) => [subject.id, subject.name])),
  );
  private readonly roomsById = computed(() => new Map(this.rooms().map((room) => [room.id, room.name])));
  private readonly classesById = computed(
    () => new Map(this.classes().map((schoolClass) => [schoolClass.id, schoolClass.name])),
  );
  private readonly teachersById = computed(() => new Map(this.teachers().map((teacher) => [teacher.id, teacher])));

  constructor() {
    // Un seul catch pour les cinq chargements : sans lui, un échec (session expirée, réseau
    // coupé, droits refusés) affichait « Aucun cours », ce qui se lit comme une journée libre
    // alors que rien n'a pu être chargé.
    void Promise.all([
      this.schoolClassService.list().then((classes) => this.classes.set(classes)),
      this.timetableService.list().then((entries) => this.entries.set(entries)),
      this.subjectService.list().then((subjects) => this.subjects.set(subjects)),
      this.roomService.list().then((rooms) => this.rooms.set(rooms)),
      this.teacherService.list().then((teachers) => this.teachers.set(teachers)),
    ]).catch((error) => this.errorMessage.set(extractErrorMessage(error)));
  }

  /** « lundi », « ce mardi »… tel que l'état vide l'emploie : « Aucun cours le lundi. » */
  protected libelleJourChoisi(): string {
    const abreges: Record<DayOfWeek, string> = {
      MONDAY: 'le lundi',
      TUESDAY: 'le mardi',
      WEDNESDAY: 'le mercredi',
      THURSDAY: 'le jeudi',
      FRIDAY: 'le vendredi',
      SATURDAY: 'le samedi',
      SUNDAY: 'le dimanche',
    };
    return abreges[this.jourChoisi()];
  }

  protected subjectName(subjectId: number): string {
    return this.subjectsById().get(subjectId) ?? `Matière #${subjectId}`;
  }

  protected className(schoolClassId: number): string {
    return this.classesById().get(schoolClassId) ?? `Classe #${schoolClassId}`;
  }

  protected teacherName(teacherId: number): string {
    const teacher = this.teachersById().get(teacherId);
    return teacher ? `${teacher.firstName} ${teacher.lastName}` : `Enseignant #${teacherId}`;
  }

  /** Deux lettres au plus : la pastille fait 34 px et doit rester lisible. */
  protected teacherInitials(teacherId: number): string {
    const teacher = this.teachersById().get(teacherId);
    if (!teacher) {
      return '?';
    }
    return `${teacher.firstName.charAt(0)}${teacher.lastName.charAt(0)}`.toUpperCase();
  }

  /**
   * Rend {@code null} quand aucune salle n'est rattachée au créneau, afin que la mention
   * disparaisse au lieu d'afficher « Salle #null ». Sur une carte étroite, un créneau sans
   * salle se lit mieux sans la parenthèse qu'avec un tiret, contrairement au tableau du web
   * où la colonne doit rester alignée.
   */
  protected roomName(roomId: number | null): string | null {
    if (roomId === null) {
      return null;
    }
    return this.roomsById().get(roomId) ?? `Salle #${roomId}`;
  }

  /**
   * L'API renvoie l'heure en HH:mm:ss. Les secondes n'apportent rien sur un emploi du temps et
   * faisaient déborder la colonne horaire, large de 56 px, sur deux lignes.
   */
  protected hourMinute(time: string): string {
    return time.slice(0, 5);
  }
}
