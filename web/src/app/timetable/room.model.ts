export interface Room {
  id: number;
  name: string;
  capacity: number | null;
}

export interface RoomRequest {
  name: string;
  capacity: number | null;
}
