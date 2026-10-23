# Relecture visuelle du site vitrine (étape 11)

Relecteur : agent-designer, 23/10/2026, branche `feature/refonte-site`.
Périmètre : `landing/index.html`, `fonctionnalites.html`, `parents.html`, `tarifs.html`,
`securite.html`, `mentions-legales.html`, `404.html`, `assets/site.v1.css`, `assets/site.v1.js`,
comparés à `docs/refonte-site/SPEC.md` et `docs/refonte-site/pages/*.md`.
Hors périmètre (corrigé en parallèle par un autre agent) : `confidentialite.html` et les mentions
de l'essai.

Méthode : relecture du HTML et du CSS sans navigateur. Les largeurs de texte citées
(en-tête, tableaux) sont des **estimations** à partir des métriques d'Inter et d'Outfit : elles
doivent être confirmées dans les outils de développement (mode appareil 320, 360, 375, 768, 1280).

## Synthèse

| Niveau | Nombre |
|---|---|
| BLOQUANT | 0 |
| À CORRIGER | 4 |
| MINEUR | 11 |

L'implémentation suit fidèlement la spécification : les tokens du §4 sont repris tels quels
dans `:root`, les contrastes sont respectés (l'or n'est jamais utilisé en texte ni en focus sur
fond clair), l'anneau de focus est vert sur fond clair et or sur fond nuit, et il est toujours
dessiné avec `outline`. Les points de rupture sont en `em` et l'approche est bien mobile
d'abord. L'ordre des titres est correct sur les 7 pages (un seul `h1`, aucun niveau sauté,
titres du pied de page en `h2`). Les textes alternatifs et les légendes sont conformes aux
fiches. Les espaces insécables sont présents partout dans le texte visible. Aucun attribut
`style` n'est utilisé et aucune ressource tierce n'est chargée. Le menu fonctionne sans
JavaScript.

---

## BLOQUANT

Aucun.

---

## À CORRIGER

### C1. En-tête trop serré entre 360 et environ 370 px (toutes les pages)

**Statut : Corrigé (fcf2905)** : `nowrap`, marque en 15 px, écart de 8 px sous 440 px ; SPEC §6.1 mise à jour. Mesuré dans Chrome : « Essai gratuit » sur une ligne (44 px de haut) à 320, 360 et 375 px ; 14,8 px libres entre la marque et les actions à 360 px, 29,8 px à 375 px. Le seuil de masquage du nom reste à 360 px.

- **Fichier** : `landing/assets/site.v1.css`, l. 376-394 (paliers `max-width: 27.4375em` et
  `22.4375em`) ; en-tête commun des 7 pages, l. 61-94.
