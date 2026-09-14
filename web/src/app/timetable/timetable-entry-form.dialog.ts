import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { extractErrorMessage } from '../core/http-error.util';
import { SchoolClass } from '../schoolclass/school-class.model';
import { SchoolClassService } from '../schoolclass/school-class.service';
import { Subject } from '../subject/subject.model';
import { SubjectService } from '../subject/subject.service';
import { Teacher } from '../teacher/teacher.model';
import { TeacherService } from '../teacher/teacher.service';
import { DAYS_OF_WEEK, TimetableEntry } from './timetable-entry.model';
import { TimetableEntryService } from './timetable-entry.service';
import { Room } from './room.model';
import { RoomService } from './room.service';

export interface TimetableEntryFormDialogData {
  entry?: TimetableEntry;
}

@Component({
  selector: 'app-timetable-entry-form-dialog',
  imports: [
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './timetable-entry-form.dialog.html',
  styleUrl: './timetable-entry-form.dialog.scss',
})
export class TimetableEntryFormDialog {
  private readonly formBuilder = inject(FormBuilder);
  private readonly timetableEntryService = inject(TimetableEntryService);
  private readonly dialogRef = inject(MatDialogRef<TimetableEntryFormDialog, TimetableEntry | undefined>);
  protected readonly data = inject<TimetableEntryFormDialogData>(MAT_DIALOG_DATA);

  protected readonly submitting = signal(false);
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly classes = signal<SchoolClass[]>([]);
  protected readonly subjects = signal<Subject[]>([]);
  protected readonly teachers = signal<Teacher[]>([]);
  protected readonly rooms = signal<Room[]>([]);
  protected readonly days = DAYS_OF_WEEK;

  protected readonly form = this.formBuilder.nonNullable.group({
    schoolClassId: [this.data.entry?.schoolClassId ?? null, [Validators.required]],
    subjectId: [this.data.entry?.subjectId ?? null, [Validators.required]],
    teacherId: [this.data.entry?.teacherId ?? null, [Validators.required]],
    roomId: [this.data.entry?.roomId ?? null, [Validators.required]],
    dayOfWeek: [this.data.entry?.dayOfWeek ?? null, [Validators.required]],
    startTime: [this.data.entry?.startTime?.slice(0, 5) ?? '', [Validators.required]],
    endTime: [this.data.entry?.endTime?.slice(0, 5) ?? '', [Validators.required]],
  });

  constructor(
    schoolClassService: SchoolClassService,
    subjectService: SubjectService,
    teacherService: TeacherService,
    roomService: RoomService,
  ) {
    void schoolClassService.list().then((classes) => this.classes.set(classes));
    void subjectService.list().then((subjects) => this.subjects.set(subjects));
    void teacherService.list().then((teachers) => this.teachers.set(teachers));
    void roomService.list().then((rooms) => this.rooms.set(rooms));
  }

  async submit(): Promise<void> {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.submitting.set(true);
    this.errorMessage.set(null);
    try {
      const value = this.form.getRawValue();
      const request = {
        schoolClassId: value.schoolClassId!,
        subjectId: value.subjectId!,
        teacherId: value.teacherId!,
        roomId: value.roomId!,
        dayOfWeek: value.dayOfWeek!,
        startTime: value.startTime,
        endTime: value.endTime,
      };
      const result = this.data.entry
        ? await this.timetableEntryService.update(this.data.entry.id, request)
        : await this.timetableEntryService.create(request);
      this.dialogRef.close(result);
    } catch (error) {
      this.errorMessage.set(extractErrorMessage(error));
    } finally {
      this.submitting.set(false);
    }
  }

  cancel(): void {
    this.dialogRef.close();
  }
}
