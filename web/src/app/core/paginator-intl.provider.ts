import { Provider } from '@angular/core';
import { MatPaginatorIntl } from '@angular/material/paginator';

/**
 * Libellés du paginateur, livrés en anglais par Material : « Items per page », « 1 – 25 of
 * 240 ». Ils sont lus à chaque page de liste, y compris par le secrétariat.
 */
class FrenchPaginatorIntl extends MatPaginatorIntl {
  override itemsPerPageLabel = 'Par page :';
  override nextPageLabel = 'Page suivante';
  override previousPageLabel = 'Page précédente';
  override firstPageLabel = 'Première page';
  override lastPageLabel = 'Dernière page';

  override getRangeLabel = (page: number, pageSize: number, length: number): string => {
    if (length === 0) {
      return '0 sur 0';
    }
    const start = page * pageSize + 1;
    const end = Math.min(start + pageSize - 1, length);
    return `${start} – ${end} sur ${length}`;
  };
}

/**
 * À placer dans le `providers` de chaque écran paginé, plutôt que dans app.config.ts :
 * importer @angular/material/paginator au niveau de l'application le tirerait dans le bundle
 * initial, alors qu'il n'est chargé qu'avec les routes qui affichent une liste.
 */
export const FRENCH_PAGINATOR: Provider = { provide: MatPaginatorIntl, useClass: FrenchPaginatorIntl };
