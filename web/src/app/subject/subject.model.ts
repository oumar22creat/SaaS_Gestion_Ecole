export interface Subject {
  id: number;
  name: string;
  code: string;
  coefficient: number;
}

export interface SubjectRequest {
  name: string;
  code: string;
  coefficient: number;
}
