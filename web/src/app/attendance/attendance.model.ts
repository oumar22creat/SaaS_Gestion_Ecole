export type AttendanceStatus = 'PRESENT' | 'ABSENT' | 'LATE' | 'EARLY_DEPARTURE';

export const ATTENDANCE_STATUSES: { value: AttendanceStatus; label: string }[] = [
  { value: 'PRESENT', label: 'Présent' },
  { value: 'ABSENT', label: 'Absent' },
  { value: 'LATE', label: 'Retard' },
  { value: 'EARLY_DEPARTURE', label: 'Départ anticipé' },
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

export interface RollCallEntry {
  studentId: number;
  status: AttendanceStatus;
  reason: string | null;
  justified: boolean;
  comment: string | null;
}

export interface RollCallRequest {
  schoolClassId: number;
  date: string;
  entries: RollCallEntry[];
}

export interface AttendanceRecordChange {
  previousStatus: AttendanceStatus;
  previousReason: string | null;
  previousJustified: boolean;
  previousComment: string | null;
  changedAt: string;
}
