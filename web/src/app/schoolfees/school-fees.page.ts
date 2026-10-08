import { DatePipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatTableModule } from '@angular/material/table';
import { MatTabsModule } from '@angular/material/tabs';
import { FRENCH_DATE_LOCALE } from '../core/date-locale.provider';
import { confirmAction } from '../core/confirm-dialog.component';
import { fieldError } from '../core/form-error.util';
import { PagedTotal } from '../core/api-response.model';
import { extractErrorMessage } from '../core/http-error.util';
import { FRENCH_PAGINATOR } from '../core/paginator-intl.provider';
import { toIsoDate } from '../core/iso-date.util';
import { formatMoney } from '../core/money.util';
import { SchoolClass } from '../schoolclass/school-class.model';
import { SchoolClassService } from '../schoolclass/school-class.service';
import { PaperworkService } from '../paperwork/paperwork.service';
import { PaymentDialog } from './payment.dialog';
import {
  FeeOutstanding,
  FeePaymentJournalEntry,
  FeeSchedule,
  FeeSummary,
  SchoolFeesService,
  paymentMethodLabel,
} from './school-fees.service';

/**
 * Comptabilité des frais scolaires (cahier-des-charges.md §19.4).
 *
 * <p>L'écran s'ouvre sur tout l'établissement, la classe n'étant qu'un filtre : un comptable
 * suit le recouvrement de son école, pas d'une classe à la fois. Les trois onglets suivent
 * les trois moments de son travail — relancer les retards, déclarer ce qui est dû, relire la
 * caisse.
 */
@Component({
  selector: 'app-school-fees-page',
  imports: [
    DatePipe,
    FormsModule,
    ReactiveFormsModule,
    MatTabsModule,
    MatFormFieldModule,
    MatSelectModule,
    MatInputModule,
    MatDatepickerModule,
    MatCheckboxModule,
    MatButtonModule,
    MatTableModule,
    MatIconModule,
    MatPaginatorModule,
    MatDialogModule,
  ],
  providers: [FRENCH_DATE_LOCALE, FRENCH_PAGINATOR],
  templateUrl: './school-fees.page.html',
  styleUrl: './school-fees.page.scss',
})
export class SchoolFeesPage {
  private readonly schoolFeesService = inject(SchoolFeesService);
  private readonly schoolClassService = inject(SchoolClassService);
  private readonly formBuilder = inject(FormBuilder);
  private readonly dialog = inject(MatDialog);
  private readonly paperworkService = inject(PaperworkService);

  protected readonly fieldError = fieldError;
  protected readonly money = formatMoney;
  protected readonly methodLabel = paymentMethodLabel;

  protected readonly classes = signal<SchoolClass[]>([]);
  protected readonly schedules = signal<FeeSchedule[]>([]);
  protected readonly outstanding = signal<FeeOutstanding[]>([]);
  protected readonly journal = signal<FeePaymentJournalEntry[]>([]);
  protected readonly summary = signal<FeeSummary | null>(null);
  protected readonly loading = signal(false);
  protected readonly creating = signal(false);
  protected readonly reminding = signal(false);
  protected readonly reminderMessage = signal<string | null>(null);
  protected readonly errorMessage = signal<string | null>(null);

  protected readonly outstandingColumns = [
    'student',
    'className',
    'schedule',
    'amount',
    'late',
    'actions',
  ];
  protected readonly scheduleColumns = ['label', 'amount', 'due', 'actions'];
  protected readonly journalColumns = ['paidAt', 'student', 'amount', 'method', 'reference', 'receipt'];

  /**
   * 'ALL' plutôt que null : Material considère une valeur nulle comme « rien de
   * sélectionné » et n'affiche alors aucun libellé — le comptable ne pouvait pas savoir que
   * l'écran portait sur tout l'établissement.
   */
  protected schoolClassId: number | 'ALL' = 'ALL';
  protected onlyOverdue = false;
  protected journalFrom = startOfMonth();
  protected journalTo = new Date();

