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
    const promise = service.submit(3, [{ studentId: 1, score: 14, absent: false, comment: null }]);
    const req = httpMock.expectOne(`${environment.apiUrl}/exams/3/grades`);
    expect(req.request.method).toBe('POST');
    req.flush({ data: [] });

    expect(await promise).toEqual([]);
  });
});
