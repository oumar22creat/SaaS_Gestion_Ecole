import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { environment } from '../../environments/environment';
import { DashboardService } from './dashboard.service';

describe('DashboardService', () => {
  let service: DashboardService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(DashboardService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('fetches the dashboard summary', async () => {
    const promise = service.summary();
    const req = httpMock.expectOne(`${environment.apiUrl}/dashboard/summary`);
    expect(req.request.method).toBe('GET');
    req.flush({
      data: { studentCount: 2, teacherCount: 1, classCount: 1, periodFrom: '2026-08-15', periodTo: '2026-09-14', attendanceRate: 50, averageGrade: 12 },
    });

    expect(await promise).toEqual({
      studentCount: 2,
      teacherCount: 1,
      classCount: 1,
      periodFrom: '2026-08-15',
      periodTo: '2026-09-14',
      attendanceRate: 50,
      averageGrade: 12,
    });
  });
});
