export type DayOfWeek = 'MONDAY' | 'TUESDAY' | 'WEDNESDAY' | 'THURSDAY' | 'FRIDAY' | 'SATURDAY' | 'SUNDAY';

export const DAYS_OF_WEEK: { value: DayOfWeek; label: string }[] = [
  { value: 'MONDAY', label: 'Lundi' },
  { value: 'TUESDAY', label: 'Mardi' },
  { value: 'WEDNESDAY', label: 'Mercredi' },
  { value: 'THURSDAY', label: 'Jeudi' },
  { value: 'FRIDAY', label: 'Vendredi' },
  { value: 'SATURDAY', label: 'Samedi' },
  { value: 'SUNDAY', label: 'Dimanche' },
];

/*
 * Les libellés (`schoolClassName`, `subjectName`, `teacherName`, `roomName`) sont joints par
 * l'API. Ils sont facultatifs ici : absents d'un serveur antérieur à leur ajout, ou d'une fiche
 * introuvable. L'écran retombe alors sur les référentiels, puis sur un libellé générique.
 */
export interface TimetableEntry {
  id: number;
  schoolClassId: number;
  schoolClassName?: string | null;
  subjectId: number;
  subjectName?: string | null;
  teacherId: number;
  /** Prénom et nom seulement — l'API n'expose ni e-mail ni téléphone de l'enseignant. */
  teacherName?: string | null;
  /** Facultative : nulle lorsque la classe occupe toujours la même salle (migration V64). */
  roomId: number | null;
  roomName?: string | null;
  dayOfWeek: DayOfWeek;
  startTime: string;
  endTime: string;
}
