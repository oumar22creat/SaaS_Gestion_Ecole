import { environment } from '../../environments/environment';
import { supportPhoneNumber, whatsAppUrl } from './support.util';

/**
 * Le lien WhatsApp est la seule issue offerte à un établissement dont l'accès est fermé :
 * s'il est mal formé, la page de blocage ne mène nulle part.
 */
describe('support.util', () => {
  it('builds a wa.me link carrying the prefilled message', () => {
    const url = whatsAppUrl("Bonjour, l'abonnement de mon établissement est échu.");

    expect(url.startsWith(`https://wa.me/${environment.supportWhatsApp}?text=`)).toBe(true);
    // L'apostrophe et les accents doivent survivre à l'encodage : WhatsApp affiche le texte tel quel.
    expect(decodeURIComponent(url.split('?text=')[1])).toBe(
      "Bonjour, l'abonnement de mon établissement est échu.",
    );
  });

  it('displays the number in a form a human can dial', () => {
    expect(supportPhoneNumber()).toBe('+223 79 82 79 79');
  });
});
