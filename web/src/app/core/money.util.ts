/** Montants métier stockés en centimes (XOF, devise zéro décimale côté Stripe — ADR-009). */
export function formatMoney(amountCents: number, currency = 'XOF'): string {
  return `${new Intl.NumberFormat('fr-FR').format(amountCents)} ${currencyLabel(currency)}`;
}

/**
 * Libellé affiché pour un code devise ISO 4217. Le code XOF reste celui échangé avec l'API ;
 * seules les familles et les écoles voient « FCFA », la forme d'usage courant en Afrique de l'Ouest.
 */
export function currencyLabel(currency: string): string {
  return currency.toUpperCase() === 'XOF' ? 'FCFA' : currency;
}
