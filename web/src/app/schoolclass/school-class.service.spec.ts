import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { environment } from '../../environments/environment';
import { SchoolClassService } from './school-class.service';

describe('SchoolClassService', () => {
  let service: SchoolClassService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(SchoolClassService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('creates a class', async () => {
    const promise = service.create({ name: '6ème A', headTeacherId: null });
    const req = httpMock.expectOne(`${environment.apiUrl}/classes`);
    expect(req.request.method).toBe('POST');
    req.flush({ data: { id: 1, name: '6ème A', headTeacherId: null } });

    expect(await promise).toEqual({ id: 1, name: '6ème A', headTeacherId: null });
  });

  it('assigns a teacher to a subject for a class', async () => {
    const promise = service.assign(1, { subjectId: 2, teacherId: 3 });
    const req = httpMock.expectOne(`${environment.apiUrl}/classes/1/subjects`);
    expect(req.request.method).toBe('POST');
    req.flush({ data: { id: 10, classId: 1, subjectId: 2, teacherId: 3 } });

    expect(await promise).toEqual({ id: 10, classId: 1, subjectId: 2, teacherId: 3 });
  });
});
