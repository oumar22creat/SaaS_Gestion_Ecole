import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { TokenReceivedEvent } from '@capacitor-firebase/messaging';
import { environment } from '../../environments/environment';
import { AuthTokenService } from '../auth/auth-token.service';
import { authInterceptor } from '../core/auth.interceptor';
import {
  MessagingPort,
  PUSH_DELAI_FIREBASE_MS,
  PUSH_DELAI_MAX_MS,
  PUSH_MESSAGING,
  PushNotificationService,
} from './push-notification.service';

/** Laisse les promesses en attente se dérouler (appels au greffon, puis requête HTTP). */
const laisserPasser = () => new Promise((resolve) => setTimeout(resolve, 0));

const URL_APPAREILS = `${environment.apiUrl}/device-tokens`;
const DELAI_TEST_MS = 30;
const DELAI_FIREBASE_TEST_MS = 20;

/** Une promesse dont le test décide quand elle aboutit — un appel réseau lent. */
function enAttente<T>(): { promesse: Promise<T>; resoudre: (valeur: T) => void } {
  let resoudre!: (valeur: T) => void;
  const promesse = new Promise<T>((r) => (resoudre = r));
  return { promesse, resoudre };
}

type FauxMessaging = jasmine.SpyObj<MessagingPort>;

function fauxMessaging(
  permission: 'granted' | 'denied' | 'prompt',
  jeton = 'jeton-fcm',
): FauxMessaging {
  const messaging = jasmine.createSpyObj<MessagingPort>('FirebaseMessaging', [
    'checkPermissions',
    'requestPermissions',
    'getToken',
    'deleteToken',
    'addListener',
  ]);
  messaging.checkPermissions.and.resolveTo({ receive: permission });
  messaging.requestPermissions.and.resolveTo({ receive: 'granted' });
  messaging.getToken.and.resolveTo({ token: jeton });
  messaging.deleteToken.and.resolveTo();
  messaging.addListener.and.resolveTo({ remove: () => Promise.resolve() });
  return messaging;
}

