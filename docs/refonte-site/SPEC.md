# Refonte du site vitrine schoolmanager.tech : spécification finale

Statut : **prête à coder** (étape 2 du plan, `PROPOSITION.md` §7). Validée sur le principe par
Oumar le 23/10/2026. Destinataire : agent-frontend (étapes 4 à 9), puis agent-devops (étape 10).

Ce document est le **socle commun** : décisions, tokens, composants, règles d'accessibilité,
ressources, liens. Chaque page a sa fiche dans `pages/`. En cas de contradiction entre la
maquette (`maquette/`) et cette spécification, **la spécification fait foi**.

## Index

| Page | URL publique | Fichier à créer dans `landing/` | Fiche |
|---|---|---|---|
| Accueil | `/` | `index.html` | [pages/accueil.md](pages/accueil.md) |
| Fonctionnalités | `/fonctionnalites` | `fonctionnalites.html` | [pages/fonctionnalites.md](pages/fonctionnalites.md) |
| Parents | `/parents` | `parents.html` | [pages/parents.md](pages/parents.md) |
| Tarifs | `/tarifs` | `tarifs.html` | [pages/tarifs.md](pages/tarifs.md) |
| Sécurité et confidentialité | `/securite` | `securite.html` | [pages/securite.md](pages/securite.md) |
| Politique de confidentialité | `/confidentialite.html` (**URL inchangée**) | `confidentialite.html` (restylée) | [pages/confidentialite.md](pages/confidentialite.md) |
| Mentions légales | `/mentions-legales` | `mentions-legales.html` | [pages/mentions-legales.md](pages/mentions-legales.md) |
| Page introuvable | toute URL inconnue | `404.html` | [pages/404.md](pages/404.md) |

Sommaire de ce document : 1. Décisions · 2. Faits autorisés · 3. Liens et messages WhatsApp ·
4. Tokens · 5. Mise en page et points de rupture · 6. Composants · 7. Accessibilité ·
8. Ressources (images, polices, icônes) · 9. Captures futures · 10. Technique, SEO, Nginx ·
11. Points non tranchés · Sources.

---

## 1. Décisions d'Oumar et conséquences

| Décision | Conséquence dans la spec |
|---|---|
| Fond clair (papier), bandes vert nuit, or en accent | Vert nuit : haut de page de l'accueil, appels finaux, pied de page, en-tête. Toutes les autres sections sur papier ou papier teinté. |
| Éditeur LOGILAB, Bamako (Mali). Directeur de la publication : Oumar Mory Coulibaly. Contact public : oumarcoulou44@gmail.com, WhatsApp +223 79 82 79 79 | Pied de page, mentions légales, FAQ « Qui édite ». L'e-mail est affiché partout où le site promet un « support par e-mail ». |
| RCCM / NIF non fournis | Lignes présentes **en commentaire HTML** dans les mentions légales (« à compléter par Oumar »), **jamais affichées vides**. |
| Hébergeur : VPS Hostinger derrière Cloudflare | Mentions légales : Hostinger International Ltd (Chypre) et Cloudflare, Inc. (États-Unis). « Union européenne » repris de la politique de confidentialité, non vérifié (voir §11). |
| Aucune mesure d'audience | Aucun script tiers, aucun cookie posé par le site. Attribution approximative via la mention « (Site : page) » dans les messages WhatsApp préremplis (§3). |
| Pas de séance photo | Captures réelles (`marketing/play-store/capture-*.png`) + 3 photos CC0 d'Afrique de l'Ouest seulement. `ecriture.jpg`, `mobile-classe.jpg`, `campus.jpg` **retirées du site** (fichiers supprimés de `landing/assets/`, `CREDITS.md` mis à jour). |
| Mentions commerciales non confirmées hors du site | **Absents partout** : domaine personnalisé, engagement de service, interlocuteur dédié, bulletin « conforme à votre modèle », bulletins « à votre charte », support par chat, moyens de paiement (espèces, virement, mobile money, Stripe, carte). |
| Étape 3 (nouvelles captures) reportée | **Aucun emplacement vide ou hachuré** (les `.placeholder` de la maquette disparaissent). §9 indique où chaque future capture remplacera l'actuelle. |
| Android | Badge « Bientôt sur Google Play » **non cliquable, atténué**, toujours à côté du badge App Store. |
| Pages de lancement | Les 8 fichiers de l'index ci-dessus. Pas de page contact, FAQ ou application mobile dédiée (`PROPOSITION.md` §3.1). |

Décisions de design prises dans cette spec (à signaler si désaccord) :

- **Pas de badges stores dans le haut de page de l'accueil** : il s'adresse aux directeurs, un
  seul appel principal (essai). Les badges vivent dans la section parents de l'accueil, sur
  `/parents` et sur `/fonctionnalites`. Le bandeau d'aiguillage parents, juste sous le haut de
  page, y mène en un clic.
- **Pas de mode sombre** : le site est déjà rythmé par des bandes sombres, et la lecture en plein
  soleil est meilleure sur fond clair. `<meta name="color-scheme" content="light">` pour éviter
  l'assombrissement automatique de Chrome Android, qui casserait les contrastes calculés.