  /**
   * Totaux renvoyés par le serveur, portant sur tout le filtre et non sur la page affichée.
   * Les sommer ici reviendrait, depuis que ces listes sont paginées, à présenter le total
   * de vingt lignes comme celui de la période — l'erreur qu'on ne découvre qu'en
   * justifiant une caisse.
   */
  protected readonly outstandingTotal = signal(0);
  protected readonly journalTotal = signal(0);
  protected readonly outstandingCount = signal(0);
  protected readonly journalCount = signal(0);
  protected readonly outstandingPage = signal(0);
  protected readonly outstandingSize = signal(25);
  protected readonly journalPage = signal(0);
  protected readonly journalSize = signal(25);

  /** Traduction du filtre d'écran vers le paramètre d'API, qui lui attend null. */
  private get classFilter(): number | null {
    return this.schoolClassId === 'ALL' ? null : this.schoolClassId;
  }

  protected readonly scheduleForm = this.formBuilder.nonNullable.group({
    label: ['', [Validators.required, Validators.maxLength(120)]],
    amountCents: [50000, [Validators.required, Validators.min(1)]],
    dueDate: [null as Date | null, [Validators.required]],
  });

  constructor() {
    void this.schoolClassService.list().then((classes) => this.classes.set(classes));
    void this.load();
    void this.loadJournal();
  }

  async load(): Promise<void> {
    this.loading.set(true);
    this.errorMessage.set(null);
    try {
      const [summary, outstanding] = await Promise.all([
        this.schoolFeesService.summary(this.classFilter),
        this.schoolFeesService.outstanding(
          this.classFilter,
          this.onlyOverdue,
          this.outstandingPage(),
          this.outstandingSize(),
        ),
      ]);
      this.summary.set(summary);
      this.applyOutstanding(outstanding);
      const classId = this.classFilter;
      this.schedules.set(classId === null ? [] : await this.schoolFeesService.listSchedules(classId));
    } catch (error) {
      // Une liste vide se lirait comme « aucun impayé », c'est-à-dire l'inverse du problème.
      this.resetOutstanding();
      this.summary.set(null);
      this.errorMessage.set(extractErrorMessage(error));
    } finally {
      this.loading.set(false);
    }
  }

  async loadOutstanding(): Promise<void> {
    this.loading.set(true);
    try {
      this.applyOutstanding(
        await this.schoolFeesService.outstanding(
          this.classFilter,
          this.onlyOverdue,
          this.outstandingPage(),
          this.outstandingSize(),
        ),
      );
    } catch (error) {
      this.resetOutstanding();
      this.errorMessage.set(extractErrorMessage(error));
    } finally {
      this.loading.set(false);
    }
  }

  async loadJournal(): Promise<void> {
    try {
      const page = await this.schoolFeesService.paymentJournal(
        toIsoDate(this.journalFrom),
        toIsoDate(this.journalTo),
        this.journalPage(),
        this.journalSize(),
      );
      this.journal.set(page.items);
      this.journalCount.set(page.total);
      this.journalTotal.set(page.totalSum);
    } catch (error) {
      this.journal.set([]);
      this.journalCount.set(0);
      this.journalTotal.set(0);
      this.errorMessage.set(extractErrorMessage(error));
    }
  }

  private applyOutstanding(page: PagedTotal<FeeOutstanding>): void {
    this.outstanding.set(page.items);
    this.outstandingCount.set(page.total);
    this.outstandingTotal.set(page.totalSum);
  }

  /** Une liste vide se lirait comme « aucun impayé », c'est-à-dire l'inverse du problème. */
  private resetOutstanding(): void {
    this.outstanding.set([]);
    this.outstandingCount.set(0);
    this.outstandingTotal.set(0);
  }

  /** Changer de filtre repart de la première page : rester en page 4 n'aurait aucun sens. */
  protected onOutstandingFilterChange(): void {
    this.outstandingPage.set(0);
    void this.loadOutstanding();
  }