describe('PushNotificationService', () => {
  let httpMock: HttpTestingController;
  let authTokenService: AuthTokenService;

  /**
   * Un service neuf, comme au lancement de l'application : rien en mémoire, seule la session
   * stockée dans `localStorage` a survécu.
   */
  function demarrer(
    messaging: FauxMessaging | null,
    { avecIntercepteur = false } = {},
  ): PushNotificationService {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        avecIntercepteur
          ? provideHttpClient(withInterceptors([authInterceptor]))
          : provideHttpClient(),
        provideHttpClientTesting(),
        { provide: PUSH_MESSAGING, useValue: messaging },
        { provide: PUSH_DELAI_MAX_MS, useValue: DELAI_TEST_MS },
        { provide: PUSH_DELAI_FIREBASE_MS, useValue: DELAI_FIREBASE_TEST_MS },
      ],
    });
    httpMock = TestBed.inject(HttpTestingController);
    authTokenService = TestBed.inject(AuthTokenService);
    return TestBed.inject(PushNotificationService);
  }

  function sessionOuverte(): void {
    localStorage.setItem(
      'auth-tokens',
      JSON.stringify({ accessToken: 'h.e30.s', refreshToken: 'r', expiresIn: 900 }),
    );
  }

  beforeEach(() => localStorage.clear());

  afterEach(() => {
    httpMock.verify();
    localStorage.clear();
  });

  describe('au démarrage', () => {
    it('re-registers the device when a session survived the restart', async () => {
      sessionOuverte();
      const messaging = fauxMessaging('granted');
      const service = demarrer(messaging);

      const reprise = service.reprendreAuDemarrage();
      await laisserPasser();
      const req = httpMock.expectOne(URL_APPAREILS);
      expect(req.request.method).toBe('POST');
      expect(req.request.body.token).toBe('jeton-fcm');
      req.flush(null, { status: 204, statusText: 'No Content' });
      await reprise;

      // Le suivi des renouvellements est relancé, comme après une connexion.
      expect(messaging.addListener).toHaveBeenCalledWith('tokenReceived', jasmine.any(Function));
      expect(messaging.requestPermissions).not.toHaveBeenCalled();
    });

    it('never asks again for a permission the user refused', async () => {
      sessionOuverte();
      const messaging = fauxMessaging('denied');
      const service = demarrer(messaging);

      await service.reprendreAuDemarrage();

      expect(messaging.requestPermissions).not.toHaveBeenCalled();
      expect(messaging.getToken).not.toHaveBeenCalled();
      expect(httpMock.match(URL_APPAREILS)).toEqual([]);
    });

    it('does not ask for the permission at first launch either, while it is still undecided', async () => {
      sessionOuverte();
      const messaging = fauxMessaging('prompt');
      const service = demarrer(messaging);

      await service.reprendreAuDemarrage();

      expect(messaging.requestPermissions).not.toHaveBeenCalled();
      expect(httpMock.match(URL_APPAREILS)).toEqual([]);
    });

    it('does nothing without a session', async () => {
      const messaging = fauxMessaging('granted');
      const service = demarrer(messaging);

      await service.reprendreAuDemarrage();

      expect(messaging.checkPermissions).not.toHaveBeenCalled();
      expect(httpMock.match(URL_APPAREILS)).toEqual([]);
    });

    it('does nothing on the web, where there is no device token', async () => {
      sessionOuverte();
      const service = demarrer(null);

      await service.reprendreAuDemarrage();
      await service.retirerAvantDeconnexion();

      expect(httpMock.match(() => true)).toEqual([]);
    });
  });

  it('asks for the permission right after login', async () => {
    sessionOuverte();
    const messaging = fauxMessaging('prompt');
    const service = demarrer(messaging);

    const enregistrement = service.enregistrerApresConnexion();
    await laisserPasser();
    httpMock.expectOne(URL_APPAREILS).flush(null, { status: 204, statusText: 'No Content' });
    await enregistrement;

    expect(messaging.requestPermissions).toHaveBeenCalled();
  });

  describe('à la déconnexion', () => {
    /**
     * Le bogue d'origine : le jeton n'était connu qu'en mémoire, rempli seulement par la page
     * de connexion. Après une réouverture de l'application, la déconnexion ne retirait rien,
     * et l'appareil restait rattaché à l'ancien compte.
     */
    it('removes the device even after a restart, when the token is not in memory', async () => {
      sessionOuverte();
      const messaging = fauxMessaging('granted', 'jeton-apres-redemarrage');
      const service = demarrer(messaging);

      const retrait = service.retirerAvantDeconnexion();
      await laisserPasser();
      const req = httpMock.expectOne(`${URL_APPAREILS}/jeton-apres-redemarrage`);
      expect(req.request.method).toBe('DELETE');
      expect(messaging.deleteToken).not.toHaveBeenCalled();
      req.flush(null, { status: 204, statusText: 'No Content' });
      await retrait;

      // Puis le jeton est invalidé chez Firebase : la connexion suivante en obtiendra un neuf.
      expect(messaging.deleteToken).toHaveBeenCalledTimes(1);
    });

    it('removes the token declared earlier in the same run', async () => {
      sessionOuverte();
      const messaging = fauxMessaging('granted', 'jeton-memoire');
      const service = demarrer(messaging);
      const reprise = service.reprendreAuDemarrage();
      await laisserPasser();
      httpMock.expectOne(URL_APPAREILS).flush(null, { status: 204, statusText: 'No Content' });
      await reprise;
      messaging.getToken.calls.reset();

      const retrait = service.retirerAvantDeconnexion();
      await laisserPasser();
      httpMock
        .expectOne(`${URL_APPAREILS}/jeton-memoire`)
        .flush(null, { status: 204, statusText: 'No Content' });
      await retrait;

      expect(messaging.getToken).not.toHaveBeenCalled();
    });

    it('is not blocked when the network fails', async () => {
      sessionOuverte();
      const messaging = fauxMessaging('granted');
      const service = demarrer(messaging);

      const retrait = service.retirerAvantDeconnexion();
      await laisserPasser();
      httpMock
        .expectOne(`${URL_APPAREILS}/jeton-fcm`)
        .error(new ProgressEvent('error'), { status: 0, statusText: 'Unknown Error' });

      await expectAsync(retrait).toBeResolved();
      expect(messaging.deleteToken).toHaveBeenCalled();
    });

    it('gives up after the maximum delay when the server never answers', async () => {
      sessionOuverte();
      const messaging = fauxMessaging('granted');
      const service = demarrer(messaging);

      const retrait = service.retirerAvantDeconnexion();
      await laisserPasser();
      const req = httpMock.expectOne(`${URL_APPAREILS}/jeton-fcm`);

      await expectAsync(retrait).toBeResolved();
      expect(messaging.deleteToken).toHaveBeenCalled();
      // La requête elle-même est annulée à son propre délai, sans rester ouverte.
      await new Promise((resolve) => setTimeout(resolve, DELAI_TEST_MS + 10));
      expect(req.cancelled).toBeTrue();
      expect(messaging.deleteToken).toHaveBeenCalled();
    });

    it('is not blocked when Firebase itself hangs', async () => {
      sessionOuverte();
      const messaging = fauxMessaging('granted');
      messaging.getToken.and.returnValue(new Promise(() => undefined));
      messaging.deleteToken.and.returnValue(new Promise(() => undefined));
      const service = demarrer(messaging);

      await expectAsync(service.retirerAvantDeconnexion()).toBeResolved();
      expect(httpMock.match(() => true)).toEqual([]);
    });

    /**
     * `deleteToken()` pousse Firebase à émettre un jeton neuf. S'il était déclaré alors que la
     * session est déjà effacée, il partirait sans authentification — ou pire, se rattacherait
     * au compte suivant avant même qu'il ait accepté les notifications.
     */
    it('does not declare a renewed token once logged out', async () => {
      sessionOuverte();
      const messaging = fauxMessaging('granted');
      const service = demarrer(messaging);
      const reprise = service.reprendreAuDemarrage();
      await laisserPasser();
      httpMock.expectOne(URL_APPAREILS).flush(null, { status: 204, statusText: 'No Content' });
      await reprise;
      const ecouteur = messaging.addListener.calls.mostRecent().args[1];

      authTokenService.clear();
      ecouteur({ token: 'jeton-renouvele' } as TokenReceivedEvent);
      await laisserPasser();

      expect(httpMock.match(URL_APPAREILS)).toEqual([]);
      expect(authTokenService.read()).toBeNull();
    });
    it('caps the whole logout even when everything hangs', async () => {
      sessionOuverte();
      const messaging = fauxMessaging('granted');
      messaging.getToken.and.returnValue(new Promise(() => undefined));
      messaging.deleteToken.and.returnValue(new Promise(() => undefined));
      const service = demarrer(messaging);

      const debut = performance.now();
      await service.retirerAvantDeconnexion();
      const duree = performance.now() - debut;

      // Plafond : retrait serveur + invalidation Firebase, et pas une attente par étape.
      expect(duree).toBeLessThan(DELAI_TEST_MS + DELAI_FIREBASE_TEST_MS + 40);
      expect(httpMock.match(() => true)).toEqual([]);
    });

    /**
     * Reprise au démarrage encore en cours (réseau lent) quand l'utilisateur se déconnecte :
     * la déconnexion l'attend, pour retirer le jeton qu'elle va déclarer et ne pas laisser sa
     * déclaration arriver après le retrait.
     */
    it('waits for a registration in progress before removing the device', async () => {
      sessionOuverte();
      const messaging = fauxMessaging('granted');
      const jeton = enAttente<{ token: string }>();
      messaging.getToken.and.returnValue(jeton.promesse);
      const service = demarrer(messaging);
      void service.reprendreAuDemarrage();
      await laisserPasser();

      const retrait = service.retirerAvantDeconnexion();
      await laisserPasser();
      expect(httpMock.match(() => true)).toEqual([]);

      jeton.resoudre({ token: 'jeton-lent' });
      await laisserPasser();
      // La déclaration de la reprise n'a pas lieu : le compte est en train de partir.
      expect(httpMock.match({ method: 'POST', url: URL_APPAREILS })).toEqual([]);
      httpMock
        .expectOne(`${URL_APPAREILS}/jeton-lent`)
        .flush(null, { status: 204, statusText: 'No Content' });
      await retrait;

      expect(messaging.deleteToken).toHaveBeenCalled();
    });

    it('does not declare the device once the session was cleared during registration', async () => {
      sessionOuverte();
      const messaging = fauxMessaging('granted');
      const jeton = enAttente<{ token: string }>();
      messaging.getToken.and.returnValue(jeton.promesse);
      const service = demarrer(messaging);
      const reprise = service.reprendreAuDemarrage();
      await laisserPasser();

      authTokenService.clear();
      jeton.resoudre({ token: 'jeton-tardif' });
      await reprise;

      expect(httpMock.match(URL_APPAREILS)).toEqual([]);
    });

    /**
     * Un retrait abandonné au délai continue en arrière-plan. Si un autre compte s'est
     * connecté entre-temps, sa requête ne doit pas partir : elle porterait le jeton de session
     * du nouveau compte et lui retirerait l'appareil.
     */
    it('drops a late removal once another account has logged in', async () => {
      sessionOuverte();
      const messaging = fauxMessaging('granted');
      const jeton = enAttente<{ token: string }>();
      messaging.getToken.and.returnValue(jeton.promesse);
      const service = demarrer(messaging);

      await service.retirerAvantDeconnexion();
      authTokenService.clear();
      authTokenService.store({ accessToken: 'h.e30.s', refreshToken: 'autre', expiresIn: 900 });
      jeton.resoudre({ token: 'jeton-fcm' });
      await laisserPasser();

      expect(httpMock.match(() => true)).toEqual([]);
    });

    /**
     * Pendant `deleteToken()`, la session existe encore. Firebase peut émettre un jeton neuf à
     * ce moment-là : il ne doit pas être redéclaré pour le compte qui s'en va.
     */
    it('does not declare a token renewed while the token is being deleted', async () => {
      sessionOuverte();
      const messaging = fauxMessaging('granted');
      const service = demarrer(messaging);
      const reprise = service.reprendreAuDemarrage();
      await laisserPasser();
      httpMock.expectOne(URL_APPAREILS).flush(null, { status: 204, statusText: 'No Content' });
      await reprise;
      const ecouteur = messaging.addListener.calls.mostRecent().args[1];
      messaging.deleteToken.and.callFake(async () => {
        ecouteur({ token: 'jeton-emis-pendant-la-suppression' } as TokenReceivedEvent);
      });

      const retrait = service.retirerAvantDeconnexion();
      await laisserPasser();
      httpMock
        .expectOne(`${URL_APPAREILS}/jeton-fcm`)
        .flush(null, { status: 204, statusText: 'No Content' });
      await retrait;
      await laisserPasser();

      expect(messaging.deleteToken).toHaveBeenCalled();
      expect(authTokenService.isAuthenticated()).toBeTrue();
      expect(httpMock.match({ method: 'POST', url: URL_APPAREILS })).toEqual([]);
    });

    it('declares again after the next login', async () => {
      sessionOuverte();
      const messaging = fauxMessaging('granted');
      const service = demarrer(messaging);
      await service.retirerAvantDeconnexion();
      expect(httpMock.match(() => true).length).toBe(1);

      const connexion = service.enregistrerApresConnexion();
      await laisserPasser();
      httpMock.expectOne(URL_APPAREILS).flush(null, { status: 204, statusText: 'No Content' });
      await expectAsync(connexion).toBeResolved();
    });

    /**
     * Régression relevée en relecture : jeton d'accès expiré, DELETE refusé en 401, le
     * renouvellement part et traîne au-delà du délai. La déconnexion se poursuit et efface la
     * session — puis la réponse du renouvellement arrivait et la réécrivait. Au lancement
     * suivant, la personne d'après retrouvait le compte précédent.
     */
    it('does not let a slow session renewal resurrect the session after logout', async () => {
      sessionOuverte();
      const messaging = fauxMessaging('granted', 'tok');
      const service = demarrer(messaging, { avecIntercepteur: true });

      const retrait = service.retirerAvantDeconnexion();
      await laisserPasser();
      httpMock
        .expectOne(`${URL_APPAREILS}/tok`)
        .flush('expired', { status: 401, statusText: 'Unauthorized' });
      await laisserPasser();
      const renouvellement = httpMock.expectOne(`${environment.apiUrl}/auth/refresh`);
      await retrait;
      authTokenService.clear(); // HomePage.logout

      renouvellement.flush({ data: { accessToken: 'NEW', refreshToken: 'NEW-R', expiresIn: 900 } });
      await laisserPasser();

      expect(authTokenService.read()).toBeNull();
      expect(localStorage.getItem('auth-tokens')).toBeNull();
      // Et le DELETE n'est pas rejoué avec un jeton de session périmé.
      expect(httpMock.match(`${URL_APPAREILS}/tok`)).toEqual([]);
    });
  });
});
