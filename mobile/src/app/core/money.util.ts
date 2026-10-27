/**
 * Montants métier stockés en centimes (code devise XOF côté API, devise sans sous-unité —
 * ADR-009). L'application affiche « FCFA », le libellé que lisent les familles, et non le code
 * ISO : la même somme doit se lire de la même façon sur le portail de la famille et sur l'écran
 * du comptable.
 */
export function formatMoney(amountCents: number, currency: string | null | undefined = 'XOF'): string {
  return `${new Intl.NumberFormat('fr-FR').format(amountCents)} ${currencyLabel(currency)}`;
}

/**
 * Libellé affiché pour un code devise ISO 4217, aligné sur le web et le backend : devise absente
 * ou vide → FCFA (devise par défaut des écoles), XOF quelle que soit la casse → FCFA, tout autre
 * code affiché tel quel en majuscules.
 */
export function currencyLabel(currency: string | null | undefined): string {
  const code = (currency ?? '').trim().toUpperCase();
  if (code === '' || code === 'XOF') {
    return 'FCFA';
  }
  return code;
}
