# Refonte du site vitrine schoolmanager.tech : proposition

Statut : **proposition à valider par Oumar**. Aucun fichier de `landing/` n'est modifié.
Maquette navigable de la nouvelle page d'accueil : `docs/refonte-site/maquette/index.html`
(ouvrir directement dans le navigateur).

Références internes : `marketing/reseaux-sociaux/PLAN.md` (positionnement, ton, §1.5 « ce
qu'on ne dit jamais »), `docs/FICHES-STORES.md` §2, `docs/DESIGN.md` (palette et typographie
de l'application).

---

## 1. Audit du site actuel

### 1.1 Points forts à garder

- **Tarifs publics en FCFA**, avec l'équivalent mensuel et par élève. C'est rare et rassurant.
- **Honnêteté** : pas de faux témoignage, pas de faux logo client.
- **Identité vert / or** déjà cohérente avec l'icône et l'application.
- **La phrase « Trois semaines pour sortir les bulletins. Un cahier d'appel égaré. »** : elle
  nomme la situation avant le produit. C'est le bon ton, celui du plan réseaux sociaux.
- **Bonnes bases techniques** : lien d'évitement, `prefers-reduced-motion`, `width`/`height`
  sur les images, animation en amélioration progressive (le contenu reste visible sans JS),
  badge App Store sans effet au survol.
- **Sécurité expliquée** (isolation par école, droits par rôle, export avant suppression).
- **Lien WhatsApp** déjà présent.

### 1.2 Problèmes

| Thème | Constat | Gravité |
|---|---|---|
| **Incohérences factuelles** | « Disponible sur Android et iOS » (Android est en test fermé). « Messagerie interne » sans préciser qu'elle n'existe que sur le web. | Bloquant |
| | Le bouton « Demander une démo » du haut de page renvoie vers `#tarifs`, pas vers WhatsApp. | Bloquant |
| | Pied de page en production : « Mentions légales et politique de confidentialité à compléter avant mise en ligne ». Aucune page de mentions légales. | Bloquant |
| | « Support par e-mail » promis, mais aucune adresse e-mail n'est affichée. | Majeur |
| | « L'appel se fait en trente secondes » : promesse non mesurée (PLAN.md §0.7 demande de la formuler comme un objectif). | Majeur |
| | Ordre des tarifs Essentiel, **Premium**, Standard : on lit les prix dans le désordre. | Majeur |
| | Plusieurs affirmations à confirmer avant de les reprendre : « domaine personnalisé », « engagement de service », « export PDF conforme à votre modèle », « support par chat ». | À vérifier |
| **Clarté du message** | Le haut de page ne dit ni Mali, ni écoles privées, ni fondamental, ni LOGILAB. « Établissements francophones » est générique. Il cite cantine et transport, absents du plan Essentiel. | Majeur |
| **Parcours parent** | Inexistant. Or les posts « parents » du plan réseaux sociaux renvoient vers le site. Un parent n'y apprend pas que l'application est gratuite pour lui, ni que l'école doit l'inscrire, ni comment obtenir ses identifiants. Tous les boutons mènent à `/register`, réservé aux écoles. | Majeur |
| **Preuves** | Aucune capture du produit, alors que 5 captures réelles existent. Les photos, passées en monochrome vert, montrent des salles de classe mais pas le produit. 3 photos sur 6 ne sont pas d'Afrique de l'Ouest ni du fondamental (`ecriture.jpg`, `mobile-classe.jpg` prise en Allemagne, `campus.jpg` dans une université sud-africaine). Le chiffre « 0 donnée partagée » fait gadget. | Majeur |
| **Conversion** | Aucune réponse aux objections : que se passe-t-il après 30 jours ? Comment paie-t-on ? Faut-il une bonne connexion ? Que deviennent mes données si j'arrête ? Pas de FAQ. Pas de mesure d'audience, donc impossible de savoir ce qui convertit. | Majeur |
| **Mobile** | Sous 960 px, les liens de navigation disparaissent sans menu de remplacement : sur téléphone, on ne peut pas aller directement aux tarifs. Sous 560 px, le nom « School Manager » disparaît de l'en-tête. L'en-tête collant de 68 px masque le haut des sections visées par les ancres (pas de `scroll-padding-top`). | Majeur |
| **Performance / connexions lentes** | Icônes Phosphor chargées par un script tiers (unpkg) qui télécharge une police d'icônes complète. Deux familles Google Fonts sur 6 graisses, servies par un tiers. Photos JPEG en 1280 px pour tous les écrans, sans WebP/AVIF ni `srcset`. Filtres CSS (`grayscale`, `mix-blend-mode`, `backdrop-filter`) coûteux sur un téléphone d'entrée de gamme. Poids réel à mesurer (Lighthouse), mais l'architecture n'est pas pensée pour la 3G. | Majeur |
| **SEO** | Une seule page, donc peu de requêtes couvertes (« logiciel de gestion scolaire Mali », « bulletin scolaire en ligne »…). Pas de `canonical`, pas d'`og:image`, pas de favicon ni d'icône Apple, pas de données structurées, pas de `robots.txt` ni de `sitemap.xml`. Titre générique sans « Mali ». | Majeur |
| **Accessibilité** | Contrastes globalement bons sur fond sombre. Mais : texte sur photo assombrie (fragile), lignes « désactivées » des tarifs à 62 % d'opacité (contraste insuffisant), focus masquable par l'en-tête collant (WCAG 2.2, critère 2.4.11), un lien WhatsApp ouvre un nouvel onglet sans le dire, niveaux de titres sautés dans le pied de page (`h4` après `h2`), pas d'autre moyen de navigation sur mobile. | Moyen |
| **Identité** | Le logo du site est une icône Phosphor générique, différente de la vraie icône de l'application (toque blanche, gland or). Le site est entièrement sombre, alors que l'application est claire avec une barre vert foncé : l'utilisateur ne reconnaît pas le produit. Le fond sombre se lit aussi moins bien en plein soleil sur téléphone. | Moyen |

> **Correctif immédiat recommandé, indépendant de la refonte** (étape 0 du plan) : les
> publications réseaux sociaux démarrent le 26/10. Corriger dès maintenant, sur le site
> actuel, la mention Android, la précision « messagerie sur le web », le lien « Demander une
> démo » et la phrase « à compléter avant mise en ligne ».

---

## 2. Objectifs et indicateurs

| Objectif | Indicateur | Cible |
|---|---|---|
| Un directeur comprend l'offre en 5 secondes | Test de 5 secondes avec 3 à 5 directeurs : « Qu'est-ce que c'est, pour qui, combien ? » | 4 sur 5 répondent juste |
| Plus d'écoles qui essaient | Clics « Essai gratuit » vers `/register`, essais démarrés | Mesurer le premier mois, puis fixer la cible. Pas de chiffre inventé. |
| Plus de conversations | Clics WhatsApp (message prérempli qui indique la page d'origine) | Idem |
| Les parents deviennent prescripteurs | Visites de `/parents`, clics App Store, partages « Montrer à mon directeur » | Idem |
| Site utilisable en 3G | LCP mobile (Lighthouse, réseau lent simulé), poids de page | LCP < 2,5 s, accueil < 500 Ko au total, < 150 Ko pour le premier écran |
| Accessible | axe / Lighthouse accessibilité, test clavier, zoom 200 %, largeur 320 px | 0 erreur axe, score ≥ 95 |
| Trouvable | Pages indexées (Search Console), requêtes « gestion scolaire Mali » | 6 pages indexées sous 1 mois |

La mesure suppose un outil d'audience (question n° 3).

---

## 3. Nouvelle arborescence

Principe : **peu de pages, chacune pour une décision**. Une petite équipe doit pouvoir les
tenir à jour. URL propres grâce au `try_files $uri.html` déjà présent dans Nginx.

### 3.1 Lancement (à faire maintenant)

| Page | URL | Cible | Rôle |
|---|---|---|---|
| **Accueil** | `/` | Directeurs d'abord, aiguillage parents | Comprendre, être rassuré, démarrer l'essai ou écrire sur WhatsApp |
| **Fonctionnalités** | `/fonctionnalites` | Directeurs, promoteurs | Le détail des modules par rôle (direction, secrétariat, comptabilité, enseignants, parents, élèves). Page de référence SEO. Indique clairement ce qui est sur le web et ce qui est sur mobile. |
| **Parents** | `/parents` | Parents | Ce que montre l'application, gratuite pour eux. Comment obtenir ses identifiants. App Store, Android en test. Bouton « Montrer cette page à mon directeur » (partage WhatsApp). C'est la cible des posts « parents ». |
| **Tarifs** | `/tarifs` | Directeurs | Les 3 plans, tableau comparatif, palier à l'élève, essai, modalités de paiement, FAQ achat |
| **Sécurité et confidentialité** | `/securite` + `/confidentialite.html` conservé | Directeurs, parents | Isolation par école, rôles, sauvegardes, export. **L'URL `/confidentialite.html` ne change pas** : elle est citée dans les fiches App Store et Google Play. |
| **Mentions légales** | `/mentions-legales` | Obligation | Éditeur LOGILAB, hébergeur, crédits photos. Aujourd'hui absente. |
| Page 404 | `/404.html` | Tous | Retour à l'accueil, WhatsApp |

**Contact et démo : pas de page dédiée.** Un formulaire demanderait un serveur, une boîte de
réception et un traitement des données. Le lien WhatsApp prérempli fait mieux pour la cible :
il est présent au même endroit sur chaque page (en-tête et pied de page), ce qui répond aussi
au critère WCAG 2.2 « aide cohérente » (3.2.6).

**FAQ : pas de page dédiée.** Les questions vivent là où elles servent : 6 questions sur
l'accueil, les questions d'achat sur `/tarifs`, celles des familles sur `/parents`.

**Application mobile : pas de page dédiée.** Elle sert deux publics différents. Les
enseignants sont traités dans l'accueil et `/fonctionnalites`, les familles dans `/parents`.

### 3.2 Plus tard (superflu pour un premier lancement)

| Page | Quand |
|---|---|
| Témoignages, études de cas, logos d'écoles | Quand des écoles réelles auront donné leur accord écrit |
| Guides et articles (« Préparer ses bulletins du trimestre », « Passer du cahier d'appel au téléphone ») | Quand le rythme des posts sera tenu ; utile en SEO, coûteux à tenir |
| Version anglaise | Si une cible anglophone (Ghana, Nigeria) se confirme |
| Page « À propos de LOGILAB » | Un bloc dans l'accueil et le pied de page suffit au lancement |
| Badge Google Play | Le jour de la sortie publique : simple remplacement de la mention « En test » |

---

## 4. Page d'accueil, section par section

Maquette : `docs/refonte-site/maquette/index.html`.

| # | Section | Contenu et message | Appel à l'action |
|---|---|---|---|
| 0 | **En-tête collant** | Vraie icône de l'application + « School Manager ». Liens : Fonctionnalités, Tarifs, Parents, Sécurité. « Se connecter ». Sur mobile : bouton « Menu » qui ouvre la même liste. | « Essai gratuit » |
| 1 | **Haut de page** (fond vert nuit) | Surtitre « Pour les écoles privées du Mali ». Titre : « Notes, absences et frais de scolarité. Au même endroit, sans cahier ni tableur. » Texte : plateforme web + application, fondamental 1ère à 9ème année, en français et en FCFA. Microtexte : « 30 jours gratuits, sans engagement. À partir de 180 000 FCFA par an. » Visuel : vraies captures de la feuille d'appel et de l'accueil de l'application. | Primaire : « Essayer gratuitement 30 jours ». Secondaire : « Demander une démo sur WhatsApp ». |
| 2 | **Aiguillage parents** | Bandeau fin juste sous le haut de page : « Vous êtes parent d'élève ? Voir ce que l'application vous montre. » | Lien vers `/parents` |
| 3 | **Faits vérifiables** | Entièrement en français · Montants en FCFA · Fondamental, 1ère à 9ème année · Pensée pour les connexions lentes · Édité à Bamako par LOGILAB. Remplace les chiffres du site actuel (« 0 donnée partagée »). | — |
| 4 | **La réalité du terrain** | « Trois semaines pour sortir les bulletins. Un cahier d'appel égaré. Des paiements notés à la main. » Trois cartes situation → ce que fait School Manager. Le problème, ce sont les outils, pas les gens. | — |
| 5 | **Une plateforme, trois publics** | Direction et secrétariat (web) · Enseignants (application) · Parents (application). Une capture par public. Ce que chacun fait réellement. La messagerie est notée « sur le web ». | Lien « Toutes les fonctionnalités » |
| 6 | **Les modules** | 6 cartes : scolarité et bulletins ; absences et emploi du temps ; frais de scolarité ; communication (annonces, messagerie web, notifications d'absence) ; cantine, transport, bibliothèque ; statistiques de direction. Mention « 20 modules, 8 rôles ». | — |
| 7 | **Côté parents** | « La vie scolaire de votre enfant, sur votre téléphone. » Absences, notes et bulletins, scolarité et reçus, emploi du temps, plusieurs enfants, notifications réglables. Encadré : « Votre école doit être inscrite. Elle vous remet vos identifiants. » Badge App Store officiel + mention « Android : en test sur Google Play ». | App Store, « Montrer à mon directeur » |
| 8 | **Démarrer en trois étapes** | Créer l'école · Importer classes et élèves par fichier CSV · Inviter les enseignants et remettre leurs identifiants aux parents. Lève l'objection « je n'ai pas le temps ». | « Ouvrir mon espace d'essai » |
| 9 | **Vos données restent les vôtres** | 4 points en langage simple (pas de « Row-Level Security » sur l'accueil, réservé à `/securite`). | Lien `/securite` |
| 10 | **Tarifs** | 3 cartes dans l'ordre croissant. Pas de mention « le plus choisi » (ce serait inventé). Phrase d'aide : « Choisissez selon votre nombre d'élèves. » | « Démarrer l'essai », lien `/tarifs` |
| 11 | **Questions fréquentes** | 6 questions en accordéon natif (`<details>`, sans JS) : les parents paient-ils ? Et Android ? Après les 30 jours ? Faut-il une bonne connexion ? Si nous arrêtons ? Qui édite School Manager ? | — |
| 12 | **Appel final** (fond vert nuit) | « Ouvrez votre espace avant le prochain trimestre. » | Essai + WhatsApp |
| 13 | **Pied de page** | LOGILAB, Bamako, Mali. WhatsApp +223 79 82 79 79. Liens des pages, App Store, mentions légales, confidentialité. | — |

---

## 5. Direction artistique

### 5.1 Principe

**Le site ressemble au produit.** Fond clair « papier », bandes vert nuit comme la barre de
l'application, or réservé aux accents. Les vraies captures sont le visuel principal ; les
photos humanisent sans prétendre être des clients.

### 5.2 Palette (contrastes calculés selon WCAG 2.x)

| Rôle | Couleur | Usage | Contraste |
|---|---|---|---|
| Papier (fond) | `#f7f4ec` | Fond de page | — |
| Encre | `#12241f` | Texte principal sur papier | env. 15:1 |
| Encre secondaire | `#4a5a55` | Texte courant secondaire | 6,6:1 sur papier |
| Vert profond (primaire) | `#0f5c4c` | Boutons primaires sur fond clair, liens | blanc dessus 7,9:1 ; sur papier 7,2:1 |
| Vert nuit | `#0b231e` | Haut de page, appel final, pied de page (= navigation de l'app, DESIGN.md) | texte `#f1f5f3` env. 15:1 |
| Or | `#c9a227` | Bouton primaire sur fond sombre, filets, puces, gland de l'icône | sur vert nuit 6,8:1 ; texte `#1a1403` dessus 7,6:1 ; **jamais en texte sur papier (2,2:1)** |
| Or foncé | `#7a5f0c` | Surtitres sur fond clair | 5,5:1 sur papier |
| Vert pâle | `#e4eee9` | Fonds de cartes, pastilles | — |

Règle : un seul bouton primaire par zone. Sur fond clair il est vert, sur fond sombre il est or.

### 5.3 Typographie

**Outfit** (titres, 600) + **Inter** (texte, 400 et 600) : les deux polices de l'application
(DESIGN.md). Le site actuel utilise Manrope, qui n'existe nulle part ailleurs. En production,
polices **auto-hébergées** en `woff2`, sous-ensemble latin (accents français inclus),
`font-display: swap`, repli `system-ui`. Texte courant à 17 px, interligne 1,6, lignes de
65 caractères au plus. La maquette utilise Google Fonts par commodité.

### 5.4 Iconographie

Pictogrammes au trait (1,75 px, coins arrondis), dans l'esprit des icônes de l'application
(captures). Un petit sprite SVG inline (une quinzaine d'icônes, quelques Ko) remplace le
script Phosphor. Le logo devient la vraie icône (toque blanche, gland or), déclinée en
favicon 32 px, icône Apple 180 px et SVG.

### 5.5 Photos

- **Couleurs naturelles.** On abandonne le traitement monochrome vert, qui éteint les visages
  et coûte cher au rendu.
- **On garde** les 3 photos CC0 d'Afrique de l'Ouest : `hero-classe.jpg`, `scolarite.jpg`,
  `bulletins.jpg` (Ghana, contexte proche). **On retire** `ecriture.jpg`, `mobile-classe.jpg`
  et `campus.jpg`.
- **À terme** : une séance photo dans une école de Bamako (question n° 4), avec autorisation
  écrite de l'école et des parents. Adultes de face (directeur, enseignant, secrétaire),
  enfants de dos ou non identifiables.
- **Crédits** dans les mentions légales (CC0 n'impose rien, mais c'est plus transparent).
- **Captures à produire** avec des données de démonstration : tableau de bord web de la
  direction, bulletin PDF, suivi des impayés, écrans parents (notification d'absence, notes
  par matière, scolarité). Aujourd'hui, aucune capture ne montre l'écran d'un parent ni le web.
  La maquette affiche un emplacement réservé à leur place, sans rien inventer.

### 5.6 Mouvement

Sobre et jamais bloquant : aucune animation qui masque le contenu au chargement (on supprime
le `opacity: 0` des révélations au défilement, qui retarde la lecture en 3G). Survols par
changement de couleur, pas de déplacement. Ouverture des questions en 150 ms. Tout est coupé
avec `prefers-reduced-motion`. La vidéo `school-manager.mp4` n'est jamais lue
automatiquement : elle se charge au clic (`preload="none"`, image d'aperçu), après
compression.

### 5.7 Composants

Bouton primaire, bouton secondaire (contour), lien WhatsApp (icône + texte explicite),
badge App Store officiel, pastille « Android : en test » (non cliquable), carte public, carte
module, carte tarif, étapes numérotées, accordéon `<details>`, cadre de capture (coins
arrondis, ombre légère), bandeau d'aiguillage, menu mobile, pied de page.
Cibles tactiles de 44 px minimum (WCAG 2.2 exige 24 px).

---

## 6. Contraintes techniques

- **Site statique**, HTML + CSS + quelques lignes de JS (menu mobile, partage), sans
  framework ni étape de compilation. Servi tel quel par l'image `web` : `deploy/Dockerfile.web`
  copie `landing/` dans `/usr/share/nginx/html/site/`, rien à changer de ce côté.
- **Budget de poids** (transfert compressé, premier chargement) :
  - premier écran (HTML + CSS + polices + image principale) : < 150 Ko ;
  - page d'accueil complète : < 500 Ko ;
  - JS : < 10 Ko ; aucune ressource tierce bloquante.
- **Images** : `<picture>` AVIF/WebP + JPEG de repli, `srcset` 480 / 800 / 1200 px, `loading="lazy"`
  sous la ligne de flottaison, `fetchpriority="high"` sur l'image principale seulement.
  Captures recadrées et converties en WebP à 540 px de large.
- **Cache** : noms de fichiers versionnés (`styles.3.css`) pour passer les ressources en cache
  long (`immutable`), HTML en `no-cache`. Ajouter `image/svg+xml` à `gzip_types` si absent.
- **CSP stricte** pour le domaine principal (`default-src 'self'`), possible dès que polices
  et icônes sont auto-hébergées.
- **SEO** :
  - par page : `title` (« Logiciel de gestion scolaire pour les écoles du Mali | School Manager »),
    `meta description`, `canonical`, Open Graph (`og:title`, `og:description`, `og:url`,
    `og:image` 1200 × 630 à créer depuis le bandeau Play), `twitter:card` ;
  - `robots.txt`, `sitemap.xml`, favicon, icône Apple, `theme-color` ;
  - JSON-LD `Organization` (LOGILAB, Bamako, contact WhatsApp) et `SoftwareApplication`
    (name, `applicationCategory: EducationalApplication`, `operatingSystem: "Web, iOS"`,
    `offers` en XOF à partir de 180 000). **Google exige une note ou un avis pour afficher le
    résultat enrichi** : nous n'en avons pas et n'en inventerons pas. Le balisage reste utile
    pour la compréhension de la page, sans étoiles dans les résultats ;
  - un balisage FAQ n'apportera pas de résultat enrichi (réservé depuis 2023 aux sites
    gouvernementaux et de santé) : facultatif ;
  - Google Search Console et Bing Webmaster Tools.
- **Accessibilité WCAG 2.2 AA** : contrastes ci-dessus, un seul `h1`, repères (`header`,
  `nav`, `main`, `footer`), lien d'évitement, focus visible et jamais masqué par l'en-tête
  (`scroll-padding-top`), cibles ≥ 24 px (44 px visés), textes de liens explicites, mention
  « nouvel onglet », textes alternatifs qui décrivent les captures, information jamais portée
  par la seule couleur, reflux à 320 px, navigation complète au clavier.
- **Français** partout, montants « 180 000 FCFA par an » avec espaces insécables.

---

## 7. Plan de réalisation

Estimation relative : S (moins d'une demi-journée), M (environ une journée), L (plusieurs jours).

| # | Étape | Agent | Taille |
|---|---|---|---|
| 0 | Correctif immédiat du site actuel : mention Android, « messagerie sur le web », lien « Demander une démo » vers WhatsApp, phrase « à compléter » retirée | agent-frontend | S |
| 1 | Validation de la proposition et réponses aux questions | Oumar | — |
| 2 | Spécification finale : gabarits des 6 pages, textes définitifs, `docs/design/ecrans/site-*.md` | agent-designer | M |
| 3 | Production des captures manquantes (web direction, bulletin, impayés, écrans parents) avec données de démonstration | agent-mobile / agent-frontend, relu par agent-designer | M |
| 4 | Préparation des ressources : favicons depuis l'icône, captures en WebP, photos AVIF/WebP + `srcset`, image Open Graph, sprite SVG, polices auto-hébergées | agent-frontend | M |
| 5 | Socle : variables CSS, composants, en-tête et pied de page communs, menu mobile | agent-frontend | M |
| 6 | Page d'accueil | agent-frontend | M |
| 7 | Pages Parents et Tarifs | agent-frontend | M |
| 8 | Pages Fonctionnalités, Sécurité, Mentions légales, 404 (`confidentialite.html` repris au nouveau style, même URL) | agent-frontend | M |
| 9 | SEO : balises, JSON-LD, `robots.txt`, `sitemap.xml` | agent-frontend | S |
| 10 | Nginx : cache des ressources versionnées, `gzip_types`, CSP du site, page 404 ; mesure d'audience si validée | agent-devops | S |
| 11 | Relecture visuelle et liste des écarts | agent-designer | S |
| 12 | Tests : Lighthouse mobile en réseau lent, axe, clavier, 320 / 375 / 768 / 1280 px, liens (`/register`, `/login`, WhatsApp, App Store), validation HTML, test des résultats enrichis | agent-tests | M |
| 13 | Relecture : faits conformes à PLAN.md §1.5 (aucun chiffre, témoignage ou date Android inventé), qualité du code | agent-relecteur | S |
| 14 | Le jour de la sortie Android : badge Google Play à la place de la pastille « En test » | agent-frontend | S |

Total hors étape 3 : environ 6 à 8 jours-agent. Ordre conforme au processus : designer →
frontend → designer (relecture) → devops → tests → relecteur.

---

## 8. Questions à trancher par Oumar

1. **Mentions légales et contact** : raison sociale exacte de LOGILAB, RCCM / NIF, adresse,
   directeur de la publication, hébergeur, et une **adresse e-mail publique** (le site promet
   un support par e-mail sans en donner).
2. **Fond clair ou sombre ?** Je recommande un site clair avec des bandes vert nuit, pour
   ressembler à l'application et rester lisible en plein soleil. Garder le tout-sombre
   actuel reste possible.
3. **Mesure d'audience** : accepter un outil sans cookies (Cloudflare Web Analytics, puisque
   le site passe déjà par Cloudflare, ou Plausible / Umami auto-hébergé) ? Sans mesure, les
   objectifs du §2 ne peuvent pas être suivis.
4. **Photos** : une séance photo est-elle possible dans une école de Bamako, avec
   autorisations écrites ? Sinon, on garde les 3 photos CC0 d'Afrique de l'Ouest et on mise
   sur les captures.
5. **Affirmations commerciales à confirmer** : « domaine personnalisé » (Premium),
   « engagement de service », « bulletin conforme à votre modèle », « support par chat », et
   **comment l'école paie l'abonnement** (virement, espèces, Orange Money / Moov Money ?).
   Tant que ce n'est pas confirmé, ces mentions restent hors du site.

---

## Sources (veille du 23/10/2026)

- W3C, « What's New in WCAG 2.2 » : critères 2.4.11 (focus non masqué), 2.5.8 (taille des
  cibles, 24 px), 3.2.6 (aide cohérente). https://www.w3.org/WAI/standards-guidelines/wcag/new-in-22/
- Google Search Central, données structurées `SoftwareApplication` : `name`, `offers.price`
  et une note ou un avis requis pour le résultat enrichi.
  https://developers.google.com/search/docs/appearance/structured-data/software-app
- Google Search Central, « Changes to HowTo and FAQ rich results » (août 2023).
  https://developers.google.com/search/blog/2023/08/howto-faq-changes
- Genesys Growth, « Designing B2B SaaS landing pages » : titre qui nomme le problème de
  l'acheteur, vraies captures du produit dans le haut de page, un appel à l'action principal.
  https://genesysgrowth.com/blog/designing-b2b-saas-landing-pages
- Photos : `landing/assets/CREDITS.md` (Wikimedia Commons, CC0).
