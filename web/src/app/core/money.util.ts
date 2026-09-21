/** Montants métier stockés en centimes (XOF, devise zéro décimale côté Stripe — ADR-009). */
export function formatMoney(amountCents: number, currency = 'XOF'): string {
  return `${new Intl.NumberFormat('fr-FR').format(amountCents)} ${currency}`;
}
