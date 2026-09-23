import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { environment } from '../../environments/environment';
import { ParentService } from './parent.service';

describe('ParentService', () => {
  let service: ParentService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(ParentService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('creates a parent', async () => {
    const request = { firstName: 'Claire', lastName: 'Martin', email: null, phone: null };
    const promise = service.create(request);
    const req = httpMock.expectOne(`${environment.apiUrl}/parents`);
    expect(req.request.method).toBe('POST');
    // Le serveur renvoie en plus l'adresse de connexion au portail, nulle pour une fiche neuve.
    req.flush({ data: { id: 1, ...request, portalEmail: null } });

    expect(await promise).toEqual({ id: 1, ...request, portalEmail: null });
  });

  it('links a parent to a student', async () => {
    const promise = service.link(5, { parentId: 1, relationship: 'MERE', primaryContact: true });
    const req = httpMock.expectOne(`${environment.apiUrl}/students/5/parents`);
    expect(req.request.method).toBe('POST');
    req.flush({ data: { studentId: 5, parentId: 1, relationship: 'MERE', primaryContact: true } });

    expect(await promise).toEqual({
      studentId: 5,
      parentId: 1,
      relationship: 'MERE',
      primaryContact: true,
    });
  });
});