  protected onOutstandingPage(event: PageEvent): void {
    this.outstandingPage.set(event.pageIndex);
    this.outstandingSize.set(event.pageSize);
    void this.loadOutstanding();
  }

  protected onJournalFilterChange(): void {
    this.journalPage.set(0);
    void this.loadJournal();
  }

  protected onJournalPage(event: PageEvent): void {
    this.journalPage.set(event.pageIndex);
    this.journalSize.set(event.pageSize);
    void this.loadJournal();
  }

  async createSchedule(): Promise<void> {
    const classId = this.classFilter;
    if (classId === null || this.scheduleForm.invalid) {
      this.scheduleForm.markAllAsTouched();
      return;
    }
    this.creating.set(true);
    this.errorMessage.set(null);
    try {
      const value = this.scheduleForm.getRawValue();
      await this.schoolFeesService.createSchedule(
        classId,
        value.label,
        value.amountCents,
        toIsoDate(value.dueDate!),
      );
      this.scheduleForm.reset({ label: '', amountCents: 50000, dueDate: null });
      await this.load();
    } catch (error) {
      this.errorMessage.set(extractErrorMessage(error));
    } finally {
      this.creating.set(false);
    }
  }

  async generate(schedule: FeeSchedule): Promise<void> {
    this.errorMessage.set(null);
    try {
      await this.schoolFeesService.generateInvoices(schedule.id);
      await this.load();
    } catch (error) {
      this.errorMessage.set(extractErrorMessage(error));
    }
  }

  /** Reçu d'un règlement encaissé, à remettre à la famille. */
  async downloadReceipt(line: FeePaymentJournalEntry): Promise<void> {
    this.errorMessage.set(null);
    try {
      await this.paperworkService.downloadPaymentReceipt(line.paymentId, line.studentName);
    } catch (error) {
      this.errorMessage.set(extractErrorMessage(error));
    }
  }

  /**
   * Relance par SMS les familles dont une échéance est dépassée. Confirmée d'abord : une
   * relance part chez de vraies familles et ne se rattrape pas.
   */
  async remindLateFamilies(): Promise<void> {
    const stats = this.summary();
    if (!stats || stats.lateStudentCount === 0) {
      return;
    }
    const confirmed = await confirmAction(this.dialog, {
      title: 'Relancer par SMS ?',
      message: `Un SMS de rappel sera envoyé aux responsables de ${stats.lateStudentCount} élève(s) en retard. Un seul message par élève, même s'il cumule plusieurs échéances.`,
    });
    if (!confirmed) {
      return;
    }
    this.reminding.set(true);
    this.errorMessage.set(null);
    this.reminderMessage.set(null);
    try {
      const count = await this.schoolFeesService.remindOverdueFamilies(this.classFilter);
      this.reminderMessage.set(
        count === 0
          ? 'Aucune famille à relancer.'
          : `${count} famille(s) relancée(s). Les envois sont tracés dans le journal des SMS.`,
      );
    } catch (error) {
      this.errorMessage.set(extractErrorMessage(error));
    } finally {
      this.reminding.set(false);
    }
  }

  openPayment(entry: FeeOutstanding): void {
    this.dialog
      .open(PaymentDialog, { data: { entry }, width: '460px' })
      .afterClosed()
      .subscribe((recorded) => {
        if (recorded) {
          void this.load();
          void this.loadJournal();
        }
      });
  }

  /** « 1 jour » plutôt que « 1 jours » : le libellé est lu à chaque ligne de relance. */
  protected lateLabel(daysLate: number): string {
    return daysLate <= 1 ? `${daysLate} jour` : `${daysLate} jours`;
  }
}

/** Le journal s'ouvre sur le mois en cours : c'est la période que le comptable rapproche. */
function startOfMonth(): Date {
  const today = new Date();
  return new Date(today.getFullYear(), today.getMonth(), 1);
}