- **Constat** : à 360 px (la largeur Android la plus courante), le nom « School Manager »
  reste visible (il n'est masqué que sous 360 px). Somme estimée : marque 30 + 8 + ≈ 116 px,
  écart 12 px, bouton « Essai gratuit » ≈ 111 px (texte 14 px, `padding .8rem`, bordure 2 × 2),
  écart 8 px, bouton Menu 44 px, soit **≈ 329 px pour 328 px disponibles**
  (360 − 2 × 16). Le bouton n'a pas de `white-space: nowrap` : c'est lui qui rétrécit, et
  « Essai gratuit » passe alors sur deux lignes dans une pilule de 51 px de haut. Avec la police
  de repli (Arial ajustée), l'écart peut être plus grand.
- **Correction attendue** : dans le palier `@media (max-width: 27.4375em)` :
  - `.topbar .btn-sm { white-space: nowrap; }`
  - `.brand { font-size: .9375rem; }` (15 px au lieu de 16, soit environ 7 px de gagnés)
  - `.topbar-inner { gap: var(--s-2); }` (8 px au lieu de 12)

  Vérifier ensuite à 360 et 375 px qu'il reste au moins 8 px de marge. Si ce n'est pas le cas,
  passer le seuil de `.brand-name` masqué de `22.4375em` à `23.4375em` (moins de 375 px) et
  mettre à jour SPEC §6.1 dans le même commit.

### C2. Tableau des 8 rôles : il défile à 320 et 375 px alors que la fiche demande qu'il tienne

**Statut : Corrigé (fcf2905)** : palier sous 600 px appliqué tel que proposé. Mesuré : `scrollWidth` = `clientWidth` du conteneur à 320 (288 px), 360 (328 px) et 375 px (343 px).

- **Fichiers** : `landing/fonctionnalites.html`, l. 303-318 ; `landing/assets/site.v1.css`,
  l. 832 (`.roles-table td:nth-child(2) { white-space: nowrap }`).
- **Constat** : la fiche `fonctionnalites.md` §9 dit : « à 320 / 375, le tableau tient sans
  défilement (colonne « Où » en 14 px) ». Avec `nowrap`, la cellule « Web et application »
  mesure ≈ 130 px. S'y ajoutent « Administrateur » en 600 (≈ 115 px) et le mot le plus long de
  la 3e colonne (≈ 100 px), plus les marges internes (60 px) : ≈ 400 px pour 288 à 343 px
  disponibles. Le conteneur défile donc (sans casser la page), mais une colonne essentielle est
  coupée à l'écran.
- **Correction attendue** : ajouter un palier sous 600 px :
  ```css
  @media (max-width: 37.4375em) {
    .roles-table { font-size: .8125rem; }
    .roles-table th, .roles-table td { padding: 10px 6px; }
    .roles-table th:first-child { padding-left: 10px; }
    .roles-table td:nth-child(2) { white-space: normal; }
    .roles-table td:last-child { hyphens: auto; }
  }
  ```
  Vérifier à 320 px que la propriété `scrollWidth` du conteneur `.table-scroll` est égale à sa
  `clientWidth`.

### C3. Le badge App Store est encore la reproduction, pas le fichier officiel d'Apple

**Statut : Non traité, en attente** : Oumar fournira le fichier officiel.

- **Fichier** : `landing/assets/badges/app-store-fr.svg`, l. 1 (le commentaire
  « À REMPLACER par le fichier officiel » est encore là).
- **Constat** : SPEC §8.6 exige le fichier officiel du kit « App Store Marketing Tools »
  (Apple interdit de recréer soi-même un badge localisé). Le SVG actuel dessine son texte en
  `<text>` avec les polices du système : chargé par `<img>`, il s'affiche avec une police
  différente selon l'appareil (pas de SF Pro sur Android ni sur Windows).
- **Correction attendue** : remplacer le fichier, sous le même nom, par le SVG officiel
  « Télécharger dans l'App Store » (noir, FR), **avant la mise en production**. Ne rien changer
  au HTML : `width="135" height="40"` et la hauteur affichée de 44 px restent valables.

### C4. Mentions légales : aucun espacement autour des blocs `address`

**Statut : Corrigé (fcf2905)** : mesuré à 375 px, 12 px avant chaque adresse et 16 px après (48 px avant le `h2` suivant).

- **Fichiers** : `landing/mentions-legales.html`, l. 116-128 ; `landing/assets/site.v1.css`,
  l. 999 (`.legal address { font-style: normal; }` seulement).
- **Constat** : la règle `.legal p + p` ne s'applique pas à un `<address>`. Résultat : la ligne
  « www.hostinger.com » est collée à « Le serveur est situé dans l'Union européenne. »
  (0 px), puis « …assurés par : » est collé à l'adresse de Cloudflare. Le rythme vertical est
  incohérent avec le reste du texte juridique (16 px entre paragraphes).
- **Correction attendue** : `.legal address { font-style: normal; margin: var(--s-3) 0 var(--s-4); }`.

---

## MINEUR

### M1. Capture d'arrière-plan du haut de page : bord inférieur coupé qui flotte

**Statut : Corrigé (fcf2905)**.

- **Fichier** : `site.v1.css`, l. 507-519 (`.shot-back`).
- **Constat** : la capture est remontée de 48 px (`translateY(-48px)`) mais garde ses coins
  inférieurs à 0 (`!important`). Son bas droit, à 55 % d'opacité, flotte donc au milieu de la
  bande nuit au lieu d'être collé au bas de la bande, contrairement à la capture principale.
- **Correction attendue** : retirer les deux lignes `border-bottom-*-radius: 0 !important` de
  `.shot-back`, pour qu'elle garde les 22 px de rayon sur ses 4 coins.

### M2. Accueil, tarifs : cartes très larges entre 600 et 899 px

**Statut : Corrigé (fcf2905)**.

- **Fichiers** : `index.html`, l. 359 (`<div class="plans">`) ; `site.v1.css`, l. 793-797.
- **Constat** : sur `/tarifs`, les cartes empilées sont limitées à 560 px (`.plans-stack`). Sur
  l'accueil, elles prennent toute la largeur à 768 px (≈ 700 px de large pour 4 lignes de
  texte) : le prix et le bouton sont très éloignés et la carte paraît vide.
- **Correction attendue** : ajouter `plans-stack` sur l'accueil :
  `<div class="plans plans-stack">`.

### M3. Tableau comparatif à 320 px : risque de débordement sur la ligne des prix

**Statut : Corrigé (fcf2905)** : mesuré à 320 px, `scrollWidth` = `clientWidth` (320 px).

