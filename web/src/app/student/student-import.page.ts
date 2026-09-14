import { Component, ViewChild, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatStepper, MatStepperModule } from '@angular/material/stepper';
import { MatTableModule } from '@angular/material/table';
import { extractErrorMessage } from '../core/http-error.util';
import { StudentImportResult } from './student.model';
import { StudentService } from './student.service';

/**
 * Import CSV en masse (cahier-des-charges.md §7, mockup "Secrétaire — Import élèves" de
 * docs/MOCKUPS.md §7). Adapté à 2 étapes (pas 3) : le backend importe en une seule opération
 * — il n'y a pas de phase de "vérification" séparée avant confirmation (voir
 * `POST /api/v1/students/import`, ROADMAP.md 1.5) — ce serait mentir à l'utilisateur que de
 * lui laisser croire qu'une étape "Confirmer" peut encore annuler l'import déjà effectué.
 */
@Component({
  selector: 'app-student-import-page',
  imports: [MatStepperModule, MatButtonModule, MatIconModule, MatTableModule, RouterLink],
  templateUrl: './student-import.page.html',
  styleUrl: './student-import.page.scss',
})
export class StudentImportPage {
  private readonly studentService = inject(StudentService);

  @ViewChild(MatStepper) private stepper!: MatStepper;

  protected readonly selectedFile = signal<File | null>(null);
  protected readonly importing = signal(false);
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly result = signal<StudentImportResult | null>(null);
  protected readonly errorColumns = ['line', 'message'];

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.selectedFile.set(input.files?.[0] ?? null);
  }

  async import(): Promise<void> {
    const file = this.selectedFile();
    if (!file) {
      return;
    }
    this.importing.set(true);
    this.errorMessage.set(null);
    try {
      this.result.set(await this.studentService.importCsv(file));
      this.stepper.next();
    } catch (error) {
      this.errorMessage.set(extractErrorMessage(error));
    } finally {
      this.importing.set(false);
    }
  }
}
