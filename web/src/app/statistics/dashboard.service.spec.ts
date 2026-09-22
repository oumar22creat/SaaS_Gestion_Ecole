import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { environment } from '../../environments/environment';
import { DashboardService } from './dashboard.service';

describe('DashboardService', () => {
  let service: DashboardService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(DashboardService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('fetches the dashboard summary', async () => {
    const summary = {
      studentCount: 2,
      teacherCount: 1,
      classCount: 1,
      periodFrom: '2026-08-15',
      periodTo: '2026-09-14',
      attendanceRate: 50,
      averageGrade: 12,
      finance: {
        invoicedCents: 200000,
        collectedCents: 150000,
        outstandingCents: 50000,
        overdueCents: 50000,
        collectionRate: 75,
        invoiceCount: 2,
        settledInvoiceCount: 1,
        overdueInvoiceCount: 1,
        lateStudentCount: 1,
        currency: 'XOF',
        collectionByMethod: [{ method: 'CASH', amountCents: 150000, paymentCount: 2 }],
      },
    };

    const promise = service.summary();
    const req = httpMock.expectOne(`${environment.apiUrl}/dashboard/summary`);
    expect(req.request.method).toBe('GET');
    req.flush({ data: summary });

    expect(await promise).toEqual(summary);
  });

  /**
   * `finance` est nul tant qu'aucune facture n'a été émise : le tableau de bord doit alors
   * masquer le bloc comptable plutôt qu'afficher « 0 % recouvré », qui se lirait comme un
   * défaut de recouvrement.
   */
  it('accepts a summary without any accounting data', async () => {
    const promise = service.summary();
    httpMock.expectOne(`${environment.apiUrl}/dashboard/summary`).flush({
      data: {
        studentCount: 0,
        teacherCount: 0,
        classCount: 0,
        periodFrom: '2026-08-15',
        periodTo: '2026-09-14',
        attendanceRate: null,
        averageGrade: null,
        finance: null,
      },
    });

    expect((await promise).finance).toBeNull();
  });
});
