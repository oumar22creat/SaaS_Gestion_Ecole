import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { environment } from '../../environments/environment';
import { PlatformDashboardService } from './platform-dashboard.service';

describe('PlatformDashboardService', () => {
  let service: PlatformDashboardService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(PlatformDashboardService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('loads the platform summary', async () => {
    const summary = {
      trialCount: 1,
      activeCount: 2,
      readOnlyCount: 0,
      suspendedCount: 0,
      cancelledCount: 0,
      mrrCents: 10000,
      arrCents: 120000,
      currency: 'XOF',
      churnRate: 0.1,
      conversionRate: 0.5,
      notificationsSentCount: 3,
    };
    const promise = service.summary();
    const req = httpMock.expectOne(`${environment.apiUrl}/admin/dashboard/summary`);
    expect(req.request.method).toBe('GET');
    req.flush({ data: summary });

    expect(await promise).toEqual(summary);
  });
});
