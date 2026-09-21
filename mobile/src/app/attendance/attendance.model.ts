export type AttendanceStatus = 'PRESENT' | 'ABSENT' | 'LATE' | 'EARLY_DEPARTURE';

/** `icon` : nom Ionicons enregistré dans app.config.ts (docs/DESIGN.md §9). */
export const ATTENDANCE_STATUSES: { value: AttendanceStatus; label: string; icon: string }[] = [
  { value: 'PRESENT', label: 'Présent', icon: 'checkmark-circle-outline' },
  { value: 'ABSENT', label: 'Absent', icon: 'close-circle-outline' },
  { value: 'LATE', label: 'Retard', icon: 'time-outline' },
  { value: 'EARLY_DEPARTURE', label: 'Départ anticipé', icon: 'exit-outline' },
];

export interface AttendanceRecord {
  id: number;
  studentId: number;
  schoolClassId: number;
  date: string;
  status: AttendanceStatus;
  reason: string | null;
  justified: boolean;
  comment: string | null;
  parentNotified: boolean;
}

export interface RollCallRequest {
  schoolClassId: number;
  date: string;
  entries: {
    studentId: number;
    status: AttendanceStatus;
    reason: string | null;
    justified: boolean;
    comment: string | null;
  }[];
}
