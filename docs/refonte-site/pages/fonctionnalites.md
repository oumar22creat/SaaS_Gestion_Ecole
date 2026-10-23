# Page Fonctionnalités (`/fonctionnalites`, `landing/fonctionnalites.html`)

Socle : [../SPEC.md](../SPEC.md).

**Objectif** : la page de référence pour un directeur ou un promoteur qui veut savoir ce que fait
réellement School Manager, rôle par rôle, et **ce qui est sur le web ou sur l'application**.
Page principale pour le référencement (« logiciel de gestion scolaire », « bulletin scolaire
en ligne », « feuille d'appel »).

## Balises

| Balise | Valeur |
|---|---|
| `title` | Fonctionnalités : bulletins, absences, frais de scolarité \| School Manager |
| `meta description` | Les modules de School Manager rôle par rôle : bulletins PDF, feuille d'appel, emploi du temps, frais de scolarité, cantine, statistiques. Sur le web et sur mobile. |
| `canonical` / `og:url` | `https://schoolmanager.tech/fonctionnalites` |
| `og:title` | Toutes les fonctionnalités de School Manager |
| `og:description` | 20 modules et 8 rôles : ce que fait chacun, sur la plateforme web et sur l'application. |
| WhatsApp de la page | W3 |
| `aria-current="page"` | lien « Fonctionnalités » |

## Gabarit

0. En-tête · 1. Introduction · 2. Web ou application · 3. Direction et secrétariat ·
4. Comptabilité et frais de scolarité · 5. Cantine, transport, bibliothèque · 6. Vie scolaire ·
7. Enseignants · 8. Parents et élèves · 9. Les 8 rôles · 10. Appel final · 11. Pied de page.
Fonds : 1 papier · 2 papier-2 · 3 papier · 4 papier-2 · 5 papier · 6 papier-2 · 7 papier ·
8 papier-2 · 9 papier · 10 nuit.

Les sections 3 à 8 suivent le même gabarit `.feature-block` : surtitre (pastille plateforme),
`h2`, chapô court, grille de modules (`h3` + paragraphe, sans carte, séparés par un filet
`--line` en haut), média facultatif. Grille des modules : 1 colonne < 600 px, 2 colonnes
≥ 600 px. Avec média (≥ 900 px) : texte `1.2fr` à gauche, média `.8fr` à droite, le texte
restant avant le média dans le DOM.

---

## 1. Introduction (`.page-intro`)

| Élément | Texte |
|---|---|
| Surtitre | Fonctionnalités |
| `h1` | Tout ce qu'une école gère, rôle par rôle. |
| Chapô | School Manager compte 20 modules et 8 rôles. La direction et le secrétariat travaillent sur la plateforme web ; les enseignants, les parents et les élèves sur l'application mobile. Chacun ne voit que ce qui le concerne. |
| `.btn-green` | Essayer gratuitement 30 jours → `/register` |
| `.btn-outline` | icône WhatsApp + « Demander une démo sur WhatsApp » → W3 |

Visuel (≥ 900 px uniquement, masqué dessous pour ne pas repousser le sommaire) :
`/assets/captures/accueil-*` dans un `.shot` 240 px, ratio 9/15. `alt` : « Capture de
l'application : accueil de l'espace établissement, avec les accès Feuille d'appel, Notes,
Emploi du temps et Notifications. » `loading` normal (au-dessus de la ligne de flottaison).

Sommaire `.toc` (`<nav aria-label="Sur cette page">`), sous les boutons :
Direction et secrétariat (`#direction`) · Comptabilité (`#comptabilite`) · Cantine, transport,
bibliothèque (`#services`) · Vie scolaire (`#vie-scolaire`) · Enseignants (`#enseignants`) ·
Parents et élèves (`#familles`) · Les 8 rôles (`#roles`).

| Largeur | Disposition |
|---|---|
| 320 / 375 | Une colonne ; boutons pleine largeur ; pastilles du sommaire sur 3 à 4 lignes. |
| 768 | Une colonne ; boutons côte à côte ; pastilles sur 2 lignes. |
| 1280 | Texte + sommaire à gauche, capture à droite. |

## 2. Web ou application (`aria-labelledby="titre-supports"`)

| Élément | Texte |
|---|---|
| `h2` | Sur le web ou sur le téléphone ? |
| Chapô | Le travail de bureau se fait sur ordinateur, le travail de terrain sur téléphone. |

Deux cartes `.card` côte à côte (≥ 600 px), l'une sous l'autre dessous :

| | Carte 1 | Carte 2 |
|---|---|---|
| Icône | `i-chart` | `i-calendar` |
| `h3` | Plateforme web | Application mobile |
| Sous-titre (`--ink-2`, 15 px) | Sur ordinateur, à l'adresse de votre école. | Sur iPhone. Sur Android : bientôt, l'application est en test. |
| Pour qui | Administrateur, direction, secrétariat, comptabilité, vie scolaire et enseignants. | Enseignants, parents et élèves. |
| Coches | Tous les modules · Bulletins PDF, certificats, cartes scolaires · Annonces et messagerie interne | Feuille d'appel et notes · Emploi du temps · Absences, notes et scolarité des enfants, pour les parents |

Encadré `.notice` sous les cartes : « **La messagerie et les annonces sont sur la plateforme
web uniquement.** L'application mobile ne permet pas d'écrire à l'école. »

## 3. Direction et secrétariat (`id="direction"`)

| Élément | Texte |
|---|---|
| Pastille | Sur le web |
| `h2` | Direction et secrétariat |
| Chapô | Le dossier de chaque élève, de l'inscription au bulletin, saisi une seule fois. |

| `h3` | Texte |
|---|---|
| Élèves et familles | Fiches des élèves, des parents et des tuteurs. Import de toute l'école par fichier CSV. |
| Classes, matières, enseignants | Classes, matières, affectation de chaque enseignant à ses classes et à ses matières, années scolaires. |
| Emploi du temps | Par classe, enseignant, matière et salle. Les conflits de salle, d'enseignant ou de classe sont signalés avant l'enregistrement. |
| Bulletins | Générés à partir des notes : moyennes par matière et générale, coefficients, rang, appréciations par matière et générale, récapitulatif des absences. Export PDF à l'en-tête de l'école. |
| Documents officiels | Certificats de scolarité et cartes scolaires, pour un élève ou pour toute une classe en une fois. |
| Annonces et messagerie | Annonces de l'établissement, messages individuels ou de groupe entre membres du personnel, pièces jointes, confirmation de lecture. |
| Statistiques | Effectifs, taux de présence, moyennes par classe, évolution des résultats dans le temps, export CSV. Rapport trimestriel par classe. |
| Aux couleurs de l'école | Logo et couleurs de l'école dans la plateforme et l'application, en-tête des bulletins, modèles de notifications. |

Média : photo `/assets/photos/cahiers-*`, ratio 4/3, `loading="lazy"`.
`alt` : « Des élèves en chemise blanche écrivent dans leurs cahiers, penchés sur un long banc de classe. »
Légende : « Photo d'illustration : Nigeria (Wikimedia Commons, CC0). »
Futur (SPEC §9) : tableau de bord web + bulletin PDF.

## 4. Comptabilité et frais de scolarité (`id="comptabilite"`)

| Élément | Texte |
|---|---|
| Pastilles | Sur le web · `.tag-plan` Plans Standard et Premium |
| `h2` | Comptabilité et frais de scolarité |
| Chapô | Savoir qui a payé, qui doit encore, et le prouver par un reçu. |

| `h3` | Texte |
|---|---|
| Grille des frais | Montants par classe, une échéance datée par ligne. |
| Factures et échéanciers | Factures générées pour tous les élèves en une fois, échéancier par famille. |
| Règlements et reçus | Saisie des paiements reçus par l'école, reçu PDF pour chaque règlement. Les parents le retrouvent dans l'application. |
| Suivi des impayés | Total dû, payé et restant, par classe et par période. |

Pas de média (futur : écran des impayés).

## 5. Cantine, transport, bibliothèque (`id="services"`)

| Élément | Texte |
|---|---|
| Pastilles | Sur le web · `.tag-plan` Standard (en option) et Premium |
| `h2` | Cantine, transport, bibliothèque |
| Chapô | Les services de l'école, suivis et facturés au même endroit que la scolarité. |

| Icône | `h3` | Texte |
|---|---|---|
| `i-receipt` | Cantine | Menus, régimes particuliers, réservation des repas par le personnel, facturation selon les repas réservés, suivi des impayés. |
| `i-bus` | Transport | Lignes et arrêts de bus, affectation de chaque élève à un circuit, facturation du service. |
| `i-book` | Bibliothèque | Catalogue avec codes-barres, emprunts et retours, relances automatiques en cas de retard, réservations avec liste d'attente. |

Grille 1 colonne < 600, 3 colonnes ≥ 900 (2 entre les deux, le troisième pleine largeur).

## 6. Vie scolaire (`id="vie-scolaire"`)

| Élément | Texte |
|---|---|
| Pastille | Sur le web |
| `h2` | Vie scolaire |
| Chapô | Absences, retards et discipline, avec l'historique de chaque élève. |

| `h3` | Texte |
|---|---|
| Absences et retards | Motifs, justificatifs, historique par élève. Les familles sont prévenues dès que l'absence est saisie. |
| Discipline | Incidents, sanctions, convocations des familles, observations positives ou négatives, historique par élève. |
| Suivi | Incidents par classe et par période, répartition par type et par gravité. |

## 7. Enseignants (`id="enseignants"`)

| Élément | Texte |
|---|---|
| Pastille | Web et application |
| `h2` | Enseignants |
| Chapô | L'essentiel du métier depuis le téléphone, en classe, entre deux cours. |

| `h3` | Texte |
|---|---|
| Feuille d'appel | Toute la classe sur un écran : présent, absent, retard ou départ anticipé. Les familles concernées sont prévenues dès que l'absence est saisie. |
| Notes | Évaluations et notes saisies depuis le téléphone. Les moyennes se calculent seules. |
| Emploi du temps | Les cours de la semaine, avec la salle et la classe. |
| Cahier de textes et documents (sur le web) | Contenu des séances, travail à faire, documents par matière ou par classe. |

Média : rail `.shots-rail` (`aria-label="Écrans de l'application enseignant"`), **sous** la
grille de modules (pleine largeur) :

| Image | `alt` | Légende |
|---|---|---|
| `/assets/captures/appel-*` | Capture de l'application : feuille d'appel de la classe de 6ème année A, avec pour chaque élève les boutons Présent, Absent, Retard et Départ anticipé. | La feuille d'appel. |
| `/assets/captures/notes-*` | Capture de l'application : liste des évaluations de l'enseignant, avec la date et le barème sur 20. | Les évaluations. |
| `/assets/captures/emploi-du-temps-*` | Capture de l'application : emploi du temps du jeudi, avec l'heure, la matière, la salle et la classe de chaque cours. | L'emploi du temps. |

Badges stores (SPEC §6.15) sous le rail.

## 8. Parents et élèves (`id="familles"`)

| Élément | Texte |
|---|---|
| Pastille | Application |
| `h2` | Parents et élèves |
| Chapô | Les familles suivent la scolarité sans se déplacer. L'application est gratuite pour elles. |

| `h3` | Texte |
|---|---|
| Absences et retards | Le parent est prévenu dès que l'absence est saisie, avec la date et le motif. |
| Notes et moyenne | Matière par matière, avec la moyenne et le rang dans la classe. |
| Scolarité | L'échéancier, ce qui a été réglé, ce qui reste dû, le reçu de chaque paiement. |
| Emploi du temps | La semaine de l'enfant, cours par cours. |
| Plusieurs enfants | Un seul compte pour tous les enfants inscrits dans l'école. |
| Notifications réglables | Chacun choisit ce qu'il reçoit. |
| Pour les élèves | Leur propre compte, avec leurs notes, leurs absences et leur emploi du temps. |

Encadré `.notice` : « **Les parents ne s'inscrivent pas seuls.** L'école crée leur compte et leur
remet leurs identifiants. » + lien « Tout savoir sur l'application pour les parents » → `/parents`.

Média : `/assets/captures/connexion-*`, `.shot` 240 px. `alt` : « Capture de l'application :
écran de connexion avec les champs Sous-domaine, E-mail et Mot de passe. » Légende : « Une
seule connexion pour les enseignants, les parents et les élèves. »
Futur (SPEC §9) : écrans parents.

## 9. Les 8 rôles (`id="roles"`)

| Élément | Texte |
|---|---|
| `h2` | 8 rôles, chacun son périmètre. |
| Chapô | Les droits sont vérifiés sur le serveur, pas dans le navigateur. Un compte ne voit que ce que son rôle autorise. |

Tableau `<table class="roles-table">` dans `.table-scroll`, `<caption class="sr-only">Les 8 rôles et ce que chacun voit</caption>`,
colonnes « Rôle » (`th scope="row"`) · « Où » · « Ce qu'il voit » :

| Rôle | Où | Ce qu'il voit |
|---|---|---|
| Administrateur | Web | Les paramètres de l'école, les comptes du personnel et l'abonnement. |
| Direction | Web | L'ensemble de l'école : scolarité, finances, statistiques. |
| Secrétariat | Web | Le dossier scolaire et financier des élèves, les documents officiels. |
| Comptabilité | Web | Les frais de scolarité, les règlements, les reçus et les impayés. |
| Vie scolaire | Web | Les absences, les retards et la discipline. |
| Enseignant | Web et application | Ses classes : appel, notes, cahier de textes. |
| Parent | Application | Le dossier de ses enfants. |
| Élève | Application | Son propre dossier. |

À relire par agent-relecteur contre les `@PreAuthorize` (SPEC §11, point 11).
Responsive : à 320 / 375, le tableau tient sans défilement (colonne « Où » en 14 px) ; sinon
le conteneur défile.

## 10. Appel final (`.closer`)

| Élément | Texte |
|---|---|
| `h2` | Voyez-le avec vos propres classes. |
| Paragraphe | 30 jours d'essai gratuit, sans engagement. Ou une démonstration sur WhatsApp, à votre rythme. |
| `.btn-gold` | Essayer gratuitement 30 jours → `/register` |
| `.btn-outline-light` | icône WhatsApp + « Demander une démo » → W3 |

## Responsive récapitulatif

| Section | 320 / 375 | 768 | 1280 |
|---|---|---|---|
| 2 | Cartes empilées | Deux cartes côte à côte | Deux cartes côte à côte |
| 3 à 8 | Une colonne : chapô, modules, média | Modules en 2 colonnes, média dessous | Modules en 2 colonnes à gauche, média à droite (3, 8) ; rail pleine largeur (7) |
| 7 rail | Défilement horizontal | 3 colonnes | 3 colonnes |
| 9 | Tableau sans défilement | Tableau | Tableau, `max-width: 900px` |