- **Pas de vidéo** au lancement (la vidéo n'est pas dans `landing/`, et coûte cher en 3G).
- **Apostrophes droites** (') dans tout le texte, comme aujourd'hui : un seul style, moins
  d'erreurs de copie. Espaces insécables obligatoires (§7.6).

---

## 2. Faits autorisés (garde-fou pour agent-relecteur)

Tout texte du site doit pouvoir être rattaché à une ligne de ce tableau. Sources : `landing/index.html`
(tarifs), `marketing/reseaux-sociaux/PLAN.md` §1.5 et §1.6, `docs/FICHES-STORES.md` §2.2 (texte
publié sur l'App Store), `landing/confidentialite.html`, `docs/ROADMAP.md`, `docs/ARCHITECTURE.md`.

| Sujet | Formulation autorisée |
|---|---|
| Disponibilité | iPhone : App Store, id6822008457, gratuite. Android : « en test », « bientôt sur Google Play ». **Jamais de date.** |
| Public | Écoles privées du Mali, enseignement fondamental, 1ère à 9ème année. Entièrement en français, montants en FCFA. Édité à Bamako par LOGILAB. |
| Parents (application) | Absences et retards prévenus dès la saisie, avec date et motif ; notes matière par matière, moyenne et rang ; échéancier, réglé / reste dû, reçu de chaque paiement ; emploi du temps ; plusieurs enfants sous un seul compte ; notifications réglables. Compte remis par l'école (sous-domaine, e-mail, mot de passe). |
| Parents : ce qui n'existe pas | **Pas de messagerie dans l'application, pas de paiement des frais dans l'application, pas d'inscription autonome.** |
| Élèves (application) | Leur propre compte : notes, absences, emploi du temps. |
| Enseignants (application) | Feuille d'appel (Présent, Absent, Retard, Départ anticipé), saisie des notes, emploi du temps de la semaine. |
| Plateforme web | 20 modules, 8 rôles. Import CSV des élèves ; bulletins PDF à l'en-tête de l'école (moyennes, coefficients, appréciations, rang, absences de la période) ; emploi du temps avec détection des conflits ; frais de scolarité, factures, reçus, impayés ; vie scolaire (incidents, sanctions, convocations, observations) ; cahier de textes et documents ; annonces et messagerie interne **sur le web** ; certificats de scolarité et cartes scolaires ; statistiques (effectifs, présence, moyennes, évolution, export CSV) ; cantine, transport, bibliothèque. |
| Rôles (8) | Administrateur, Direction, Secrétariat, Comptabilité, Vie scolaire, Enseignant, Parent, Élève. |
| Connexions lentes | « L'application ne télécharge que ce qu'elle affiche. » Ne pas l'étendre à la plateforme web. |
| Tarifs | Essentiel 180 000 FCFA/an (jusqu'à 150 élèves ; 15 000/mois ; 1 200/élève/an). Standard 750 000 FCFA/an (jusqu'à 800 élèves ; 62 500/mois ; environ 940/élève/an). Premium dès 1 800 000 FCFA/an (effectif illimité, tarif dégressif selon l'effectif). Palier : 1 000 FCFA par élève et par an au-delà d'un palier dépassé de peu. Abonnement annuel. |
| Contenu des plans | Essentiel : scolarité, notes et bulletins ; absences et emploi du temps ; application mobile ; support par e-mail. Standard : + frais de scolarité et facturation ; + statistiques de direction ; + cantine, transport, bibliothèque **en option**. Premium : tous les modules ; statistiques avancées et exports ; accompagnement à la reprise des données. |
| Essai | 30 jours, sans engagement, aucun paiement demandé pour l'ouvrir. **L'essai fonctionne avec le plan Essentiel** (`ARCHITECTURE.md`, plan d'essai par défaut `ESSENTIEL`) : ne jamais écrire « tous les modules pendant l'essai ». Fin d'essai sans abonnement : lecture seule, puis suspension. Aucun délai chiffré (non confirmé). |
| Sécurité | Un espace par école, isolé par la base de données (PostgreSQL, sécurité au niveau des lignes) ; droits vérifiés côté serveur ; HTTPS obligatoire ; mots de passe jamais stockés en clair ; sauvegardes chiffrées conservées 30 jours ; actions sensibles journalisées ; export sur demande, suppression 90 jours après la fin du service. Aucune donnée vendue, louée ni utilisée à des fins publicitaires. |
| Interdits (PLAN.md §1.5) | Chiffres d'utilisateurs ou d'écoles, témoignages, noms d'écoles, superlatifs, comparaison avec un concurrent, fausse urgence, promesse de résultat scolaire, « l'appel en trente secondes » présenté comme mesuré. |

---

## 3. Liens exacts

### 3.1 Liens fixes

| Cible | `href` | Remarque |
|---|---|---|
| Essai gratuit | `/register` | Redirigé par Nginx vers `https://app.<domaine>/register`. Même onglet. |
| Connexion (web) | `/login` | Redirigé vers `https://app.<domaine>/login`. Même onglet. |
| App Store | `https://apps.apple.com/app/id6822008457` | `target="_blank" rel="noopener"` + texte masqué « (nouvel onglet) ». |
| E-mail | `mailto:oumarcoulou44@gmail.com` | Texte visible : `oumarcoulou44@gmail.com`. |
| Pages internes | `/`, `/fonctionnalites`, `/parents`, `/tarifs`, `/securite`, `/confidentialite.html`, `/mentions-legales` | **Toujours en chemin absolu depuis la racine**, y compris les ressources (`/assets/...`) : la page 404 est servie à des adresses arbitraires (`/a/b/c`), un chemin relatif y casserait CSS et images. |

### 3.2 Messages WhatsApp préremplis

Tous en `target="_blank" rel="noopener"`, texte masqué « (WhatsApp, nouvel onglet) », icône
WhatsApp à gauche du libellé. La mention « (Site : …) » en fin de message remplace la mesure
d'audience : Oumar voit d'où vient la conversation.

| Code | Usage | Texte du message | `href` exact |
|---|---|---|---|
| W1 | Accueil : démo (haut de page, appel final, en-tête mobile, pied de page) | Bonjour, je souhaite une démonstration de School Manager pour mon école. (Site : accueil) | `https://wa.me/22379827979?text=Bonjour%2C%20je%20souhaite%20une%20d%C3%A9monstration%20de%20School%20Manager%20pour%20mon%20%C3%A9cole.%20%28Site%20%3A%20accueil%29` |
| W2 | Accueil : aide au démarrage (section « trois étapes ») | Bonjour, j'ai besoin d'aide pour démarrer avec School Manager. (Site : accueil) | `https://wa.me/22379827979?text=Bonjour%2C%20j%27ai%20besoin%20d%27aide%20pour%20d%C3%A9marrer%20avec%20School%20Manager.%20%28Site%20%3A%20accueil%29` |
| W3 | Fonctionnalités : démo (toute la page) | Bonjour, je souhaite une démonstration de School Manager pour mon école. (Site : fonctionnalités) | `https://wa.me/22379827979?text=Bonjour%2C%20je%20souhaite%20une%20d%C3%A9monstration%20de%20School%20Manager%20pour%20mon%20%C3%A9cole.%20%28Site%20%3A%20fonctionnalit%C3%A9s%29` |
| W4 | Tarifs : question (toute la page sauf Premium) | Bonjour, j'ai une question sur les tarifs de School Manager. (Site : tarifs) | `https://wa.me/22379827979?text=Bonjour%2C%20j%27ai%20une%20question%20sur%20les%20tarifs%20de%20School%20Manager.%20%28Site%20%3A%20tarifs%29` |
| W5 | Tarifs : devis Premium | Bonjour, je souhaite un devis pour le plan Premium de School Manager. (Site : tarifs) | `https://wa.me/22379827979?text=Bonjour%2C%20je%20souhaite%20un%20devis%20pour%20le%20plan%20Premium%20de%20School%20Manager.%20%28Site%20%3A%20tarifs%29` |
| W6 | Parents : question (en-tête mobile, pied de page) | Bonjour, je suis parent d'élève et j'ai une question sur l'application School Manager. (Site : parents) | `https://wa.me/22379827979?text=Bonjour%2C%20je%20suis%20parent%20d%27%C3%A9l%C3%A8ve%20et%20j%27ai%20une%20question%20sur%20l%27application%20School%20Manager.%20%28Site%20%3A%20parents%29` |
| W7 | Sécurité (toute la page) | Bonjour, j'ai une question sur la sécurité des données dans School Manager. (Site : sécurité) | `https://wa.me/22379827979?text=Bonjour%2C%20j%27ai%20une%20question%20sur%20la%20s%C3%A9curit%C3%A9%20des%20donn%C3%A9es%20dans%20School%20Manager.%20%28Site%20%3A%20s%C3%A9curit%C3%A9%29` |
| W8 | Générique : confidentialité, mentions légales, 404 | Bonjour, j'ai une question sur School Manager. (Site) | `https://wa.me/22379827979?text=Bonjour%2C%20j%27ai%20une%20question%20sur%20School%20Manager.%20%28Site%29` |
| W9 | Partage parent vers directeur (pas de numéro : l'utilisateur choisit le contact) | Bonjour, je suis parent d'élève. Avec School Manager, les parents suivent les absences, les notes et la scolarité de leur enfant sur leur téléphone. Notre école pourrait l'utiliser : https://schoolmanager.tech/ | `https://wa.me/?text=Bonjour%2C%20je%20suis%20parent%20d%27%C3%A9l%C3%A8ve.%20Avec%20School%20Manager%2C%20les%20parents%20suivent%20les%20absences%2C%20les%20notes%20et%20la%20scolarit%C3%A9%20de%20leur%20enfant%20sur%20leur%20t%C3%A9l%C3%A9phone.%20Notre%20%C3%A9cole%20pourrait%20l%27utiliser%20%3A%20https%3A%2F%2Fschoolmanager.tech%2F` |

Règle : le lien WhatsApp de l'**en-tête mobile** et du **pied de page** utilise le message de la
page courante (accueil W1, fonctionnalités W3, parents W6, tarifs W4, sécurité W7, autres W8).
agent-tests vérifie que chaque `href` se décode en exactement le texte de la colonne 3.

---

## 4. Tokens de design

À déclarer une seule fois dans `:root` de `/assets/site.vN.css` (actuellement `site.v3.css`). Les valeurs reprennent la
palette de l'application (`docs/DESIGN.md` : vert `#0f5c4c`, or `#c9a227`, navigation
`#0b231e`, Inter / Outfit). La charte Moov Africa ne s'applique pas à ce produit.

### 4.1 Couleurs et contrastes

Contrastes recalculés (formule WCAG 2.x, luminance relative). Seuils : texte courant 4,5:1,
grand texte et éléments graphiques 3:1.

| Token | Hex | Usage | Contrastes vérifiés |
|---|---|---|---|
| `--paper` | `#f7f4ec` | Fond de page | — |
| `--paper-2` | `#efebe0` | Sections teintées | — |
| `--surface` | `#ffffff` | Cartes, FAQ, tableau | — |
| `--line` | `#ddd7c8` | Bordures décoratives des cartes (jamais seule indication d'un état) | — |
| `--ink` | `#12241f` | Texte principal | 14,7:1 sur papier ; 13,6:1 sur papier-2 ; 16,2:1 sur blanc |
| `--ink-2` | `#4a5a55` | Texte secondaire, légendes | 6,6:1 sur papier ; 6,1:1 sur papier-2 ; 7,3:1 sur blanc |
| `--green` | `#0f5c4c` | Bouton primaire sur clair, liens, icônes, **anneau de focus sur clair** | blanc dessus 7,9:1 ; sur papier 7,2:1 ; sur papier-2 6,6:1 ; sur vert pâle 6,7:1 |
| `--green-hover` | `#0b4a3d` | Survol du vert | blanc dessus > 10:1 |
| `--green-pale` | `#e4eee9` | Bandeau parents, pastilles, survol des boutons contour | — |
| `--night` | `#0b231e` | En-tête, haut de page accueil, appels finaux, pied de page | — |
| `--night-2` | `#12352d` | Filets et survols sur fond nuit | — |
| `--on-night` | `#f1f5f3` | Texte principal sur nuit | 15,0:1 |
| `--on-night-2` | `#b9ccc5` | Texte secondaire sur nuit | 9,8:1 |
| `--gold` | `#c9a227` | Bouton primaire sur nuit, puces, filets, **anneau de focus sur nuit**, mots en or du titre (grand texte sur nuit) | sur nuit 6,8:1 ; **sur papier 2,2:1 : interdit en texte et en focus sur fond clair** |
| `--gold-hover` | `#ddb63a` | Survol de l'or | — |
| `--gold-ink` | `#1a1403` | Texte sur bouton or | 7,6:1 sur or ; 9,5:1 sur or survolé |
| `--gold-dark` | `#7a5f0c` | Surtitres sur fond clair | 5,5:1 sur papier ; 5,1:1 sur papier-2 ; 6,1:1 sur blanc |
| `--outline-light` | `rgba(241,245,243,.55)` | Bordure du bouton contour sur nuit | env. 5,4:1 sur nuit (≥ 3:1 requis) |

Correction par rapport à la maquette : la maquette utilisait l'or pour **tous** les anneaux de
focus, soit 2,2:1 sur papier (échec du critère 1.4.11). Désormais : focus vert sur fond clair,
focus or sur fond nuit (§7.2).

### 4.2 Typographie

Polices **auto-hébergées** (§8.3). Corps de base : `html { font-size: 100% }`, tailles en `rem`.

| Token / élément | Police | Graisse | Taille | Interligne | Autres |
|---|---|---|---|---|---|
| `--font-title` | `"Outfit", system-ui, -apple-system, "Segoe UI", Roboto, sans-serif` | 600 | — | — | — |
| `--font-body` | `"Inter", system-ui, -apple-system, "Segoe UI", Roboto, sans-serif` | 400 / 600 | — | — | — |
| `h1` accueil | Outfit | 600 | `clamp(2rem, 1.35rem + 3vw, 3.4rem)` (32 → 54 px) | 1.1 | `max-width: 18ch`, `letter-spacing: -0.02em`, `text-wrap: balance` |
| `h1` pages intérieures | Outfit | 600 | `clamp(1.9rem, 1.4rem + 2.4vw, 3rem)` (30 → 48 px) | 1.12 | `max-width: 22ch` |
| `h2` | Outfit | 600 | `clamp(1.75rem, 1.3rem + 2vw, 2.6rem)` (28 → 42 px) | 1.12 | `max-width: 24ch` |
| `h3` | Outfit | 600 | `1.2rem` (19 px) | 1.25 | — |
| Texte courant | Inter | 400 | `1.0625rem` (17 px) | 1.6 | paragraphes `max-width: 65ch` |
| Chapô (`.lede`) | Inter | 400 | `clamp(1.0625rem, 1rem + .4vw, 1.1875rem)` | 1.6 | `max-width: 56ch` |
| Petit texte | Inter | 400 | `0.9375rem` (15 px) | 1.5 | — |
| Légende | Inter | 400 | `0.8125rem` (13 px) | 1.45 | **uniquement** légendes de photo et bas de pied de page |
| Surtitre (`.eyebrow`) | Inter | 600 | `0.8125rem` | 1.3 | capitales, `letter-spacing: .12em`, couleur `--gold-dark` (clair) ou `--gold` (nuit) |
| Prix | Outfit | 600 | `clamp(1.6rem, 1.3rem + 1.4vw, 2rem)` | 1.15 | chiffres tabulaires `font-variant-numeric: tabular-nums` |
| Boutons | Inter | 600 | `1rem` (md) / `0.9375rem` (sm) | 1.25 | — |

### 4.3 Espacements (base 4 px)

| Token | Valeur | Usage type |
|---|---|---|
| `--s-1` | 4 px | Écart icône / texte serré |
| `--s-2` | 8 px | Écart icône / texte, puces |
| `--s-3` | 12 px | Écart entre boutons, entre questions FAQ |
| `--s-4` | 16 px | Padding interne compact, écart de grille mobile |
| `--s-5` | 24 px | Padding des cartes, écart de grille |
| `--s-6` | 32 px | Titre de section → contenu |
| `--s-7` | 48 px | Entre blocs d'une même section |
| `--s-8` | 64 px | — |
| `--section-y` | `clamp(3.5rem, 8vw, 6rem)` (56 → 96 px) | Padding vertical des sections |
| `--pad` | `clamp(1rem, 4vw, 2.5rem)` (16 → 40 px) | Marge latérale du conteneur |
| `--shell` | 1180 px | Largeur maximale du contenu |
| `--narrow` | 820 px | FAQ, pages juridiques (`46rem` de texte) |
| `--header-h` | 64 px | Hauteur de l'en-tête collant |

### 4.4 Rayons et ombres

| Token | Valeur | Usage |
|---|---|---|
| `--r-sm` | 8 px | Badges stores, anneau de focus |
| `--r-md` | 12 px | FAQ, encadrés, captures dans une carte, tableau |
| `--r-lg` | 16 px | Cartes, photos |
| `--r-xl` | 22 px | Capture du haut de page |
| `--r-pill` | 999 px | Boutons, pastilles, puces de sommaire |
| `--shadow-card` | `0 1px 2px rgba(18,36,31,.06), 0 4px 12px rgba(18,36,31,.05)` | Cartes sur papier (facultative, pas d'ombre au survol) |
| `--shadow-shot` | `0 12px 32px rgba(18,36,31,.16)` | Captures sur fond clair |
| `--shadow-shot-dark` | `0 24px 60px rgba(0,0,0,.35)` | Captures sur fond nuit |
| `--shadow-menu` | `0 16px 40px rgba(0,0,0,.35)` | Menu mobile déroulé |

### 4.5 Mouvement

| Token | Valeur | Usage |
|---|---|---|
| `--t-fast` | 150 ms ease-out | Couleurs au survol, ouverture FAQ et menu |
| `--t-base` | 200 ms ease-out | Bordure et fond des boutons |

Aucune animation d'apparition au défilement, aucun déplacement au survol, aucun contenu masqué
au chargement. `@media (prefers-reduced-motion: reduce)` : `scroll-behavior: auto` et
`transition: none` partout.

---

## 5. Mise en page et points de rupture

**Mobile d'abord** : styles de base pour 320 px, puis `min-width` en `em` (suivent le zoom).

| Nom | Requête | Largeur | Usage principal |
|---|---|---|---|
| base | — | 320 à 599 px | Une colonne |
| `sm` | `(min-width: 37.5em)` | ≥ 600 px | Grilles 2 colonnes (modules, sécurité, pied de page) |
| `md` | `(min-width: 56.25em)` | ≥ 900 px | Navigation complète, haut de page en 2 colonnes, grilles 3 colonnes |
| `lg` | `(min-width: 75em)` | ≥ 1200 px | Grille sécurité en 4 colonnes, marges maximales |

Réglages fins de l'en-tête uniquement : `(max-width: 27.4375em)` (< 440 px) et
`(max-width: 22.4375em)` (< 360 px), voir §6.1.

Les 4 largeurs de contrôle de chaque fiche de page : **320** (petit Android), **375** (iPhone
courant), **768** (tablette portrait, palier `sm`), **1280** (ordinateur, palier `lg`).
Exigence commune : **aucun défilement horizontal de la page à 320 px** et à 400 % de zoom
sur 1280 px (critère 1.4.10). Seuls le tableau comparatif et le rail de captures défilent
horizontalement, à l'intérieur de leur propre conteneur (§6.12, §6.16).

---

## 6. Composants

Les noms de classes reprennent ceux de la maquette quand ils existent. Chaque composant est
défini une fois dans `site.vN.css` (actuellement `site.v3.css`).

### 6.1 En-tête commun (`.topbar`), identique sur les 8 pages

Structure (dans cet ordre dans le DOM) :

1. Lien d'évitement `<a class="skip-link" href="#contenu">Aller au contenu principal</a>`
   (premier élément du `body`). `<main id="contenu">` sur chaque page.
2. `<header class="topbar">` collant (`position: sticky; top: 0; z-index: 50`), hauteur 64 px,
   fond `--night`, filet bas `1px rgba(255,255,255,.08)`.
3. Marque : `<a class="brand" href="/">` contenant l'icône de l'application
   (`/assets/brand/icone-72.png`, affichée 36 × 36, `alt=""`, rayon 9 px) et le texte
   « School Manager » (Outfit 600, 17 px). Nom accessible : le texte visible suffit.
4. Navigation bureau `<nav class="nav-desktop" aria-label="Navigation principale">` :
   **Fonctionnalités** (`/fonctionnalites`) · **Tarifs** (`/tarifs`) · **Parents**
   (`/parents`) · **Sécurité** (`/securite`). Lien de la page courante :
   `aria-current="page"`, couleur `--on-night`, soulignement or 2 px décalé de 6 px.
   Autres liens `--on-night-2`, survol `--on-night` + soulignement.
5. Actions : lien discret **« Se connecter »** (`/login`, classe `.link-quiet`) puis bouton or
   petit format **« Essai gratuit »** (`/register`).
6. Menu mobile `<details class="nav-mobile">` :
   - `<summary>` : icône menu (`#i-menu`) + texte « Menu ». Contour `1px rgba(241,245,243,.35)`,
     44 × 44 px minimum, pilule. Ouvert : contour or, icône `#i-close`.
   - Panneau `<nav aria-label="Menu">` : liens Fonctionnalités, Tarifs, Parents, Sécurité
     (même ordre que le bureau, `aria-current` identique), séparateur, **Se connecter**
     (`/login`), **Écrire sur WhatsApp** (message de la page, §3.2), **E-mail** (`mailto:`).
   - Panneau : `position: absolute; right: 0; top: calc(100% + 10px); width: min(320px, calc(100vw - 2rem))`,
     fond `--night`, bordure `--night-2`, rayon 16 px, `--shadow-menu`, liens de 48 px de haut.
   - Fonctionne sans JS (natif). Amélioration par `site.vN.js` (actuellement `site.v2.js`) : fermeture par Échap (le focus
     revient sur le `summary`), par clic hors du panneau et au clic sur un lien.

Comportement par largeur :

| Largeur | Visible |
|---|---|
| ≥ 900 px | Marque + 4 liens + « Se connecter » + « Essai gratuit ». Menu masqué (`display: none`). |
| 600 à 899 px (768) | Marque + « Se connecter » + « Essai gratuit » + bouton « Menu » (icône + texte). Liens bureau masqués. |
| 440 à 599 px | Marque + « Essai gratuit » + « Menu ». « Se connecter » passe dans le menu (déjà présent). |
| 360 à 439 px (375) | Idem ; texte « Menu » masqué **visuellement** (`.sr-only`, pas `display:none`, pour garder le nom accessible) ; icône 30 px, marque en 15 px ; écart de 8 px entre la marque et les actions ; bouton « Essai gratuit » `padding: .5rem .8rem`, 14 px, `white-space: nowrap` (toujours sur une ligne). |
| < 360 px (320) | Idem ; texte « School Manager » masqué visuellement (l'icône reste, le nom reste lu). |

`html { scroll-padding-top: calc(var(--header-h) + 1rem) }` : les ancres et le focus ne passent
jamais sous l'en-tête (critère 2.4.11).

### 6.2 Pied de page commun (`.footer`), identique sur les 8 pages

Fond `--night`, texte `--on-night-2`, liens `--on-night` (survol or + soulignement), padding
`48px 0 32px`.

Grille : 1 colonne (< 600) → 2 colonnes (600 à 899) → `1.6fr repeat(3, 1fr)` (≥ 900).

- **Colonne 1** : marque (icône 32 px + « School Manager », non cliquable, `<p>`), puis :
  « Gestion scolaire pour les écoles privées du Mali, sur le web et sur mobile. Édité par
  LOGILAB, Bamako (Mali). » Puis liste de contact :
  - « WhatsApp : +223 79 82 79 79 » (lien, message de la page)
  - « E-mail : oumarcoulou44@gmail.com » (lien `mailto:`)
- **Colonne 2**, titre « Produit » : Fonctionnalités · Tarifs · Parents · Télécharger sur l'App Store (nouvel onglet).
- **Colonne 3**, titre « Confiance » : Sécurité · Politique de confidentialité · Mentions légales.
- **Colonne 4**, titre « Accès » : Se connecter · Inscrire mon école (`/register`).
- **Ligne de base** (filet `--night-2`, 14 px) : « © 2026 LOGILAB » à gauche ; « Photos
  d'illustration : Wikimedia Commons, CC0. » suivi du lien « Crédits » (`/mentions-legales#credits`) à droite.

Titres de colonnes : `<h2 class="footer-title">` (Inter 600, 13 px, capitales,
`letter-spacing: .1em`, `--on-night-2`). Aucun niveau sauté. Liens avec `padding-block: 6px`
pour une cible d'au moins 24 px de haut (36 px visés).

L'ordre WhatsApp → e-mail est **identique sur toutes les pages** (critère 3.2.6, aide cohérente).

### 6.3 Boutons (`.btn`)

Base : `inline-flex`, centré, `gap: 8px`, `min-height: 48px`, `padding: 12px 22px`, rayon
pilule, `border: 2px solid transparent` (la bordure transparente devient visible en mode
contraste forcé), Inter 600 16 px, sans soulignement. Transition `background-color, border-color, color` 200 ms.

| Variante | Fond | Texte | Bordure | Survol | Où |
|---|---|---|---|---|---|
| `.btn-green` (primaire clair) | `--green` | `#fff` | — | fond `--green-hover` | Sections claires, **un seul par section** |
| `.btn-gold` (primaire nuit) | `--gold` | `--gold-ink` | — | fond `--gold-hover` | Bandes nuit, en-tête |
| `.btn-outline` | transparent | `--green` | `--green` | fond `--green-pale` | Secondaire sur clair |
| `.btn-outline-light` | transparent | `--on-night` | `--outline-light` | bordure `--gold` | Secondaire sur nuit |
| `.btn-sm` | — | 15 px | — | — | En-tête : `min-height: 44px; padding: 8px 16px` |

États : focus (§7.2) ; actif `filter: brightness(.95)` ; pas d'état désactivé sur le site.
Icône (WhatsApp, flèche) : 20 px, `aria-hidden="true"`. Rangée de boutons `.btn-row` :
`flex-wrap: wrap; gap: 12px; margin-top: 32px`. **À moins de 600 px, les boutons d'une rangée
prennent toute la largeur** (`flex: 1 1 100%`) pour des cibles franches.

### 6.4 Liens texte

Couleur `--green` sur clair, `--on-night` sur nuit ; soulignés dans les paragraphes
(`text-underline-offset: .2em`). Lien « suite » (`.more a`) : 600, flèche `#i-arrow` 16 px,
`padding-block: 8px`, non souligné, souligné au survol. Un lien n'est jamais signalé par la seule
couleur dans un paragraphe (soulignement obligatoire).

Liens externes : texte masqué « (nouvel onglet) » ou « (WhatsApp, nouvel onglet) » via
`<span class="sr-only">`.

### 6.5 Surtitre et en-tête de section

`.eyebrow` (§4.2) au-dessus du `h2`, `margin-bottom: 12px`. `h2` : `margin-bottom: 32px` sauf s'il
est suivi d'un chapô (`16px`, puis chapô `margin-bottom: 32px`). Chaque `section` porte
`aria-labelledby` vers l'`id` de son titre.

### 6.6 Haut de page de l'accueil (`.hero`)

Voir `pages/accueil.md` §1. Fond `--night` + deux dégradés radiaux (or 14 % en haut à droite,
vert 60 % en haut à gauche), comme la maquette. Grille `1.1fr .9fr` à partir de 900 px.

### 6.7 Introduction de page intérieure (`.page-intro`)

Fond `--paper`, `padding-block: clamp(2.5rem, 6vw, 4.5rem) clamp(2rem, 5vw, 3.5rem)`, filet bas
`--line`. Contenu : surtitre, `h1`, chapô, rangée de boutons ou badges (selon la page), visuel
facultatif à droite (≥ 900 px, grille `1.15fr .85fr`, aligné en bas). Exception au §6.3 :
la rangée de boutons est à 24 px (`--s-5`) du chapô, et non à 32 px, le chapô étant déjà aéré. Pas de fond nuit sur les
pages intérieures : le nuit reste réservé à l'accueil, aux appels finaux et au pied de page.

### 6.8 Sommaire de page (`.toc`)

`<nav aria-label="Sur cette page">` sous l'introduction des pages longues (Fonctionnalités,
Confidentialité). Liste de pastilles-liens : fond `--surface`, bordure `--line`, texte
`--green` 600 15 px, `min-height: 40px`, `padding: 8px 14px`, rayon pilule,
`flex-wrap: wrap; gap: 8px`. Survol : fond `--green-pale`.

### 6.9 Bandeau d'aiguillage parents (`.switch`)

Bande pleine largeur sous le haut de page de l'accueil. Fond `--green-pale`, filet bas `--line`,
`padding-block: 14px`, texte 15,5 px `--ink`. Contenu : « Vous êtes parent d'élève ? » + lien 600
« Voir ce que l'application vous montre » + flèche → `/parents`. Le lien a `padding-block: 4px`
(cible ≥ 24 px). À 320 px le texte passe sur deux lignes, sans troncature.

### 6.10 Liste de faits (`.facts`)

`<ul>` en ligne, `flex-wrap: wrap; gap: 8px 28px`, 15 px, 600, `--ink-2`, puce ronde or 7 px
(décorative, `::before`). Filet bas `--line`. À 320 px : une colonne.

### 6.11 Cartes

Base `.card` : fond `--surface`, bordure `1px --line`, rayon 16 px, padding 24 px,
`--shadow-card` facultative, **aucun effet au survol** (les cartes ne sont pas cliquables).

| Variante | Contenu |
|---|---|
| `.card` « situation » | `h3.card-problem` (intitulé de problème, sans guillemets ni attribution) (Outfit 600, 20 px, `--ink`, filet gauche or 3 px, `padding-left: 14px`) + paragraphe `--ink-2`. Pastille facultative (§6.13). |
| `.role` « public » | Icône 32 px verte, `h3` + pastille plateforme, liste `.ticks`. |
| `.module` | Icône 32 px verte, `h3` (`margin: 16px 0 6px`), paragraphe 15,5 px `--ink-2`, pastille de plan facultative. |
| `.feature` (parents) | Idem `.module`. |

Liste à coches `.ticks` : `display: grid; gap: 10px`, icône `#i-check` verte 20 px alignée sur la
première ligne, texte 15,5 px `--ink-2` (17 px `--ink` en variante `.ticks-lg`).

### 6.12 Cadre de capture (`.shot`) et rail de captures (`.shots-rail`)

- `.shot` : `<figure>` sans marge, fond `--night`, rayon 14 px (22 px dans le haut de page),
  `overflow: hidden`, bordure `1px rgba(18,36,31,.08)`, `--shadow-shot` (ou `-dark` sur nuit).
  Image `object-fit: cover; object-position: top`, ratio forcé par `aspect-ratio` (9/16 entier,
  ou 9/15 recadré en bas). `width`/`height` HTML toujours renseignés (540 × 960).
- Légende `<figcaption>` sous le cadre : 15 px `--ink-2`, `margin-top: 10px`, centrée.
- `.shots-rail` (3 captures côte à côte) :
  - ≥ 600 px : grille 3 colonnes, `gap: 24px`, captures `max-width: 260px` centrées.
  - < 600 px : défilement horizontal `overflow-x: auto; scroll-snap-type: x mandatory`,
    chaque capture `flex: 0 0 72%`, `scroll-snap-align: center`. Le conteneur porte
    `tabindex="0"`, `role="region"` et `aria-label` (défilable au clavier, critère 2.1.1).
    Indication visible sous le rail, < 600 px seulement : « Faites glisser pour voir les autres écrans. »

### 6.13 Pastilles (`.tag`)

Inter 600, 12 px, `letter-spacing: .04em`, `padding: 3px 10px`, rayon pilule.

| Variante | Fond | Texte | Libellés utilisés |
|---|---|---|---|
| `.tag` (plateforme) | `--green-pale` | `--green` | « Sur le web », « Application », « Web et application » |
| `.tag-plan` | `--paper-2` + bordure `--line` | `--ink-2` | « Plans Standard et Premium », « Standard (en option) et Premium » |

### 6.14 Encadré (`.notice`)

Fond `--surface`, bordure `--line`, **filet gauche 4 px `--green`**, rayon 12 px, padding
`16px 20px`, texte `--ink-2`, `strong` en `--ink`. Variante `.notice-gold` (pages juridiques) :
filet gauche or, autorisé car l'or y est décoratif (pas du texte).

### 6.15 Badges stores (`.store-row`)

Rangée `flex-wrap: wrap; align-items: center; gap: 12px 16px; margin-top: 24px`.

1. **App Store** : `<a class="store-badge" href="https://apps.apple.com/app/id6822008457" target="_blank" rel="noopener">`
   contenant `<img src="/assets/badges/app-store-fr.svg" width="135" height="40" alt="Télécharger dans l'App Store">`
   puis `<span class="sr-only"> (nouvel onglet)</span>`. Hauteur affichée **44 px** (minimum Apple :
   40 px), largeur auto. Espace libre ≥ 11 px tout autour (un quart de la hauteur, règle Apple).
   **Aucun effet au survol** ; focus : anneau standard.
2. **Google Play, bientôt** : `<span class="store-badge store-badge-soon">` contenant
   `<img src="/assets/badges/google-play-bientot.svg" width="135" height="40" alt="Bientôt disponible sur Google Play">`.
   Non focalisable, non cliquable, `opacity: .6`, `cursor: default`. Composant inactif : exempté
   des exigences de contraste (1.4.3), et son texte alternatif est explicite.
3. Note `.store-note` (15 px, `--ink-2` sur clair) : « Sur iPhone dès maintenant. Sur Android,
   l'application est en test : bientôt sur Google Play. »

À moins de 360 px, les deux badges passent l'un sous l'autre sans réduction sous 40 px.
Le jour de la sortie Android (étape 14) : remplacer le `span` par un lien vers la fiche Google
Play avec le badge officiel « Disponible sur Google Play », retirer l'opacité et la note.

### 6.16 Tableau comparatif (`.compare`)

Voir `pages/tarifs.md` §3. `<table>` sémantique avec `<caption>`, `<th scope="col">` pour les
plans, `<th scope="row">` pour les lignes. Conteneur `.table-scroll` : `overflow-x: auto`,
`tabindex="0"`, `role="region"`, `aria-labelledby` vers la légende. Cellules :
coche `#i-check` verte + `<span class="sr-only">Inclus</span>` ; tiret `#i-minus` `--ink-2` +
`<span class="sr-only">Non inclus</span>` ; texte en clair pour « En option », effectifs et prix.
Les lignes non incluses ne sont **jamais** atténuées par opacité (défaut du site actuel).
Lignes alternées `--paper` / `--surface`. Pas d'en-tête de colonnes collant : le conteneur en
`overflow-x: auto` devient la référence du positionnement collant et ne défile pas verticalement,
la règle serait sans effet (le tableau ne fait que 12 lignes). Anneau de focus du conteneur
dessiné vers l'intérieur (`outline-offset: -3px`), car il peut toucher les bords de l'écran.

### 6.17 Carte tarif (`.plan`)

Fond `--surface`, bordure `--line`, rayon 16 px, padding `28px 24px`, colonne flex (le bouton
reste en bas). Ordre : `h3` nom ; `.plan-scale` effectif (vert 600 15 px) ; `.plan-price`
(« dès » en 16 px `--ink-2` si Premium, montant, puis « FCFA par an » sur sa propre ligne en
14 px 600 `--ink-2`) ; `.plan-eq` équivalences (14,5 px `--ink-2`) ; `.ticks` ; bouton
`.btn-outline` pleine largeur. **Aucune carte mise en avant**, aucune mention « le plus choisi ».
Grille : 1 colonne < 900 px, 3 colonnes ≥ 900 px, ordre Essentiel, Standard, Premium.

### 6.18 Étapes numérotées (`.steps`)

`<ol>` de 3 cartes. Numéro dans une pastille ronde 36 px fond `--night`, chiffre or Outfit 600
(généré par `counter`, le `<ol>` porte la sémantique). 1 colonne < 760 px, 3 colonnes au-delà.

### 6.19 Questions fréquentes (`.faq`)

`<details>` / `<summary>` natifs, **sans JS** et sans attribut `name` (plusieurs questions
peuvent rester ouvertes : plus simple à relire). Chaque `details` : fond `--surface`, bordure
`--line`, rayon 12 px, `gap: 10px` entre questions. `summary` : 600, 17 px, `min-height: 52px`,
`padding: 16px 52px 16px 20px`, marqueur natif masqué, signe « + » vert à droite (devient
« − » à l'ouverture), contenu `padding: 0 20px 18px`, `--ink-2`. Le `summary` n'a pas de titre
`hN` à l'intérieur (lecture plus propre) ; la section porte le `h2` « Questions fréquentes ».
Ouverture : fondu du contenu 150 ms (`@keyframes` opacité), coupé en mouvement réduit.

### 6.20 Appel final (`.closer`)

Bande pleine largeur fond `--night` + dégradé radial or 16 % en haut au centre, texte centré,
`padding-block: var(--section-y)`. `h2` centré (`margin-inline: auto`), paragraphe
`--on-night-2`, rangée de boutons centrée : `.btn-gold` + `.btn-outline-light`.

### 6.21 Photo d'illustration (`.photo`)

`<figure>` avec `<picture>` (§8.1), rayon 16 px, `object-fit: cover`, ratio fixé par la fiche de
page. Légende obligatoire `<figcaption>` 13 px `--ink-2` : « Photo d'illustration : [lieu]
(Wikimedia Commons, CC0). » Les photos montrent des élèves d'Afrique de l'Ouest, **jamais
présentés comme des clients**.

### 6.22 Texte juridique (`.legal`)

Colonne `max-width: 46rem`, `padding: 48px var(--pad) 96px`. `h1`, date (15 px `--ink-2`),
`h2` (22 px, `margin-top: 48px`), `h3` (18 px). Tableaux : bordure `--line`, en-tête fond
`--paper-2` texte `--ink`, cellules `padding: 11px 14px`, conteneur `.table-scroll` (§6.16)
sous 600 px. Liens soulignés `--green`.

### 6.23 États

Le site est statique : pas d'état de chargement ni d'état vide applicatif. États prévus :

| État | Traitement |
|---|---|
| Image en cours de chargement | `width`/`height` renseignés (aucun décalage) et fond `--paper-2` (clair) ou `--night-2` (nuit) sur le cadre. |
| Image absente / bloquée | Le texte alternatif s'affiche dans le cadre ; aucune information n'est portée par l'image seule. |
| Sans JavaScript | Tout fonctionne : menu et FAQ en `<details>`. Seules disparaissent les fermetures par Échap et clic extérieur du menu. |
| Police pas encore chargée | `font-display: swap` + police de repli aux métriques ajustées (§8.3). |
| Page inexistante | `404.html` (statut HTTP 404, §10.4). |
| Impression | `@media print` : en-tête, menu, rangées de boutons et appels finaux masqués ; texte noir sur blanc ; URL des liens externes affichée après le lien dans `.legal` (`a[href^="http"]::after { content: " (" attr(href) ")" }`). |
| Contraste forcé (Windows) | Bordures transparentes des boutons visibles, icônes en `currentColor`, focus `outline` (pas `box-shadow`). |

---

## 7. Accessibilité (WCAG 2.2 niveau AA)

### 7.1 Structure

- `<html lang="fr">`, un seul `h1` par page, niveaux sans saut, titres de pied de page en `h2`.
- Repères : `header`, `nav` (étiquetés, voir §6.1), `main#contenu`, `footer`. Sections avec
  `aria-labelledby`.
- Ordre du DOM = ordre visuel (pas de `order` CSS qui inverserait la lecture ; les mises en page
  « image à gauche » se font en grille avec l'image placée par `grid-column`, le texte restant
  avant l'image dans le DOM).

### 7.2 Clavier et focus

- Tout est atteignable et utilisable au clavier ; aucun `tabindex` positif.
- `:focus-visible` : `outline: 3px solid var(--green); outline-offset: 3px; border-radius: 8px`
  sur fond clair ; dans `.topbar`, `.hero`, `.closer`, `.footer` : `outline-color: var(--gold)`.
  Contraste de l'anneau : 7,2:1 (vert sur papier), 6,8:1 (or sur nuit).
- Focus jamais masqué par l'en-tête collant : `scroll-padding-top` (§6.1) (critère 2.4.11).
- Menu mobile : Entrée / Espace ouvrent le `summary` ; Échap ferme et rend le focus au `summary`.

### 7.3 Cibles

Boutons 48 px, petits boutons et `summary` 44 px, liens de menu 48 px, liens de pied de page et
de texte ≥ 24 px de haut grâce au `padding-block`. Critère 2.5.8 (24 px) respecté partout,
44 px visés pour tout contrôle isolé.

### 7.4 Textes alternatifs

- Captures : `alt` qui décrit ce que montre l'écran (textes donnés dans chaque fiche).
  Capture dupliquée ou purement décorative (capture d'arrière-plan du haut de page) : `alt=""`
  et `aria-hidden="true"` sur la `figure`.
- Photos : `alt` descriptif sans prétendre qu'il s'agit d'une école cliente.
- Icônes : `aria-hidden="true" focusable="false"` ; le sens est toujours porté par le texte voisin.
- Icône de marque dans l'en-tête : `alt=""` (le texte « School Manager » nomme le lien).

### 7.5 Couleur et contraste

Contrastes du §4.1 uniquement. L'or n'est jamais du texte ni un anneau de focus sur fond clair.
« Inclus / non inclus » du tableau : forme d'icône différente + texte masqué (jamais la seule
couleur). Liens soulignés dans les paragraphes.

### 7.6 Langue et typographie française

- Espace insécable (`&nbsp;`) : dans les nombres (`180&nbsp;000`), avant « FCFA », avant « : »,
  « ? », « ! », « ; », à l'intérieur des guillemets « », entre « 30 » et « jours », dans
  « +223&nbsp;79&nbsp;82&nbsp;79&nbsp;79 ».
- « 1ère », « 9ème » écrits ainsi (usage malien, cohérent avec l'application).
- Montants toujours « 180 000 FCFA par an » (jamais « 180k », jamais « F »).

### 7.7 Divers

- Zoom 200 % et 400 % sans perte (unités `rem`/`em`, pas de hauteur fixe sur les blocs de texte).
- Espacement du texte modifiable (1.4.12) : pas de `overflow: hidden` sur un bloc de texte.
- Liens qui ouvrent un nouvel onglet annoncés (§6.4).
- `prefers-reduced-motion` respecté (§4.5).
- Pas de contenu qui clignote, pas de carrousel automatique.

---

## 8. Ressources

Tout est produit à l'étape 4 dans `landing/assets/`. Chemins publics = `/assets/...`.

### 8.1 Photos (3, toutes d'Afrique de l'Ouest)

| Source | Sujet (vérifié sur Wikimedia Commons) | Fichiers à produire | Utilisée sur |
|---|---|---|---|
| `landing/assets/hero-classe.jpg` (1280 × 852) | « Students reading in a classroom », Bright Kwame Ayisi, Amanya, région d'Oti, **Ghana**, CC0 | `/assets/photos/classe-{480,800,1200}.{avif,webp,jpg}` | Accueil §4 |
| `landing/assets/scolarite.jpg` (1280 × 900) | « Pupils studying a book », Bright Kwame Ayisi, **Ghana**, CC0 | `/assets/photos/eleves-lecture-{480,800,1200}.{avif,webp,jpg}` | Accueil §7, Parents §1 |
| `landing/assets/bulletins.jpg` (1280 × 719) | « Students writing notes », Onwuka Glory, **Nigeria** (catégorie « Read Aloud Day by WfMin Nigeria 2024 »), CC0 | `/assets/photos/cahiers-{480,800,1200}.{avif,webp,jpg}` | Fonctionnalités §3 |

Correction : `PROPOSITION.md` §5.5 attribuait `bulletins.jpg` au Ghana ; elle vient du Nigeria
(Afrique de l'Ouest, conservée). Le 1200 px est produit par recadrage, pas agrandi : si la source
fait 1280 px, le fichier 1200 est simplement redimensionné.

Balisage type :

```html
<picture>
  <source type="image/avif" srcset="/assets/photos/classe-480.avif 480w, /assets/photos/classe-800.avif 800w, /assets/photos/classe-1200.avif 1200w" sizes="(min-width: 56.25em) 520px, 100vw">
  <source type="image/webp" srcset="(idem en .webp)" sizes="(idem)">
  <img src="/assets/photos/classe-800.jpg" width="1200" height="799" alt="…" loading="lazy" decoding="async">
</picture>
```

Qualité : AVIF q50, WebP q75, JPEG q78 progressif. Cible : < 60 Ko pour le 800 px en AVIF.
À supprimer de `landing/assets/` : `ecriture.jpg`, `mobile-classe.jpg`, `campus.jpg`. Les 3 JPEG
sources restent dans `landing/assets/` (ou passent dans `marketing/`) mais ne sont plus
référencés par les pages. Mettre à jour `landing/assets/CREDITS.md` (3 photos + auteurs).

### 8.2 Captures (les 5 existantes uniquement)

Source : `marketing/play-store/capture-N-*.png` (1080 × 1920, données de démonstration).
Production : WebP q80 en **540 × 960** et **360 × 640**, recadrage aucun (le cadre CSS recadre).

| Source | Fichiers | Ce que montre l'écran |
|---|---|---|
| `capture-1-feuille-appel.png` | `/assets/captures/appel-{360,540}.webp` | Feuille d'appel, classe 6ème année A, 22 oct. 2026, compteurs, boutons Présent / Absent / Retard / Départ anticipé par élève |
| `capture-2-notes.png` | `/assets/captures/notes-{360,540}.webp` | Liste des évaluations de l'enseignant (47), date et barème sur 20 |
| `capture-3-emploi-du-temps.png` | `/assets/captures/emploi-du-temps-{360,540}.webp` | Emploi du temps du jeudi, cours par heure, matière, salle, classe |
| `capture-4-accueil.png` | `/assets/captures/accueil-{360,540}.webp` | Accueil de l'espace établissement : Feuille d'appel, Notes, Emploi du temps, Notifications |
| `capture-5-connexion.png` | `/assets/captures/connexion-{360,540}.webp` | Écran de connexion : Sous-domaine, E-mail, Mot de passe, Se connecter |

Balisage : `<img src="/assets/captures/appel-540.webp" srcset="/assets/captures/appel-360.webp 360w, /assets/captures/appel-540.webp 540w" sizes="(min-width: 56.25em) 270px, 68vw" width="540" height="960" alt="…">`.
**Ces 5 captures sont des écrans enseignant ou communs** : ne jamais les légender comme des
écrans parent.

### 8.3 Polices

- Fichiers : `/assets/fonts/inter-400.woff2`, `/assets/fonts/inter-600.woff2`,
  `/assets/fonts/outfit-600.woff2`, sous-ensemble **latin** de Google Fonts (inclut accents
  français, œ, ’, « », espace fine U+202F). Source : paquets `@fontsource/inter` et
  `@fontsource/outfit` (licence SIL OFL 1.1, copier `OFL.txt` dans `/assets/fonts/`).
- `@font-face` avec `font-display: swap` et `unicode-range` du sous-ensemble latin.
- `<link rel="preload" as="font" type="font/woff2" crossorigin>` pour `inter-400` et
  `outfit-600` seulement.
- Polices de repli aux métriques ajustées (`size-adjust`, `ascent-override`,
  `descent-override`) sur `local("Arial")`, valeurs calculées avec un outil comme Fontaine ou
  Capsize (ne pas les estimer à la main), pour limiter le décalage au chargement.
- Plus aucun appel à `fonts.googleapis.com` ni `unpkg.com`.

### 8.4 Icônes

Sprite SVG **inline**, identique sur toutes les pages, placé juste après `<body>` dans un
`<svg class="sprite" aria-hidden="true" focusable="false">` (classe CSS
`.sprite { position:absolute; width:0; height:0; overflow:hidden }`, **pas d'attribut `style`**
à cause de la CSP). Trait 1,75 px, coins arrondis, `fill: none; stroke: currentColor`.

Symboles : `i-check`, `i-minus`, `i-arrow`, `i-whatsapp`, `i-mail`, `i-menu`, `i-close`,
`i-book`, `i-calendar`, `i-coins`, `i-chat`, `i-bus`, `i-chart`, `i-shield`, `i-users`,
`i-export`, `i-lock`, `i-bell`, `i-child` (deux silhouettes, plusieurs enfants), `i-receipt`,
`i-clock`, `i-server`, `i-eye-off`. Les 14 premiers existent dans la maquette ; les autres
sont à dessiner dans le même style.

### 8.5 Marque, favicons, image de partage

| Fichier | Source | Taille |
|---|---|---|
| `/favicon.ico` | `marketing/play-store/icone-512.png` | 32 × 32 |
| `/assets/brand/favicon-32.png` | idem | 32 × 32 |
| `/apple-touch-icon.png` | idem | 180 × 180 |
| `/assets/brand/icone-72.png` | idem | 72 × 72 (affichée 36 px dans l'en-tête, 32 px dans le pied) |
| `/assets/og/school-manager-1200x630.jpg` | composition à créer | 1200 × 630, < 150 Ko |

Composition de l'image de partage (script Python dans la lignée de
`marketing/play-store/generer.py`) : fond `#0b231e`, icône 140 px en haut à gauche, titre
« School Manager » Outfit 600 72 px `#f1f5f3`, sous-titre « La gestion scolaire des écoles
privées du Mali » Inter 34 px `#b9ccc5`, filet or 6 px sous le titre ; à droite, la capture
`capture-1-feuille-appel.png` à 300 px de large, coins arrondis, coupée en bas. Marges ≥ 64 px
(zone sûre des aperçus WhatsApp et Facebook). Aucun prix ni chiffre sur l'image.

### 8.6 Badges

| Fichier | Source |
|---|---|
| `/assets/badges/app-store-fr.svg` | **Fichier officiel** du kit « App Store Marketing Tools » d'Apple, version française noire « Télécharger dans l'App Store ». Apple interdit de recréer soi-même un badge localisé : le SVG reproduit du site actuel est remplacé. |
| `/assets/badges/google-play-bientot.svg` | Extraction du SVG inline actuel de `landing/index.html` (badge « Bientôt sur Google Play ») dans un fichier, sans modification. |

---

## 9. Captures futures (étape 3 reportée)

Aujourd'hui, aucune capture ne montre l'écran d'un parent ni la plateforme web. La spec
n'affiche **que** les captures existantes ; ce tableau dit quoi remplacer quand l'étape 3 aura
produit les nouvelles (données de démonstration uniquement, `PLAN.md` §1.5).

| Page, section | Aujourd'hui | Demain | Modification à faire |
|---|---|---|---|
| Accueil §1, haut de page | `appel` (avant) + `accueil` (arrière, ≥ 900 px) | Inchangé, ou `accueil` remplacé par le tableau de bord web de la direction en cadre « navigateur » | Changer la source de `.shot-back` |
| Accueil §5, trois publics | Cartes sans image + rail `notes`, `emploi-du-temps`, `connexion` | Une capture en tête de chaque carte : tableau de bord web (direction), `notes` (enseignants), notification d'absence (parents) | Ajouter un `.shot` en tête de chaque `.role`, supprimer le rail |
| Accueil §7, côté parents | Photo `eleves-lecture` | Écran parent « notes par matière » ou « notification d'absence » | Remplacer `.photo` par `.shot` (même emplacement, ratio 9/15) |
| Fonctionnalités §3, direction | Photo `cahiers` | Tableau de bord web + bulletin PDF | Remplacer `.photo` par deux `.shot` |
| Fonctionnalités §4, comptabilité | Pas d'image | Écran de suivi des impayés (web) | Ajouter un `.shot` à droite (grille `1fr 1fr` ≥ 900 px) |
| Fonctionnalités §8, familles | `connexion` | Écrans parents « scolarité » et « notes » | Remplacer par un rail de 2 ou 3 écrans parents |
| Parents §1, introduction | Photo `eleves-lecture` | Deux écrans parents superposés, comme le haut de page de l'accueil | Remplacer `.photo` par `.hero-visual` clair |
| Parents §3, ce que vous voyez | Cartes à icône | Rail de 3 écrans parents sous les cartes | Ajouter un `.shots-rail` |
| Image de partage | Composition avec `appel` | Idem avec un écran parent pour `/parents` (image dédiée) | `og:image` propre à `/parents` |

Captures à produire (rappel) : tableau de bord web direction, bulletin PDF, suivi des impayés,
écrans parents (notification d'absence, notes par matière, scolarité et reçus).

---

## 10. Technique, SEO, Nginx

### 10.1 Fichiers et poids

- `landing/` : 8 pages HTML, `robots.txt`, `sitemap.xml`, `favicon.ico`, `apple-touch-icon.png`,
  `assets/site.vN.css` (actuellement v3), `assets/site.vN.js` (actuellement v2), `assets/{photos,captures,fonts,brand,badges,og}/`.
  `styles.css` et `main.js` actuels supprimés une fois toutes les pages migrées.
- Pas de framework, pas de compilation. En-tête, pied de page et sprite **copiés à l'identique**
  dans chaque page, encadrés par `<!-- DÉBUT EN-TÊTE COMMUN v1 -->` / `<!-- FIN EN-TÊTE COMMUN v1 -->`
  (idem pied et sprite) pour faciliter les mises à jour ; seuls changent `aria-current` et le
  message WhatsApp.
- Versionnement par nom de fichier : `site.vN.css` devient `site.vN+1.css` à chaque modification (ancien fichier supprimé),
  et toutes les pages sont mises à jour dans le même commit.
- Aucun attribut `style=""`, aucun script inline exécutable (CSP). Les blocs
  `<script type="application/ld+json">` sont autorisés (non exécutés).
- `site.vN.js` (actuellement `site.v2.js`, `defer`, < 2 Ko) : uniquement le comportement du menu mobile (§6.1).
- Budgets (transfert compressé, premier chargement) : premier écran < 150 Ko ; accueil complet
  < 500 Ko ; JS < 10 Ko ; aucune ressource tierce.
- `loading="lazy"` sur toute image sous la ligne de flottaison ; `fetchpriority="high"` sur la
  seule capture principale du haut de page de l'accueil, et sur rien d'autre.

### 10.2 `<head>` commun (ordre)

```html
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>…</title>
<meta name="description" content="…">
<link rel="canonical" href="https://schoolmanager.tech/…">
<meta name="theme-color" content="#0b231e">
<meta name="color-scheme" content="light">
<link rel="icon" href="/favicon.ico" sizes="32x32">
<link rel="apple-touch-icon" href="/apple-touch-icon.png">
<link rel="preload" href="/assets/fonts/inter-400.woff2" as="font" type="font/woff2" crossorigin>
<link rel="preload" href="/assets/fonts/outfit-600.woff2" as="font" type="font/woff2" crossorigin>
<link rel="stylesheet" href="/assets/site.v3.css">
<script src="/assets/site.v2.js" defer></script>
<meta property="og:type" content="website">
<meta property="og:site_name" content="School Manager">
<meta property="og:locale" content="fr_FR">
<meta property="og:title" content="…">
<meta property="og:description" content="…">
<meta property="og:url" content="https://schoolmanager.tech/…">
<meta property="og:image" content="https://schoolmanager.tech/assets/og/school-manager-1200x630.jpg">
<meta property="og:image:width" content="1200">
<meta property="og:image:height" content="630">
<meta property="og:image:alt" content="School Manager, la gestion scolaire des écoles privées du Mali, avec la feuille d'appel de l'application.">
<meta name="twitter:card" content="summary_large_image">
```

Les valeurs propres à chaque page (`title`, `description`, `canonical`, `og:*`) sont dans les
fiches. URL canoniques **sans extension** (`/tarifs`), sauf `/confidentialite.html`.

### 10.3 Données structurées

Sur l'accueil uniquement (texte exact dans `pages/accueil.md` §14) : `Organization` (LOGILAB)
et `SoftwareApplication` (School Manager, `offers` en XOF à partir de 180 000). Pas d'avis ni de
note (nous n'en avons pas) : pas d'étoiles dans les résultats, c'est attendu. Pas de balisage
FAQ (aucun résultat enrichi possible pour un site de ce type depuis 2023).

### 10.4 `robots.txt` et `sitemap.xml`

```text
User-agent: *
Allow: /

Sitemap: https://schoolmanager.tech/sitemap.xml
```

`sitemap.xml` : les 7 URL canoniques (`/`, `/fonctionnalites`, `/parents`, `/tarifs`,
`/securite`, `/confidentialite.html`, `/mentions-legales`), `lastmod` = date de mise en ligne.
La 404 n'y figure pas et porte `<meta name="robots" content="noindex">`.

### 10.5 À faire côté Nginx (agent-devops, étape 10)

Configuration actuelle : `deploy/nginx/conf.d/app.conf.template`, bloc « Site vitrine ».

1. `error_page 404 /404.html;` dans le bloc du site (aujourd'hui `try_files … =404` renvoie la
   page 404 par défaut de Nginx).
2. Cache : `*.v[0-9]+.(css|js)` et `/assets/fonts/` en `Cache-Control: public, max-age=31536000, immutable` ;
   images en 7 jours ; HTML en `no-cache`. Ajouter `avif` et `ico` à l'expression des
   ressources (absents aujourd'hui).
3. `gzip_types` : ajouter `image/svg+xml` (absent de `deploy/nginx/nginx.conf` ligne 31).
4. CSP du domaine nu :
   `default-src 'self'; img-src 'self' data:; style-src 'self'; script-src 'self'; font-src 'self'; connect-src 'self'; base-uri 'self'; form-action 'none'; frame-ancestors 'none'`.
5. Redirections 301 : `www.` vers le domaine nu ; `/index.html` vers `/` ; `/<page>.html` vers
   `/<page>` **sauf** `/confidentialite.html` (URL citée dans les fiches stores).
6. Aucune mesure d'audience (décision d'Oumar).

---

## 11. Points non tranchés (à valider par Oumar)

1. **RCCM / NIF de LOGILAB** et **forme juridique** : non fournis. Lignes en commentaire dans
   `mentions-legales.html`. Adresse : seule « Bamako, Mali » est affichée pour LOGILAB ; la
   politique de confidentialité cite « Kalaban Coura ACI » pour Oumar Mory Coulibaly. Faut-il
   l'afficher aussi pour l'éditeur ?
2. **Notifications push et politique de confidentialité** : `FcmNotificationGateway` existe et
   s'active avec `app.fcm.enabled`. Si c'est le cas en production, Firebase (Google) reçoit
   des jetons d'appareil et le contenu des notifications. La politique dit le contraire
   (« Si un service de notifications est ajouté, ce tableau sera mis à jour avant toute mise en
   service ») et affirme que « la seule permission demandée […] est l'accès à Internet », alors
   qu'iOS demande l'autorisation des notifications. **Corrigé** par le commit 6c6963b
   (`fix/parcours-mobile-et-web`), reporté mot pour mot dans la nouvelle page : date au
   23 octobre 2026, permission de notification, jeton de notification, sous-traitant Google
   (Firebase Cloud Messaging) ; `/securite` le mentionne aussi. Le site promet
   « prévenu dès que l'absence est saisie » (repris de la fiche App Store publiée) : à confirmer
   en production.
3. **Localisation du serveur** : « Union européenne » vient de la politique de confidentialité ;
   le datacenter exact du VPS Hostinger n'est pas documenté dans `DEPLOIEMENT.md`.
4. **Essai sur le plan Essentiel** : le site actuel (même après l'étape 0) dit « L'essai couvre
   l'intégralité des modules » et « Essai de 30 jours sur tous les plans ». Le code démarre
   l'essai sur Essentiel, sans cantine, transport ni bibliothèque. La nouvelle spec dit la
   vérité. **Corrigé** en ligne par le commit 6c6963b ; dans le nouveau site, le bouton du plan
   Standard devient « Choisir Standard » (→ `/register`) pour ne pas suggérer un essai Standard.
5. **« Cantine, transport, bibliothèque en option » (Standard)** : repris du site actuel. Dans
   le code, ces modules sont inclus dès que le plan Standard est souscrit, sans prix d'option.
   Si une option payante existe, en donner le prix ; sinon écrire « inclus ».
6. **« Statistiques de direction » absentes d'Essentiel** : repris du site actuel, alors que les
   statistiques de base existent pour tous. Confirmer la règle commerciale.
7. **Modules non affectés à un plan** (cahier de textes, documents, vie scolaire, messagerie,
   certificats, cartes scolaires) : décrits sur `/fonctionnalites`, mais absents du tableau
   comparatif tant qu'Oumar n'a pas dit dans quels plans ils figurent.
8. **« Abonnement annuel »** : repris du site. La facturation Stripe actuelle est mensuelle
   (`ARCHITECTURE.md`, « périodicité annuelle hors périmètre »). Les modalités de règlement
   restent hors du site (« écrivez-nous »).
9. **Délais après l'essai** (lecture seule, puis suspension) : non chiffrés sur le site, car
   « à confirmer avec le porteur de projet » dans `ARCHITECTURE.md`.
10. **Badge « Bientôt sur Google Play »** : conservé à la demande d'Oumar. Les règles de marque
    Google n'autorisent en principe que le badge officiel non modifié ; risque faible pendant
    le test, à retirer au profit du badge officiel dès la sortie.
11. **Tableau des rôles** de `/fonctionnalites` §9 : rédigé d'après la politique de
    confidentialité et la fiche store ; agent-relecteur doit le confronter aux `@PreAuthorize`.

---

## Sources (veille du 23/10/2026)

- W3C, « What's New in WCAG 2.2 » : 2.4.11 focus non masqué, 2.5.8 taille des cibles (24 px),
  3.2.6 aide cohérente. https://www.w3.org/WAI/standards-guidelines/wcag/new-in-22/
- WebAIM, « Disclosures » et Hassell Inclusion, « Accessible accordions part 2 : using details
  and summary » : `<details>` natif accessible au clavier et aux lecteurs d'écran, état annoncé.
  https://webaim.org/techniques/disclosures/ ·
  https://hassellinclusion.com/blog/accessible-accordions-part-2-using-details-summary/
- Apple, « Marketing Resources and Identity Guidelines » : hauteur minimale 40 px à l'écran,
  espace libre d'un quart de la hauteur, ne jamais créer soi-même un badge localisé.
  https://developer.apple.com/app-store/marketing/guidelines/
- Google Search Central : données structurées `SoftwareApplication` (note requise pour le
  résultat enrichi) et fin des résultats enrichis FAQ (août 2023).
  https://developers.google.com/search/docs/appearance/structured-data/software-app ·
  https://developers.google.com/search/blog/2023/08/howto-faq-changes
- Wikimedia Commons : pages des 3 photos (auteurs, lieux, CC0), voir `pages/mentions-legales.md` §6.
- Hostinger International Ltd, siège : 61 Lordou Vironos Street, 6023 Larnaca, Chypre
  (https://www.datarequests.org/company/hostinger/). Cloudflare, Inc. : 101 Townsend Street,
  San Francisco, CA 94107 (rapport annuel 10-K, https://www.sec.gov/Archives/edgar/data/1477333/000147733326000016/cloud-20251231.htm).
