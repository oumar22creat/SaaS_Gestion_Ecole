import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { environment } from '../../environments/environment';
import { TimetableEntryService } from './timetable-entry.service';

describe('TimetableEntryService', () => {
  let service: TimetableEntryService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(TimetableEntryService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('surfaces the conflict message returned by the backend', async () => {
    const request = {
      schoolClassId: 1,
      subjectId: 1,
      teacherId: 1,
      roomId: 1,
      dayOfWeek: 'MONDAY' as const,
      startTime: '08:00',
      endTime: '09:00',
    };
    const promise = service.create(request);
    const req = httpMock.expectOne(`${environment.apiUrl}/timetable-entries`);
    req.flush(
      { error: { code: 'TEACHER_ALREADY_BOOKED', message: 'Cet enseignant a déjà un cours sur ce créneau' } },
      { status: 409, statusText: 'Conflict' },
    );

    await expectAsync(promise).toBeRejected();
  });
});
