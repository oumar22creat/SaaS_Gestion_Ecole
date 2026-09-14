import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { environment } from '../../environments/environment';
import { SubjectService } from './subject.service';

describe('SubjectService', () => {
  let service: SubjectService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(SubjectService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('creates a subject', async () => {
    const request = { name: 'Maths', code: 'MATH', coefficient: 3 };
    const promise = service.create(request);
    const req = httpMock.expectOne(`${environment.apiUrl}/subjects`);
    expect(req.request.method).toBe('POST');
    req.flush({ data: { id: 1, ...request } });

    expect(await promise).toEqual({ id: 1, ...request });
  });

  it('removes a subject', async () => {
    const promise = service.remove(1);
    httpMock.expectOne({ url: `${environment.apiUrl}/subjects/1`, method: 'DELETE' }).flush(null);
    await expectAsync(promise).toBeResolved();
  });
});
