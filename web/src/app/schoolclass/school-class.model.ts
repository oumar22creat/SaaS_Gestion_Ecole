export interface SchoolClass {
  id: number;
  name: string;
  headTeacherId: number | null;
}

export interface SchoolClassRequest {
  name: string;
  headTeacherId: number | null;
}

export interface ClassSubjectAssignment {
  id: number;
  classId: number;
  subjectId: number;
  teacherId: number;
}

export interface ClassSubjectAssignmentRequest {
  subjectId: number;
  teacherId: number;
}
