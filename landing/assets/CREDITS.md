# Crédits des ressources du site vitrine

Les fichiers de ce dossier sont produits par `marketing/site-vitrine/generer.py` à partir des
sources du dépôt (voir `docs/refonte-site/SPEC.md` §8). Ne pas les retoucher à la main :
modifier la source puis relancer le script.

## Photos d'illustration (`photos/`)

Trois photos d'élèves d'Afrique de l'Ouest, toutes issues de Wikimedia Commons et placées dans
le domaine public (**CC0 1.0**) : aucune attribution n'est juridiquement exigée, l'usage
commercial est autorisé. Elles sont créditées quand même (légende sous chaque photo, pied de
page et `mentions-legales.html#credits`). Elles ne représentent pas des écoles utilisatrices de
School Manager et ne doivent jamais être présentées comme telles.

| Fichiers produits | Source (dans `marketing/site-vitrine/photos/`) | Œuvre sur Wikimedia Commons | Auteur, lieu |
|---|---|---|---|
| `photos/classe-{480,800,1200}.{avif,webp,jpg}` | `hero-classe.jpg` | [Students reading in a classroom](https://commons.wikimedia.org/wiki/File:Students_reading_in_a_classroom.jpg) | Bright Kwame Ayisi, Amanya (région d'Oti, Ghana) |
| `photos/eleves-lecture-{480,800,1200}.{avif,webp,jpg}` | `scolarite.jpg` | [Pupils studying a book](https://commons.wikimedia.org/wiki/File:Pupils_studying_a_book.jpg) | Bright Kwame Ayisi (Ghana) |
| `photos/cahiers-{480,800,1200}.{avif,webp,jpg}` | `bulletins.jpg` | [Students writing notes](https://commons.wikimedia.org/wiki/File:Students_writing_notes.jpg) | Onwuka Glory (Nigeria) |

Retirées du site lors de la refonte (fichiers supprimés) : `ecriture.jpg`, `mobile-classe.jpg`,
`campus.jpg`.

## Captures d'écran (`captures/`)

Produites depuis `marketing/play-store/capture-*.png`. Données de démonstration : les noms
d'élèves qui y figurent sont fictifs.

## Polices (`fonts/`)

Inter (400, 600) et Outfit (600), sous-ensemble latin des paquets `@fontsource/inter` et
`@fontsource/outfit` 5.3.0. Licence SIL Open Font License 1.1, texte complet dans
`fonts/OFL.txt`.

## Badges des stores (`badges/`)

| Fichier | Source | Conditions |
|---|---|---|
| `badges/app-store-fr.svg` | Reproduction du badge Apple « Télécharger dans l'App Store » (noir, FR), reprise du site précédent. **À remplacer par le fichier officiel** du kit « App Store Marketing Tools » d'Apple, sous le même nom : Apple interdit de recréer soi-même un badge localisé. | Marque Apple Inc. Usage limité au lien vers la fiche App Store (id6822008457), badge non modifié, sans effet au survol, espace libre autour. |
| `badges/google-play-bientot.svg` | Logo Google Play redessiné + texte « Bientôt sur Google Play », extrait sans modification du site précédent. | Google Play et le logo Google Play sont des marques de Google LLC. Non cliquable et atténué tant que l'application Android est en test. À la sortie, remplacer par le badge officiel « Disponible sur Google Play » relié à la fiche. |

## Marque et image de partage

`brand/`, `favicon.ico`, `apple-touch-icon.png` et `og/school-manager-1200x630.jpg` sont
dérivés de `marketing/play-store/icone-512.png` et de `capture-1-feuille-appel.png`.
