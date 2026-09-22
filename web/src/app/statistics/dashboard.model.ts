import { FeeSummary } from '../schoolfees/school-fees.service';

export interface DashboardSummary {
  studentCount: number;
  teacherCount: number;
  classCount: number;
  periodFrom: string;
  periodTo: string;
  attendanceRate: number | null;
  averageGrade: number | null;
  /** Null tant qu'aucune facture n'a été émise : un bloc à zéro se lirait comme un défaut
   *  de recouvrement alors que l'établissement n'a simplement rien facturé. */
  finance: FeeSummary | null;
}
