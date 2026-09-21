import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { environment } from '../../environments/environment';
import { TeacherService } from './teacher.service';

describe('TeacherService', () => {
  let service: TeacherService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(TeacherService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('lists teachers', async () => {
    const promise = service.list();
    const req = httpMock.expectOne((r) => r.url === `${environment.apiUrl}/teachers`);
    expect(req.request.method).toBe('GET');
    req.flush({
      data: [
        { id: 1, firstName: 'Marie', lastName: 'Curie', email: null, phone: null, active: true },
      ],
    });

    expect(await promise).toEqual([
      { id: 1, firstName: 'Marie', lastName: 'Curie', email: null, phone: null, active: true },
    ]);
  });

  it('creates a teacher', async () => {
    const request = { firstName: 'Marie', lastName: 'Curie', email: null, phone: null };
    const promise = service.create(request);
    const req = httpMock.expectOne(`${environment.apiUrl}/teachers`);
    expect(req.request.method).toBe('POST');
    req.flush({ data: { id: 1, ...request, active: true } });

    expect(await promise).toEqual({ id: 1, ...request, active: true });
  });

  it('deactivates a teacher', async () => {
    const promise = service.deactivate(1);
    const req = httpMock.expectOne(`${environment.apiUrl}/teachers/1`);
    expect(req.request.method).toBe('DELETE');
    req.flush(null);

    await expectAsync(promise).toBeResolved();
  });
});
