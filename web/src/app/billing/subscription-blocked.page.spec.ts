import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { environment } from '../../environments/environment';
import { SubscriptionBlockedPage } from './subscription-blocked.page';

/**
 * C'est le seul écran que voit un établissement dont l'accès est fermé. S'il n'explique pas
 * la cause ou ne mène nulle part, le client appelle en pensant à une panne.
 */
describe('SubscriptionBlockedPage', () => {
  let fixture: ComponentFixture<SubscriptionBlockedPage>;
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    sessionStorage.clear();
    await TestBed.configureTestingModule({
      imports: [SubscriptionBlockedPage],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    }).compileComponents();
    httpMock = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(SubscriptionBlockedPage);
  });

  afterEach(() => {
    httpMock.verify();
    sessionStorage.clear();
  });

  /** Renseigne l'abonnement renvoyé par l'API, ou simule un refus, puis rend l'écran. */
  async function render(subscription: unknown | null): Promise<HTMLElement> {
    const request = httpMock.expectOne(`${environment.apiUrl}/billing/subscription`);
    if (subscription === null) {
      request.flush({ error: { code: 'ACCESS_DENIED' } }, { status: 403, statusText: 'Forbidden' });
    } else {
      request.flush({ data: subscription });
    }
    await fixture.whenStable();
    fixture.detectChanges();
    return fixture.nativeElement as HTMLElement;
  }

  it('names the cause and points to WhatsApp', async () => {
    const element = await render({
      status: 'EXPIRED',
      planCode: 'STANDARD',
      planName: 'Standard',
      trialEndsAt: null,
      currentPeriodEnd: '2027-09-23T13:29:06Z',
    });

    expect(element.textContent).toContain('Abonnement échu');
    expect(element.textContent).toContain('Standard');
    expect(element.textContent).toContain('23/09/2027');

    const whatsApp = element.querySelector<HTMLAnchorElement>('a[href^="https://wa.me/"]');
    expect(whatsApp).not.toBeNull();
    expect(whatsApp!.href).toContain(environment.supportWhatsApp);
    // Le lien s'ouvre hors de l'application : sans rel="noopener", la page cible garde une
    // référence sur la nôtre.
    expect(whatsApp!.rel).toContain('noopener');
  });

  /**
   * Un enseignant n'a pas le droit de lire l'abonnement de son école : l'appel échoue, et
   * l'écran doit rester utile malgré tout.
   */
  it('still explains the situation when the subscription cannot be read', async () => {
    const element = await render(null);

    expect(element.textContent).toContain('Abonnement échu');
    expect(element.querySelector('a[href^="https://wa.me/"]')).not.toBeNull();
  });
});
