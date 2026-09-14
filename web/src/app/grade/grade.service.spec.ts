import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { environment } from '../../environments/environment';
import { GradeService } from './grade.service';

describe('GradeService', () => {
  let service: GradeService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(GradeService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('submits grades for an exam', async () => {
    const entries = [{ studentId: 1, score: 16, absent: false, comment: null }];
    const promise = service.submit(1, entries);
    const req = httpMock.expectOne(`${environment.apiUrl}/exams/1/grades`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ entries });
    req.flush({ data: [{ id: 1, examId: 1, studentId: 1, score: 16, absent: false, comment: null }] });

    expect((await promise).length).toBe(1);
  });
});
