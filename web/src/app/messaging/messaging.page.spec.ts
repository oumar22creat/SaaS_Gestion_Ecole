import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { environment } from '../../environments/environment';
import { AuthTokenService } from '../auth/auth-token.service';
import { MessagingPage } from './messaging.page';

/**
 * ADR-019 : annonce réservée à ADMIN/DIRECTION ; nouvelle conversation ouverte à tout le
 * personnel, avec l'annuaire GET /conversations/recipients (plafond de 30 destinataires).
 */
describe('MessagingPage', () => {
  let fixture: ComponentFixture<MessagingPage>;
  let httpMock: HttpTestingController;
  let role: string;
  let recipients: unknown[];

  const STAFF = ['ADMIN', 'DIRECTION', 'TEACHER', 'SECRETARY', 'VIE_SCOLAIRE', 'ACCOUNTANT'];
  const RECIPIENTS_URL = `${environment.apiUrl}/conversations/recipients?size=200`;

  function recipient(id: number, role = 'TEACHER') {
    return { id, firstName: 'Prénom', lastName: `Nom${id}`, displayName: `Prénom Nom${id}`, role };
  }

  type PageApi = {
    participantIds: { set: (ids: number[]) => void };
    create: (announcement: boolean) => Promise<void>;
    onSearch: (term: string) => void;
  };

  beforeEach(() => {
    recipients = [recipient(2), recipient(3, 'SECRETARY')];
  });

  const announcement = {
    id: 5,
    title: 'Rentrée',
    announcement: true,
    createdByUserId: 1,
    createdAt: '2026-09-01T08:00:00Z',
    participantUserIds: [],
    unreadCount: 0,
  };

  async function render(): Promise<HTMLElement> {
    await TestBed.configureTestingModule({
      imports: [MessagingPage],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: AuthTokenService, useValue: { role: () => role } },
      ],
    }).compileComponents();
    httpMock = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(MessagingPage);
    httpMock
      .expectOne((r) => r.url === `${environment.apiUrl}/conversations`)
      .flush({ data: [announcement] });
    if (STAFF.includes(role)) {
      httpMock.expectOne(RECIPIENTS_URL).flush({ data: recipients });
    }
    await fixture.whenStable();
    fixture.detectChanges();
    return fixture.nativeElement as HTMLElement;
  }

  afterEach(() => httpMock.verify());

  for (const staffRole of ['TEACHER', 'SECRETARY', 'VIE_SCOLAIRE', 'ACCOUNTANT']) {
    it(`${staffRole} : nouvelle conversation avec l’annuaire, sans annonce`, async () => {
      role = staffRole;
      const el = await render();

      expect(el.textContent).toContain('Nouvelle conversation');
      expect(el.textContent).not.toContain('Annonce');
      expect(el.querySelector('[data-testid="recipient-count"]')?.textContent).toContain('0 / 30');
    });
  }

  for (const directionRole of ['ADMIN', 'DIRECTION']) {
    it(`${directionRole} : nouvelle conversation et annonce proposées`, async () => {
      role = directionRole;
      const el = await render();

      expect(el.textContent).toContain('Nouvelle conversation');
      expect(el.textContent).toContain('Annonce');
    });
  }

  for (const familyRole of ['PARENT', 'STUDENT']) {
    it(`${familyRole} : ni formulaire ni appel à l’annuaire`, async () => {
      role = familyRole;
      const el = await render();

      expect(el.textContent).not.toContain('Nouvelle conversation');
      httpMock.expectNone((r) => r.url.includes('/recipients'));
    });
  }

  it('TEACHER : la conversation part vers les destinataires choisis', async () => {
    role = 'TEACHER';
    await render();
    const page = fixture.componentInstance as unknown as PageApi;
    page.participantIds.set([2, 3]);

    const done = page.create(false);
    const req = httpMock.expectOne(
      (r) => r.method === 'POST' && r.url === `${environment.apiUrl}/conversations`,
    );
    expect(req.request.body).toEqual({
      title: '',
      announcement: false,
      participantUserIds: [2, 3],
    });
    req.flush({ data: { ...announcement, id: 9, announcement: false } });
    await new Promise((resolve) => setTimeout(resolve));
    httpMock.expectOne((r) => r.url === `${environment.apiUrl}/conversations`).flush({ data: [] });
    await done;
  });

  it('SECRETARY : au-delà de 30 destinataires, message clair et aucun envoi', async () => {
    role = 'SECRETARY';
    recipients = Array.from({ length: 31 }, (_, i) => recipient(i + 10));
    await render();
    const page = fixture.componentInstance as unknown as PageApi;
    page.participantIds.set(Array.from({ length: 31 }, (_, i) => i + 10));
    fixture.detectChanges();

    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('[data-testid="too-many-recipients"]')?.textContent).toContain(
      'au plus 30 destinataires',
    );
    expect(el.textContent).toContain('demandez une annonce à la direction');
    const button = [...el.querySelectorAll('button')].find((b) =>
      b.textContent?.includes('Nouvelle conversation'),
    ) as HTMLButtonElement;
    expect(button.disabled).toBe(true);

    await page.create(false);
    httpMock.expectNone((r) => r.method === 'POST');
  });

  it('VIE_SCOLAIRE : la recherche interroge l’annuaire par nom', async () => {
    role = 'VIE_SCOLAIRE';
    await render();
    const page = fixture.componentInstance as unknown as PageApi;
    jasmine.clock().install();
    try {
      page.onSearch('dupont');
      jasmine.clock().tick(300);
    } finally {
      jasmine.clock().uninstall();
    }
    const req = httpMock.expectOne((r) => r.url.endsWith('/conversations/recipients'));
    expect(req.request.params.get('search')).toBe('dupont');
    expect(req.request.params.get('size')).toBe('200');
    req.flush({ data: [] });
  });

  it('TEACHER : une annonce s’affiche en lecture seule', async () => {
    role = 'TEACHER';
    await render();
    const page = fixture.componentInstance as unknown as {
      active: { set: (c: unknown) => void };
    };
    page.active.set(announcement);
    fixture.detectChanges();

    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('[data-testid="announcement-read-only"]')).not.toBeNull();
    expect(el.textContent).not.toContain('Envoyer');
  });
});
