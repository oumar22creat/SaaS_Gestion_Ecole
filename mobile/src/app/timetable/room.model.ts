/** Salle de cours — identique à web/src/app/timetable/room.model.ts, sans les champs d'écriture. */
export interface Room {
  id: number;
  name: string;
  capacity: number | null;
}
