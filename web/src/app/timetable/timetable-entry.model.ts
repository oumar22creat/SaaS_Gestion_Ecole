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

export interface TimetableEntry {
  id: number;
  schoolClassId: number;
  subjectId: number;
  teacherId: number;
  roomId: number;
  dayOfWeek: DayOfWeek;
  startTime: string;
  endTime: string;
}

export interface TimetableEntryRequest {
  schoolClassId: number;
  subjectId: number;
  teacherId: number;
  roomId: number;
  dayOfWeek: DayOfWeek;
  startTime: string;
  endTime: string;
}
