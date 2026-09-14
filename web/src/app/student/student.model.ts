export interface Student {
  id: number;
  studentNumber: string;
  firstName: string;
  lastName: string;
  birthDate: string | null;
  gender: string | null;
  schoolClassId: number | null;
  active: boolean;
}

export interface StudentRequest {
  studentNumber: string;
  firstName: string;
  lastName: string;
  birthDate: string | null;
  gender: string | null;
  schoolClassId: number | null;
}

export interface StudentImportResult {
  imported: number;
  errors: { line: number; message: string }[];
}
