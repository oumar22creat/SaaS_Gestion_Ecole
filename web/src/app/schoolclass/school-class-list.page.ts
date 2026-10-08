import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatTableModule } from '@angular/material/table';
import { confirmAction } from '../core/confirm-dialog.component';
import { Teacher } from '../teacher/teacher.model';
import { TeacherService } from '../teacher/teacher.service';
import { SchoolClassFormDialog } from './school-class-form.dialog';
import { SchoolClass } from './school-class.model';
import { SchoolClassService } from './school-class.service';

@Component({
  selector: 'app-school-class-list-page',
  imports: [MatTableModule, MatButtonModule, MatIconModule, MatDialogModule, RouterLink],
  templateUrl: './school-class-list.page.html',
  styleUrl: './school-class-list.page.scss',
})
export class SchoolClassListPage {
  private readonly schoolClassService = inject(SchoolClassService);
  private readonly dialog = inject(MatDialog);
  private readonly teacherService = inject(TeacherService);

  protected readonly classes = signal<SchoolClass[]>([]);
  protected readonly teachers = signal<Teacher[]>([]);
  protected readonly loading = signal(false);
  /** Identifiant de la classe dont la planche se prépare : une classe de cinquante prend quelques secondes. */
  protected readonly printingCards = signal<number | null>(null);
  protected readonly columns = ['name', 'headTeacherId', 'actions'];

  /**
   * Nom du professeur principal. La colonne affichait l'identifiant brut : le secrétariat
   * y lisait « 3 », ce qui ne désigne personne et rendait ce réglage invisible — alors
   * qu'il se retrouve imprimé en tête du rapport trimestriel de la classe.
   */
  protected headTeacherName(schoolClass: SchoolClass): string {
    if (schoolClass.headTeacherId === null) {
      return '—';
    }
    const teacher = this.teachers().find((item) => item.id === schoolClass.headTeacherId);
    return teacher ? `${teacher.firstName} ${teacher.lastName}` : '—';
  }

  constructor() {
    void this.refresh();
    void this.teacherService.list().then((teachers) => this.teachers.set(teachers));
  }

  async refresh(): Promise<void> {
    this.loading.set(true);
    try {
      this.classes.set(await this.schoolClassService.list());
    } finally {
      this.loading.set(false);
    }
  }

  openCreateDialog(): void {
    this.dialog
      .open(SchoolClassFormDialog, { data: {}, width: '480px' })
      .afterClosed()
      .subscribe((result) => result && void this.refresh());
  }

  openEditDialog(schoolClass: SchoolClass): void {
    this.dialog
      .open(SchoolClassFormDialog, { data: { schoolClass }, width: '480px' })
      .afterClosed()
      .subscribe((result) => result && void this.refresh());
  }

  async remove(schoolClass: SchoolClass): Promise<void> {
    const confirmed = await confirmAction(this.dialog, {
      title: 'Supprimer cette classe ?',
      message: `"${schoolClass.name}" sera définitivement supprimée.`,
    });
    if (!confirmed) {
      return;
    }
    await this.schoolClassService.remove(schoolClass.id);
    await this.refresh();
  }

  /** Les cartes d'identité de la classe, en un seul PDF prêt à découper. */
  protected async printIdCards(schoolClass: SchoolClass): Promise<void> {
    this.errorMessage.set(null);
    this.printingCards.set(schoolClass.id);
    try {
      await this.schoolClassService.downloadIdCards(schoolClass);
    } catch (error) {
      this.errorMessage.set(extractErrorMessage(error));
    } finally {
      this.printingCards.set(null);
    }
  }
}