- **Fichiers** : `tarifs.html`, l. 180 ; `site.v1.css`, l. 835-842.
- **Constat** : colonnes de plan ≈ 64 px (56 px utiles). « 1 800 000 » (espaces insécables)
  en Outfit 600 13 px mesure ≈ 58 px : le tableau peut déborder de 2 à 4 px et faire défiler
  le conteneur, alors que la fiche indique « aucun défilement attendu ».
- **Correction attendue** : dans le palier `max-width: 37.4375em`, ajouter
  `.compare .row-price td { font-size: .75rem; }`, puis vérifier à 320 px.

### M4. En-tête « collant » du tableau comparatif sans effet

**Statut : Corrigé (fcf2905)** : règle retirée, SPEC §6.16 et fiche tarifs mises à jour.

- **Fichier** : `site.v1.css`, l. 844 (`.compare thead th { position: sticky; top: 0 }`).
- **Constat** : `.table-scroll` a `overflow-x: auto`, ce qui en fait le conteneur de référence
  du positionnement collant. Comme ce conteneur ne défile pas verticalement, l'en-tête ne colle
  jamais (comportement normal des navigateurs, voir sources). Aucun effet visible, mais une
  intention de la SPEC §6.16 non tenue.
- **Correction attendue** : retirer la règle (le tableau ne fait que 12 lignes) et supprimer la
  mention « en-tête collant » de SPEC §6.16 et de la fiche tarifs. Ne pas utiliser
  `overflow-x: clip`, qui supprimerait le défilement de secours.

### M5. Anneau de focus du tableau comparatif rogné sous 600 px

**Statut : Corrigé (fcf2905)**.

- **Fichier** : `site.v1.css`, l. 841 (`margin-inline: calc(-1 * var(--pad))`).
- **Constat** : le conteneur va d'un bord à l'autre de l'écran. Ses côtés gauche et droit,
  dessinés à 3 px à l'extérieur, sortent donc de l'écran : seuls le haut et le bas restent
  visibles.
- **Correction attendue** : `.table-scroll:focus-visible { outline-offset: -3px; }`
  (valable pour tous les tableaux).

### M6. Badges stores sous 360 px : zone cliquable et anneau de focus sur toute la largeur

**Statut : Corrigé (fcf2905)**.

- **Fichier** : `site.v1.css`, l. 684-686 (`.store-badge { flex: 1 1 100% }`).
- **Constat** : le lien App Store devient une boîte de 288 px de large pour une image de
  148 px. L'anneau de focus entoure donc un grand vide à droite du badge.
- **Correction attendue** : remplacer par
  `@media (max-width: 22.4375em) { .store-row { flex-direction: column; align-items: flex-start; } }`.

### M7. Lien WhatsApp sous la FAQ parents : icône mal alignée

**Statut : Corrigé (fcf2905)** : règle ajoutée et espace retiré dans `parents.html`.

- **Fichiers** : `parents.html`, l. 239 ; `site.v1.css`, l. 214-224 et l. 882.
- **Constat** : l'icône `.ico` (20 px, `display` inline par défaut) est posée sur la ligne de
  base du texte, environ 4 px trop haut, et le soulignement du lien passe sous l'espace qui
  la suit.
- **Correction attendue** :
  `.after-faq a { display: inline-flex; align-items: center; gap: var(--s-2); padding-block: 8px; }`.
  Retirer aussi l'espace entre `</svg>` et « Écrivez-nous ».

### M8. Rangée de boutons des introductions : 24 px au lieu de 32 px

**Statut : Corrigé (fcf2905)** : 24 px conservés et documentés dans SPEC §6.7.

- **Fichier** : `site.v1.css`, l. 903 (`.page-intro .btn-row { margin-top: var(--s-5) }`).
- **Constat** : SPEC §6.3 fixe la `.btn-row` à 32 px du contenu qui précède. Écart volontaire
  ou non, il n'est documenté nulle part.
- **Correction attendue** : soit supprimer la surcharge, soit l'ajouter dans SPEC §6.7
  (« rangée de boutons à 24 px du chapô »). Je recommande de garder 24 px et de documenter :
  le chapô est déjà aéré.

### M9. Titres `h3` de la section « Confiance » en 18 px au lieu de 19 px

**Statut : Corrigé (fcf2905)**.

- **Fichier** : `site.v1.css`, l. 751 (`.trust-item h3 { font-size: 1.125rem }`).
- **Constat** : partout ailleurs, les `h3` font 1.2rem (SPEC §4.2). Sur l'accueil, ces titres
  sont voisins de cartes en 19 px, d'où une petite rupture de hiérarchie.
- **Correction attendue** : retirer `font-size: 1.125rem` de `.trust-item h3`.

### M10. Impression : en-tête du tableau et titres du pied de page illisibles

**Statut : Corrigé (fcf2905)**.

