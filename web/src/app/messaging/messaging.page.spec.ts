import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { environment } from '../../environments/environment';
import { AuthTokenService } from '../auth/auth-token.service';
import { MessagingPage } from './messaging.page';

/**
 * ADR-019 : annonce réservée à ADMIN/DIRECTION ; la liste des destinataires (GET /users)
 * aussi — les autres membres du personnel ne voient pas un formulaire sans destinataire.
 */
describe('MessagingPage', () => {
  let fixture: ComponentFixture<MessagingPage>;
  let httpMock: HttpTestingController;
  let role: string;

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
    httpMock.expectOne((r) => r.url === `${environment.apiUrl}/conversations`).flush({ data: [announcement] });
    if (role === 'ADMIN' || role === 'DIRECTION') {
      httpMock.expectOne((r) => r.url === `${environment.apiUrl}/users`).flush({ data: [] });
    }
    await fixture.whenStable();
    fixture.detectChanges();
    return fixture.nativeElement as HTMLElement;
  }

  afterEach(() => httpMock.verify());

  for (const staffRole of ['TEACHER', 'SECRETARY', 'VIE_SCOLAIRE', 'ACCOUNTANT']) {
    it(`${staffRole} : ni annonce ni nouvelle conversation, avec un message clair`, async () => {
      role = staffRole;
      const el = await render();

      expect(el.textContent).not.toContain('Annonce');
      expect(el.textContent).not.toContain('Nouvelle conversation');
      expect(el.querySelector('[data-testid="no-new-conversation"]')).not.toBeNull();
    });
  }

  it('DIRECTION : nouvelle conversation et annonce proposées', async () => {
    role = 'DIRECTION';
    const el = await render();

    expect(el.textContent).toContain('Nouvelle conversation');
    expect(el.textContent).toContain('Annonce');
    expect(el.querySelector('[data-testid="no-new-conversation"]')).toBeNull();
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
