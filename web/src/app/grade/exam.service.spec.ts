import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { environment } from '../../environments/environment';
import { ExamService } from './exam.service';

describe('ExamService', () => {
  let service: ExamService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(ExamService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('creates an exam', async () => {
    const request = { schoolClassId: 1, subjectId: 1, label: 'Contrôle', maxScore: 20, coefficient: 1, examDate: '2026-09-20' };
    const promise = service.create(request);
    const req = httpMock.expectOne(`${environment.apiUrl}/exams`);
    expect(req.request.method).toBe('POST');
    req.flush({ data: { id: 1, ...request } });

    expect(await promise).toEqual({ id: 1, ...request });
  });

  it('fetches exam statistics', async () => {
    const promise = service.statistics(1);
    const req = httpMock.expectOne(`${environment.apiUrl}/exams/1/statistics`);
    req.flush({ data: { examId: 1, gradedCount: 2, average: 12, min: 8, max: 16 } });

    expect(await promise).toEqual({ examId: 1, gradedCount: 2, average: 12, min: 8, max: 16 });
  });
});
