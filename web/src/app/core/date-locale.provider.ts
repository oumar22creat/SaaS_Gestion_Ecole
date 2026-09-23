import { Provider } from '@angular/core';
import { DateAdapter, MAT_DATE_LOCALE, NativeDateAdapter, provideNativeDateAdapter } from '@angular/material/core';
import { MatDatepickerIntl } from '@angular/material/datepicker';

/** jj/mm/aaaa, avec / - ou . comme séparateur — les trois se tapent au clavier. */
const FRENCH_DATE_PATTERN = /^(\d{1,2})[/\-.](\d{1,2})[/\-.](\d{4})$/;

/**
 * Adaptateur de date français.
 *
 * <p>`MAT_DATE_LOCALE` corrige l'AFFICHAGE, mais pas la LECTURE : l'adaptateur natif de
 * Material analyse la saisie avec `Date.parse`, qui interprète toujours « 01/10/2027 » à
 * l'américaine — le 10 janvier au lieu du 1er octobre. Le champ réaffiche ensuite une date
 * française parfaitement plausible, si bien que l'erreur passe inaperçue : une date de
 * naissance, une échéance de paiement ou une date d'évaluation partait fausse sans que
 * personne ne le voie.
 *
 * <p>Ne concerne que la saisie au clavier — choisir une date dans le calendrier a toujours
 * fonctionné, ce qui rendait le défaut d'autant plus discret.
 */
class FrenchDateAdapter extends NativeDateAdapter {
  override parse(value: unknown): Date | null {
    if (typeof value !== 'string') {
      return super.parse(value);
    }
    const match = FRENCH_DATE_PATTERN.exec(value.trim());
    if (!match) {
      return super.parse(value);
    }
    const [, day, month, year] = match;
    const parsed = new Date(Number(year), Number(month) - 1, Number(day));
    // Rejette « 31/02/2027 » : JS le décalerait silencieusement au 3 mars.
    return parsed.getDate() === Number(day) && parsed.getMonth() === Number(month) - 1
      ? parsed
      : null;
  }
}

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
  // Après provideNativeDateAdapter() : c'est ce qui permet de remplacer l'adaptateur natif
  // sans perdre les formats de date qu'il fournit.
  { provide: DateAdapter, useClass: FrenchDateAdapter, deps: [MAT_DATE_LOCALE] },
];