- **Fichier** : `site.v1.css`, l. 1027-1033.
- **Constat** : les navigateurs n'impriment pas les fonds par défaut. Le texte `--on-night`
  des en-têtes `.compare` et `.roles-table` (fond nuit) devient donc blanc sur blanc, et
  `.footer-title` (`--on-night-2`) n'est pas forcé en noir.
- **Correction attendue** : dans `@media print`, ajouter
  `.compare thead th, .roles-table thead th { background: #fff !important; color: #000 !important; border-bottom: 2px solid #000; }`
  et ajouter `.footer-title, .eyebrow-dark` à la liste des éléments forcés en `#000`.

### M11. Page 404 : alignement du texte à trancher

**Statut : Corrigé (fcf2905)** : texte gardé à gauche, fiche `404.md` mise à jour (aucun changement de code).

- **Fichiers** : `404.html`, l. 87-105 ; `site.v1.css`, l. 962-968.
- **Constat** : la fiche `404.md` dit « Centré » à 320 et 375 px. Le conteneur est bien centré,
  mais le texte est aligné à gauche.
- **Correction attendue** : garder le texte à gauche (meilleure lecture de l'encadré et de la
  liste de liens) et remplacer « Centré » par « Colonne `--narrow` centrée, texte aligné à
  gauche » dans la fiche. Aucun changement de code.

---

## Points vérifiés et conformes (pour mémoire)

- Tokens §4.1 à 4.5 identiques à la SPEC. `prefers-reduced-motion` coupe les transitions,
  l'animation de la FAQ et le défilement doux.
- Focus : `outline` 3 px décalé de 3 px, vert sur fond clair et or dans `.topbar`, `.hero`,
  `.closer` et `.footer`. `scroll-padding-top` compense l'en-tête collant (2.4.11).
- Cibles : boutons de 48 px, petit bouton et `summary` de 44 px, liens du menu de 48 px, liens
  du pied de page avec `padding-block: 6px`. Le lien « Crédits » (13 px) relève de l'exception
  « dans une phrase » du critère 2.5.8.
- Le menu mobile suit SPEC §6.1 : `details` natif, libellé « Menu » masqué visuellement sous
  440 px et toujours lu, `aria-current` cohérent entre le bureau et le mobile, message WhatsApp
  de la page. Le script ferme le menu par Échap et rend le focus au `summary`.
- Pas de défilement horizontal de la page à 320 px : le tableau comparatif et le rail défilent
  dans leur propre conteneur (`tabindex="0"`, `role="region"`, nom accessible). Les textes
  masqués sont contenus grâce à `position: relative`.
- Responsive des grilles conforme aux fiches : modules 1/2/3 colonnes, confiance 1/2/4, étapes
  à 760 px, sécurité 2 colonnes dans 1000 px, plans 1 puis 3 colonnes, pied de page 1/2/4.
- `fetchpriority="high"` uniquement sur la capture principale de l'accueil. `loading="lazy"`
  absent des images au-dessus de la ligne de flottaison (introductions Fonctionnalités et
  Parents).
- Textes : conformes aux fiches mot pour mot sur les pages relues. Balises `title`,
  `description` et `og:*` conformes. Aucune mention interdite (SPEC §2) relevée.

## Sources (veille du 23/10/2026)

- W3C, « Understanding SC 2.5.8 Target Size (Minimum) » (exception des liens dans une phrase) :
  https://www.w3.org/WAI/WCAG22/Understanding/target-size-minimum.html
- Ben Frain, « Yes! You can use position: sticky and overflow together » (un conteneur en
  `overflow: auto` devient la référence du positionnement collant ; `overflow: clip` comme
  alternative) : https://benfrain.com/?p=4829
- CSS-Tricks, « Position sticky and table headers » : https://css-tricks.com/?p=278476

## Vérification après corrections (fcf2905, 23/10/2026)

Chrome sans tête, pages chargées dans des iframes de 320, 360, 375, 768 et 1280 px, polices
chargées (`document.fonts.ready`).

- Aucun défilement horizontal de la page (`scrollWidth` = `clientWidth`) sur les 8 pages
  (accueil, fonctionnalités, parents, tarifs, sécurité, mentions légales, confidentialité, 404)
  aux 5 largeurs.
- Tableau des rôles et tableau comparatif : aucun défilement de leur conteneur, à toutes les
  largeurs.
- « Essai gratuit » sur une seule ligne à 320, 360 et 375 px.
- HTML des 8 pages bien formé (`html.parser`).
- Hors périmètre, pour information : sur `confidentialite.html`, les tableaux juridiques
  défilent dans leur conteneur sous 600 px (`min-width: 9rem` par cellule, voulu), et les
  tableaux 2 et 4 dépassent de 2 px (288 pour 286 px à 320 px), probablement à cause des
  bordures du conteneur. La page elle-même ne défile pas.
