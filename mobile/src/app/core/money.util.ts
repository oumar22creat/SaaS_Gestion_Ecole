/**
 * Montants métier stockés en centimes (XOF, devise sans sous-unité — ADR-009). Identique à
 * web/src/app/core/money.util.ts : la même somme doit se lire de la même façon sur le portail
 * de la famille et sur l'écran du comptable.
 */
export function formatMoney(amountCents: number, currency = 'XOF'): string {
  return `${new Intl.NumberFormat('fr-FR').format(amountCents)} ${currency}`;
}
