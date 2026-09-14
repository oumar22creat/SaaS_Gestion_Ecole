import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { environment } from '../../environments/environment';
import { RoomService } from './room.service';

describe('RoomService', () => {
  let service: RoomService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(RoomService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('creates a room', async () => {
    const promise = service.create({ name: 'Salle 101', capacity: 30 });
    const req = httpMock.expectOne(`${environment.apiUrl}/rooms`);
    expect(req.request.method).toBe('POST');
    req.flush({ data: { id: 1, name: 'Salle 101', capacity: 30 } });

    expect(await promise).toEqual({ id: 1, name: 'Salle 101', capacity: 30 });
  });
});
