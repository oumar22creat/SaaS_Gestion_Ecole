import { environment } from '../../environments/environment';

/**
 * Même rôle que web/src/app/core/support.util.ts : l'abonnement se règle en espèces auprès
 * de l'éditeur, joint par WhatsApp. Dupliqué plutôt que partagé — les deux applications sont
 * deux projets Angular distincts, sans bibliothèque commune (voir docs/ARCHITECTURE.md).
 */
export function whatsAppUrl(message: string): string {
  return `https://wa.me/${environment.supportWhatsApp}?text=${encodeURIComponent(message)}`;
}

/** Numéro affiché en clair, pour qui préfère appeler ou n'a pas WhatsApp. */
export function supportPhoneNumber(): string {
  const digits = environment.supportWhatsApp;
  return `+${digits.slice(0, 3)} ${digits.slice(3).replace(/(\d{2})(?=\d)/g, '$1 ')}`;
}
