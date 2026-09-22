import { Provider } from '@angular/core';
import { MAT_DATE_LOCALE, provideNativeDateAdapter } from '@angular/material/core';
import { MatDatepickerIntl } from '@angular/material/datepicker';

/**
 * Libellés du calendrier, livrés en anglais par Material. Ils ne sont pas décoratifs : ce
 * sont les infobulles des flèches de navigation et ce que lit un lecteur d'écran.
 */
class FrenchDatepickerIntl extends MatDatepickerIntl {
  override calendarLabel = 'Calendrier';
  override openCalendarLabel = 'Ouvrir le calendrier';
  override closeCalendarLabel = 'Fermer le calendrier';
  override prevMonthLabel = 'Mois précédent';
  override nextMonthLabel = 'Mois suivant';
  override prevYearLabel = 'Année précédente';
  override nextYearLabel = 'Année suivante';
  override prevMultiYearLabel = '24 années précédentes';
  override nextMultiYearLabel = '24 années suivantes';
  override switchToMonthViewLabel = 'Choisir une date';
  override switchToMultiYearViewLabel = 'Choisir un mois et une année';
}

/**
 * Sélecteurs de date en français. Sans la locale, Material affiche et interprète les dates
 * au format américain : une naissance le 2 janvier 2012 s'affichait « 1/2/2012 », que tout
 * le monde lit ici comme le 1er février.
 *
 * L'adaptateur de date est fourni ici, et non repris de `MatNativeDateModule` : les
 * fournisseurs d'un NgModule importé par un composant autonome remontent à l'injecteur
 * d'environnement, où un MAT_DATE_LOCALE déclaré au niveau du composant n'est pas visible —
 * la locale y serait donc ignorée.
 *
 * À placer dans le `providers` de chaque écran qui utilise un sélecteur de date, plutôt que
 * dans app.config.ts : importer @angular/material/core au niveau de l'application le tire
 * dans le bundle initial (mesuré à +65 ko brut, +18 ko transférés) alors qu'il n'est sinon
 * chargé qu'avec les routes qui en ont besoin.
 */
export const FRENCH_DATE_LOCALE: Provider[] = [
  { provide: MAT_DATE_LOCALE, useValue: 'fr-FR' },
  { provide: MatDatepickerIntl, useClass: FrenchDatepickerIntl },
  provideNativeDateAdapter(),
];
