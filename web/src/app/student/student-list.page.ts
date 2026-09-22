import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatChipsModule } from '@angular/material/chips';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatTableModule } from '@angular/material/table';
import { confirmAction } from '../core/confirm-dialog.component';
import { FamilyAccessDialog } from '../familyaccess/family-access.dialog';
import { ListSearchComponent } from '../core/list-search.component';
import { FRENCH_PAGINATOR } from '../core/paginator-intl.provider';
import { SchoolClass } from '../schoolclass/school-class.model';
import { SchoolClassService } from '../schoolclass/school-class.service';
import { StudentFormDialog } from './student-form.dialog';
import { Student } from './student.model';
import { StudentService } from './student.service';

@Component({
  selector: 'app-student-list-page',
  imports: [
    MatTableModule,
    MatButtonModule,
    MatIconModule,
    MatTooltipModule,
    MatChipsModule,
    MatDialogModule,
    MatPaginatorModule,
    ListSearchComponent,
    RouterLink,
  ],
  providers: [FRENCH_PAGINATOR],
  templateUrl: './student-list.page.html',
  styleUrl: './student-list.page.scss',
})
export class StudentListPage {
  private readonly studentService = inject(StudentService);
  private readonly schoolClassService = inject(SchoolClassService);
  private readonly dialog = inject(MatDialog);

  protected readonly students = signal<Student[]>([]);
  protected readonly classes = signal<SchoolClass[]>([]);
  protected readonly loading = signal(false);
  protected readonly total = signal(0);
  protected readonly search = signal('');
  protected readonly pageIndex = signal(0);
  protected readonly pageSize = signal(25);
  protected readonly columns = [
    'studentNumber',
    'firstName',
    'lastName',
    'schoolClassId',
    'active',
    'portalAccess',
    'actions',
  ];

  constructor() {
    void this.refresh();
    void this.schoolClassService.list().then((classes) => this.classes.set(classes));
  }

  /** Une nouvelle recherche repart de la première page : rester en page 4 n'aurait aucun sens. */
  protected onSearch(term: string): void {
    this.search.set(term);
    this.pageIndex.set(0);
    void this.refresh();
  }

  protected onPage(event: PageEvent): void {
    this.pageIndex.set(event.pageIndex);
    this.pageSize.set(event.pageSize);
    void this.refresh();
  }

  /** La colonne affichait l'identifiant technique de la classe (« 6 ») au lieu de son nom. */
  protected className(schoolClassId: number | null): string {
    if (schoolClassId === null) {
      return '—';
    }
    return this.classes().find((schoolClass) => schoolClass.id === schoolClassId)?.name ?? '—';
  }

  async refresh(): Promise<void> {
    this.loading.set(true);
    try {
      const page = await this.studentService.page({
        page: this.pageIndex(),
        pageSize: this.pageSize(),
        search: this.search(),
      });
      this.students.set(page.items);
      this.total.set(page.total);
    } finally {
      this.loading.set(false);
    }
  }

  openCreateDialog(): void {
    this.dialog
      .open(StudentFormDialog, { data: {}, width: '480px' })
      .afterClosed()
      .subscribe((result) => result && void this.refresh());
  }

  /**
   * Ouvre l'accès au portail mobile. Le point d'entrée existait côté serveur mais aucun écran
   * ne l'appelait : toute la partie parent/élève de l'application mobile était donc
   * inatteignable pour un établissement réel.
   */
  openFamilyAccess(student: Student): void {
    this.dialog
      .open(FamilyAccessDialog, {
        width: '460px',
        data: {
          mode: 'open',
          target: 'student',
          recordId: student.id,
          personName: `${student.firstName} ${student.lastName}`,
          suggestedEmail: null,
        },
      })
      .afterClosed()
      .subscribe((opened) => opened && void this.refresh());
  }

  /** Réinitialise le mot de passe de l'accès mobile, depuis la fiche. */
  resetFamilyPassword(student: Student): void {
    this.dialog
      .open(FamilyAccessDialog, {
        width: '460px',
        data: {
          mode: 'reset',
          target: 'student',
          recordId: student.id,
          personName: `${student.firstName} ${student.lastName}`,
          suggestedEmail: null,
        },
      })
      .afterClosed()
      .subscribe((done) => done && void this.refresh());
  }

  openEditDialog(student: Student): void {
    this.dialog
      .open(StudentFormDialog, { data: { student }, width: '480px' })
      .afterClosed()
      .subscribe((result) => result && void this.refresh());
  }

  async deactivate(student: Student): Promise<void> {
    const confirmed = await confirmAction(this.dialog, {
      title: 'Désactiver cet élève ?',
      message: `${student.firstName} ${student.lastName} n'apparaîtra plus comme actif.`,
    });
    if (!confirmed) {
      return;
    }
    await this.studentService.deactivate(student.id);
    await this.refresh();
  }
}
