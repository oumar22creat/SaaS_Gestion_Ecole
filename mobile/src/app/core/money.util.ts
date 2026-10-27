/**
 * Montants métier stockés en centimes (code devise XOF côté API, devise sans sous-unité —
 * ADR-009). L'application affiche « FCFA », le libellé que lisent les familles, et non le code
 * ISO : la même somme doit se lire de la même façon sur le portail de la famille et sur l'écran
 * du comptable.
 */
const CURRENCY_LABELS: Record<string, string> = { XOF: 'FCFA' };

export function formatMoney(amountCents: number, currency = 'XOF'): string {
  const label = CURRENCY_LABELS[currency] ?? currency;
  return `${new Intl.NumberFormat('fr-FR').format(amountCents)} ${label}`;
}
