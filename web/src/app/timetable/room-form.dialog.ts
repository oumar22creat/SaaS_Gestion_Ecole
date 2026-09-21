import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { extractErrorMessage } from '../core/http-error.util';
import { Room } from './room.model';
import { RoomService } from './room.service';
import { fieldError } from '../core/form-error.util';

export interface RoomFormDialogData {
  room?: Room;
}

@Component({
  selector: 'app-room-form-dialog',
  imports: [
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './room-form.dialog.html',
  styleUrl: './room-form.dialog.scss',
})
export class RoomFormDialog {
  protected readonly fieldError = fieldError;
  private readonly formBuilder = inject(FormBuilder);
  private readonly roomService = inject(RoomService);
  private readonly dialogRef = inject(MatDialogRef<RoomFormDialog, Room | undefined>);
  protected readonly data = inject<RoomFormDialogData>(MAT_DIALOG_DATA);

  protected readonly submitting = signal(false);
  protected readonly errorMessage = signal<string | null>(null);

  protected readonly form = this.formBuilder.nonNullable.group({
    name: [this.data.room?.name ?? '', [Validators.required, Validators.maxLength(100)]],
    capacity: [this.data.room?.capacity ?? null],
  });

  async submit(): Promise<void> {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.submitting.set(true);
    this.errorMessage.set(null);
    try {
      const value = this.form.getRawValue();
      const result = this.data.room
        ? await this.roomService.update(this.data.room.id, value)
        : await this.roomService.create(value);
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
