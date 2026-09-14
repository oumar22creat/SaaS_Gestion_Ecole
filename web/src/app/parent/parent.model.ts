export interface Parent {
  id: number;
  firstName: string;
  lastName: string;
  email: string | null;
  phone: string | null;
}

export interface ParentRequest {
  firstName: string;
  lastName: string;
  email: string | null;
  phone: string | null;
}

export interface StudentParentLink {
  studentId: number;
  parentId: number;
  relationship: string;
  primaryContact: boolean;
}

export interface StudentParentLinkRequest {
  parentId: number;
  relationship: string;
  primaryContact: boolean;
}
