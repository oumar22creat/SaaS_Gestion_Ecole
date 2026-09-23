import { environment } from '../../environments/environment';

/**
 * Lien vers la discussion WhatsApp de l'éditeur.
 *
 * <p>La plateforme n'encaisse rien en ligne : l'abonnement se règle en espèces, puis le
 * Super-Administrateur l'enregistre. Toutes les demandes d'un établissement — souscrire,
 * renouveler, comprendre pourquoi son accès est fermé — passent donc par une conversation,
 * et wa.me est le canal que les établissements utilisent déjà au Mali.
 *
 * <p>Le message est pré-rempli pour que l'éditeur sache de quelle école il s'agit dès le
 * premier message : sans cela, il reçoit « bonjour » d'un numéro inconnu.
 */
export function whatsAppUrl(message: string): string {
  return `https://wa.me/${environment.supportWhatsApp}?text=${encodeURIComponent(message)}`;
}

/** Numéro affiché en clair, pour qui préfère appeler ou n'a pas WhatsApp. */
export function supportPhoneNumber(): string {
  // 22379827979 → +223 79 82 79 79
  const digits = environment.supportWhatsApp;
  const country = digits.slice(0, 3);
  const rest = digits.slice(3).replace(/(\d{2})(?=\d)/g, '$1 ');
  return `+${country} ${rest}`;
}
