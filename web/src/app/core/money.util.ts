/** Montants métier stockés en centimes (XOF, devise zéro décimale côté Stripe — ADR-009). */
export function formatMoney(amountCents: number, currency?: string | null): string {
  return `${new Intl.NumberFormat('fr-FR').format(amountCents)} ${currencyLabel(currency)}`;
}

/**
 * Libellé affiché pour un code devise ISO 4217. Le code XOF reste celui échangé avec l'API ;
 * seules les familles et les écoles voient « FCFA », la forme d'usage courant en Afrique de l'Ouest.
 * Une devise absente (null, vide) vaut XOF : la console plateforme renvoie null sans plan actif,
 * et le paramètre par défaut d'une fonction ne s'applique pas à null.
 */
export function currencyLabel(currency?: string | null): string {
  const code = (currency || 'XOF').toUpperCase();
  return code === 'XOF' ? 'FCFA' : code;
}
