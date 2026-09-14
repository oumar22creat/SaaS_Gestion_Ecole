export interface Teacher {
  id: number;
  firstName: string;
  lastName: string;
  email: string | null;
  phone: string | null;
  active: boolean;
}

export interface TeacherRequest {
  firstName: string;
  lastName: string;
  email: string | null;
  phone: string | null;
}
