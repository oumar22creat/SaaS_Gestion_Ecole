import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { environment } from '../../environments/environment';
import { ReportCardService } from './report-card.service';

describe('ReportCardService', () => {
  let service: ReportCardService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(ReportCardService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('lists report cards for a class and period', async () => {
    const promise = service.list(4, 'Trimestre 1');
    const req = httpMock.expectOne(
      `${environment.apiUrl}/report-cards?schoolClassId=4&periodLabel=Trimestre%201`,
    );
    req.flush({ data: [] });

    expect(await promise).toEqual([]);
  });
});
