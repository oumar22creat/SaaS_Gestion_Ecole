import { DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatMenuModule } from '@angular/material/menu';
import { MatTableModule } from '@angular/material/table';
import { confirmAction } from '../core/confirm-dialog.component';
import { FRENCH_DATE_LOCALE } from '../core/date-locale.provider';
import { fieldError } from '../core/form-error.util';
import { extractErrorMessage } from '../core/http-error.util';
import { toIsoDate } from '../core/iso-date.util';
import { PromotionDialog } from './promotion.dialog';
import { SchoolYear, SchoolYearService } from './school-year.service';

/**
 * Années scolaires et rentrée (cahier-des-charges.md §7).
 *
 * <p>Une année est le cadre de tout le reste : les inscriptions, les bulletins, les frais s'y
 * rattachent. L'écran sert deux gestes rares mais structurants — ouvrir l'année suivante, et
 * y faire passer la promotion.
 */
@Component({
  selector: 'app-school-year-list-page',
  imports: [
    DatePipe,
    ReactiveFormsModule,
    MatTableModule,
    MatButtonModule,
    MatIconModule,
    MatMenuModule,
    MatFormFieldModule,
    MatInputModule,
    MatDatepickerModule,
    MatDialogModule,
  ],
  providers: [FRENCH_DATE_LOCALE],
  templateUrl: './school-year-list.page.html',
})
export class SchoolYearListPage {
  private readonly schoolYearService = inject(SchoolYearService);
  private readonly formBuilder = inject(FormBuilder);
  private readonly dialog = inject(MatDialog);

  protected readonly fieldError = fieldError;
  protected readonly years = signal<SchoolYear[]>([]);
  protected readonly loading = signal(false);
  protected readonly creating = signal(false);
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly columns = ['label', 'period', 'status', 'enrolled', 'actions'];

  protected readonly form = this.formBuilder.nonNullable.group({
    label: ['', [Validators.required, Validators.maxLength(50)]],
    startDate: [null as Date | null, [Validators.required]],
    endDate: [null as Date | null, [Validators.required]],
  });

  constructor() {
    void this.refresh();
  }

  async refresh(): Promise<void> {
    this.loading.set(true);
    try {
      this.years.set(await this.schoolYearService.list());
    } catch (error) {
      this.years.set([]);
      this.errorMessage.set(extractErrorMessage(error));
    } finally {
      this.loading.set(false);
    }
  }

  protected statusLabel(status: string): string {
    return { PLANNED: 'Préparée', ACTIVE: 'En cours', CLOSED: 'Clôturée' }[status] ?? status;
  }

  /** L'année en cours : la seule sur laquelle une rentrée peut prendre son point de départ. */
  protected activeYear(): SchoolYear | undefined {
    return this.years().find((year) => year.status === 'ACTIVE');
  }

  async create(): Promise<void> {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.creating.set(true);
    this.errorMessage.set(null);
    try {
      const value = this.form.getRawValue();
      await this.schoolYearService.create(
        value.label,
        toIsoDate(value.startDate!),
        toIsoDate(value.endDate!),
      );
      this.form.reset({ label: '', startDate: null, endDate: null });
      await this.refresh();
    } catch (error) {
      this.errorMessage.set(extractErrorMessage(error));
    } finally {
      this.creating.set(false);
    }
  }

  /**
   * Activer bascule tout l'établissement. L'année en cours est clôturée au passage — d'où la
   * confirmation, qui nomme les deux années pour qu'aucune méprise ne soit possible.
   */
  async activate(year: SchoolYear): Promise<void> {
    const current = this.activeYear();
    const confirmed = await confirmAction(this.dialog, {
      title: `Basculer sur ${year.label} ?`,
      message: current
        ? `Tout l'établissement travaillera sur ${year.label}. ${current.label} sera clôturée : ses bulletins et ses factures restent consultables, mais plus modifiables.`
        : `Tout l'établissement travaillera sur ${year.label}.`,
    });
    if (!confirmed) {
      return;
    }
    await this.run(() => this.schoolYearService.activate(year.id));
  }

  async close(year: SchoolYear): Promise<void> {
    const confirmed = await confirmAction(this.dialog, {
      title: `Clôturer ${year.label} ?`,
      message:
        "L'année passe en lecture seule. Si c'est l'année en cours, l'établissement se retrouvera sans année active tant qu'une autre n'est pas activée.",
    });
    if (!confirmed) {
      return;
    }
    await this.run(() => this.schoolYearService.close(year.id));
  }

  /** Ouvre l'assistant de rentrée vers cette année, depuis l'année en cours. */
  openPromotion(target: SchoolYear): void {
    const source = this.activeYear();
    if (!source) {
      this.errorMessage.set("Aucune année en cours : activez l'année de départ avant la rentrée.");
      return;
    }
    this.dialog
      .open(PromotionDialog, {
        width: '860px',
        data: { sourceYear: source, targetYear: target },
      })
      .afterClosed()
      .subscribe((done) => done && void this.refresh());
  }

  private async run(operation: () => Promise<unknown>): Promise<void> {
    this.errorMessage.set(null);
    try {
      await operation();
      await this.refresh();
    } catch (error) {
      this.errorMessage.set(extractErrorMessage(error));
    }
  }
}
