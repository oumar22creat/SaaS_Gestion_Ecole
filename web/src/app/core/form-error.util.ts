import { AbstractControl } from '@angular/forms';

/**
 * Message affiché sous un champ invalide (docs/DESIGN.md §6 : une bordure rouge ne suffit
 * pas, l'erreur doit être explicite et rattachée au champ).
 *
 * <p>Centralisé plutôt que réécrit dans chaque gabarit : les mêmes règles de validation
 * doivent produire la même phrase partout, sinon l'application explique la même contrainte
 * de trois façons différentes selon l'écran.
 */
export function fieldError(control: AbstractControl | null | undefined): string {
  const errors = control?.errors;
  if (!errors) {
    return '';
  }
  if (errors['required']) {
    return 'Ce champ est obligatoire.';
  }
  if (errors['email']) {
    return 'Adresse e-mail invalide.';
  }
  if (errors['minlength']) {
    return `${errors['minlength'].requiredLength} caractères minimum.`;
  }
  if (errors['maxlength']) {
    return `${errors['maxlength'].requiredLength} caractères maximum.`;
  }
  if (errors['min']) {
    return `Valeur minimale : ${errors['min'].min}.`;
  }
  if (errors['max']) {
    return `Valeur maximale : ${errors['max'].max}.`;
  }
  if (errors['pattern']) {
    return 'Format attendu non respecté.';
  }
  // Un validateur métier peut fournir son propre message plutôt qu'un simple drapeau.
  const custom = Object.values(errors).find((value) => typeof value === 'string');
  return typeof custom === 'string' ? custom : 'Valeur invalide.';
}
