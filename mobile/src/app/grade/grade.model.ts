export interface Exam {
  id: number;
  schoolClassId: number;
  subjectId: number;
  label: string;
  maxScore: number;
  coefficient: number;
  examDate: string;
}

export interface Grade {
  id: number;
  examId: number;
  studentId: number;
  score: number | null;
  absent: boolean;
  comment: string | null;
}

export interface GradeEntry {
  studentId: number;
  score: number | null;
  absent: boolean;
  comment: string | null;
}
