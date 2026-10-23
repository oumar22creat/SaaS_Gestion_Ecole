import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { TokenReceivedEvent } from '@capacitor-firebase/messaging';
import { environment } from '../../environments/environment';
import { AuthTokenService } from '../auth/auth-token.service';
import {
  MessagingPort,
  PUSH_DELAI_MAX_MS,
  PUSH_MESSAGING,
  PushNotificationService,
} from './push-notification.service';

/** Laisse les promesses en attente se dérouler (appels au greffon, puis requête HTTP). */
const laisserPasser = () => new Promise((resolve) => setTimeout(resolve, 0));

const URL_APPAREILS = `${environment.apiUrl}/device-tokens`;
const DELAI_TEST_MS = 30;

type FauxMessaging = jasmine.SpyObj<MessagingPort>;

function fauxMessaging(permission: 'granted' | 'denied' | 'prompt', jeton = 'jeton-fcm'): FauxMessaging {
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
  function demarrer(messaging: FauxMessaging | null): PushNotificationService {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: PUSH_MESSAGING, useValue: messaging },
        { provide: PUSH_DELAI_MAX_MS, useValue: DELAI_TEST_MS },
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
      httpMock.expectNone(URL_APPAREILS);
    });

    it('does not ask for the permission at first launch either, while it is still undecided', async () => {
      sessionOuverte();
      const messaging = fauxMessaging('prompt');
      const service = demarrer(messaging);

      await service.reprendreAuDemarrage();

      expect(messaging.requestPermissions).not.toHaveBeenCalled();
      httpMock.expectNone(URL_APPAREILS);
    });

    it('does nothing without a session', async () => {
      const messaging = fauxMessaging('granted');
      const service = demarrer(messaging);

      await service.reprendreAuDemarrage();

      expect(messaging.checkPermissions).not.toHaveBeenCalled();
      httpMock.expectNone(URL_APPAREILS);
    });

    it('does nothing on the web, where there is no device token', async () => {
      sessionOuverte();
      const service = demarrer(null);

      await service.reprendreAuDemarrage();
      await service.retirerAvantDeconnexion();

      httpMock.expectNone(() => true);
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
      httpMock.expectNone(() => true);
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

      httpMock.expectNone(URL_APPAREILS);
    });
  });
});
