export interface DashboardSummary {
  studentCount: number;
  teacherCount: number;
  classCount: number;
  periodFrom: string;
  periodTo: string;
  attendanceRate: number | null;
  averageGrade: number | null;
}
