import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { environment } from '../../environments/environment';
import { AttendanceService } from './attendance.service';

describe('AttendanceService', () => {
  let service: AttendanceService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(AttendanceService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('submits a roll-call', async () => {
    const request = {
      schoolClassId: 1,
      date: '2026-09-17',
      entries: [{ studentId: 2, status: 'PRESENT' as const, reason: null, justified: false, comment: null }],
    };
    const promise = service.submitRollCall(request);
    const req = httpMock.expectOne(`${environment.apiUrl}/attendance/roll-call`);
    expect(req.request.method).toBe('POST');
    req.flush({ data: [] });

    expect(await promise).toEqual([]);
  });
});
