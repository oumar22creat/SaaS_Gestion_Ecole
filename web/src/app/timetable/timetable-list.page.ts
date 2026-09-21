import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { confirmAction } from '../core/confirm-dialog.component';
import { SchoolClass } from '../schoolclass/school-class.model';
import { SchoolClassService } from '../schoolclass/school-class.service';
import { Subject } from '../subject/subject.model';
import { SubjectService } from '../subject/subject.service';
import { Teacher } from '../teacher/teacher.model';
import { TeacherService } from '../teacher/teacher.service';
import { DAYS_OF_WEEK, TimetableEntry } from './timetable-entry.model';
import { TimetableEntryFormDialog } from './timetable-entry-form.dialog';
import { TimetableEntryService } from './timetable-entry.service';
import { Room } from './room.model';
import { RoomService } from './room.service';

@Component({
  selector: 'app-timetable-list-page',
  imports: [MatTableModule, MatButtonModule, MatIconModule, MatDialogModule, RouterLink],
  templateUrl: './timetable-list.page.html',
  styleUrl: './timetable-list.page.scss',
})
export class TimetableListPage {
  private readonly timetableEntryService = inject(TimetableEntryService);
  private readonly dialog = inject(MatDialog);
  private readonly schoolClassService = inject(SchoolClassService);
  private readonly subjectService = inject(SubjectService);
  private readonly teacherService = inject(TeacherService);
  private readonly roomService = inject(RoomService);

  protected readonly entries = signal<TimetableEntry[]>([]);
  protected readonly classes = signal<SchoolClass[]>([]);
  protected readonly subjects = signal<Subject[]>([]);
  protected readonly teachers = signal<Teacher[]>([]);
  protected readonly rooms = signal<Room[]>([]);
  protected readonly loading = signal(false);
  protected readonly columns = [
    'dayOfWeek',
    'time',
    'schoolClassId',
    'subjectId',
    'teacherId',
    'roomId',
    'actions',
  ];
  protected readonly dayLabels = new Map(DAYS_OF_WEEK.map((d) => [d.value, d.label]));

  constructor() {
    void this.refresh();
    void this.schoolClassService.list().then((classes) => this.classes.set(classes));
    void this.subjectService.list().then((subjects) => this.subjects.set(subjects));
    void this.teacherService.list().then((teachers) => this.teachers.set(teachers));
    void this.roomService.list().then((rooms) => this.rooms.set(rooms));
  }

  async refresh(): Promise<void> {
    this.loading.set(true);
    try {
      this.entries.set(await this.timetableEntryService.list());
    } finally {
      this.loading.set(false);
    }
  }

  className(id: number): string {
    return this.classes().find((c) => c.id === id)?.name ?? `#${id}`;
  }

  subjectName(id: number): string {
    return this.subjects().find((s) => s.id === id)?.name ?? `#${id}`;
  }

  teacherName(id: number): string {
    const teacher = this.teachers().find((t) => t.id === id);
    return teacher ? `${teacher.firstName} ${teacher.lastName}` : `#${id}`;
  }

  roomName(id: number): string {
    return this.rooms().find((r) => r.id === id)?.name ?? `#${id}`;
  }

  openCreateDialog(): void {
    this.dialog
      .open(TimetableEntryFormDialog, { data: {}, width: '480px' })
      .afterClosed()
      .subscribe((result) => result && void this.refresh());
  }

  openEditDialog(entry: TimetableEntry): void {
    this.dialog
      .open(TimetableEntryFormDialog, { data: { entry }, width: '480px' })
      .afterClosed()
      .subscribe((result) => result && void this.refresh());
  }

  async remove(entry: TimetableEntry): Promise<void> {
    const confirmed = await confirmAction(this.dialog, {
      title: 'Supprimer ce créneau ?',
      message: `${this.subjectName(entry.subjectId)} — ${this.className(entry.schoolClassId)} sera retiré de l'emploi du temps.`,
    });
    if (!confirmed) {
      return;
    }
    await this.timetableEntryService.remove(entry.id);
    await this.refresh();
  }
}
