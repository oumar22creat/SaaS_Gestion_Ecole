import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { environment } from '../../environments/environment';
import { StudentService } from './student.service';

describe('StudentService', () => {
  let service: StudentService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(StudentService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('creates a student', async () => {
    const request = {
      studentNumber: 'E001',
      firstName: 'Léa',
      lastName: 'Martin',
      birthDate: null,
      gender: null,
      schoolClassId: null,
    };
    const promise = service.create(request);
    const req = httpMock.expectOne(`${environment.apiUrl}/students`);
    expect(req.request.method).toBe('POST');
    req.flush({ data: { id: 1, ...request, active: true } });

    expect(await promise).toEqual({ id: 1, ...request, active: true });
  });

  it('imports a CSV file', async () => {
    const file = new File(['studentNumber,firstName,lastName\nE001,Léa,Martin'], 'students.csv', {
      type: 'text/csv',
    });
    const promise = service.importCsv(file);
    const req = httpMock.expectOne(`${environment.apiUrl}/students/import`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body instanceof FormData).toBe(true);
    req.flush({ data: { imported: 1, errors: [] } });

    expect(await promise).toEqual({ imported: 1, errors: [] });
  });
});
