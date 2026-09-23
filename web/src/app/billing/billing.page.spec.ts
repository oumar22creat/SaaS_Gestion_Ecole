import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { environment } from '../../environments/environment';
import { BillingPage } from './billing.page';

/**
 * L'écran Abonnement doit annoncer exactement ce que le site vitrine annonce : un
 * établissement qui compare les deux et y lit deux prix différents n'achète pas. Ces tests
 * fixent la lecture de l'offre publique (180 000 / 750 000 / dès 1 800 000 FCFA par an) sur
 * les montants mensuels que renvoie l'API.
 */
describe('BillingPage', () => {
  let fixture: ComponentFixture<BillingPage>;
  let httpMock: HttpTestingController;

  /** Les trois plans tels que les sert l'API après V61. */
  const plans = [
    {
      code: 'ESSENTIEL',
      name: 'Essentiel',
      priceCents: 15000,
      annualPriceCents: 180000,
      currency: 'XOF',
      maxStudents: 150,
      canteenIncluded: false,
      transportIncluded: false,
      libraryIncluded: false,
      customDomainIncluded: false,
      purchasable: false,
    },
    {
      code: 'STANDARD',
      name: 'Standard',
      priceCents: 62500,
      annualPriceCents: 750000,
      currency: 'XOF',
      maxStudents: 800,
      canteenIncluded: true,
      transportIncluded: true,
      libraryIncluded: true,
      customDomainIncluded: false,
      purchasable: false,
    },
    {
      code: 'PREMIUM',
      name: 'Premium',
      priceCents: 150000,
      annualPriceCents: 1800000,
      currency: 'XOF',
      maxStudents: null,
      canteenIncluded: true,
      transportIncluded: true,
      libraryIncluded: true,
      customDomainIncluded: true,
      purchasable: false,
    },
  ];

  /** Rend l'écran avec les plans ci-dessus et l'abonnement passé en argument. */
  async function render(subscription: unknown | null): Promise<string> {
    fixture = TestBed.createComponent(BillingPage);
    httpMock.expectOne(`${environment.apiUrl}/billing/plans`).flush({ data: plans });
    const subscriptionRequest = httpMock.expectOne(`${environment.apiUrl}/billing/subscription`);
    if (subscription === null) {
      subscriptionRequest.flush(
        { error: { code: 'NO_SUBSCRIPTION' } },
        { status: 404, statusText: 'Not Found' },
      );
    } else {
      subscriptionRequest.flush({ data: subscription });
    }
    await fixture.whenStable();
    fixture.detectChanges();
    // Les espaces insécables des montants formatés (fr-FR) empêchent une comparaison
    // naïve : on les ramène à l'espace ordinaire avant d'assertir.
    return (fixture.nativeElement as HTMLElement).textContent!.replace(/[  ]/g, ' ');
  }

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [BillingPage],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('affiche les tarifs annuels de l’offre publique', async () => {
    const text = await render(null);

    expect(text).toContain('180 000');
    expect(text).toContain('750 000');
    expect(text).toContain('1 800 000');
  });

  it('rappelle l’équivalent mensuel et le prix par élève', async () => {
    const text = await render(null);

    expect(text).toContain('62 500');
    // 750 000 / 800 = 937,5 → 938, ce que le site arrondit à « 940 FCFA par élève ».
    expect(text).toContain('938 FCFA par élève et par an');
  });

  it('présente le plan sans plafond comme un prix plancher', async () => {
    const text = await render(null);

    expect(text).toContain('Effectif illimité');
    expect(text).toContain('dès');
    expect(text).toContain('Tarif dégressif selon l');
  });

  it('distingue les modules absents du plan d’entrée', async () => {
    await render(null);

    const cards = (fixture.nativeElement as HTMLElement).querySelectorAll('.plan-card');
    const essentielModules = cards[0].querySelectorAll('.plan-features li.off');
    const premiumModules = cards[2].querySelectorAll('.plan-features li.off');

    // Essentiel : cantine/transport/bibliothèque et domaine personnalisé sont en retrait.
    expect(essentielModules.length).toBe(2);
    expect(premiumModules.length).toBe(0);
  });

  it('repère le plan en cours sans le proposer à l’achat', async () => {
    const text = await render({
      status: 'ACTIVE',
      planCode: 'STANDARD',
      planName: 'Standard',
      trialEndsAt: null,
      currentPeriodEnd: '2027-07-31',
    });

    expect(text).toContain('Plan actuel');
    const current = (fixture.nativeElement as HTMLElement).querySelectorAll(
      '.plan-card.plan-current',
    );
    expect(current.length).toBe(1);
    expect(current[0].textContent).toContain('Standard');
  });
});
