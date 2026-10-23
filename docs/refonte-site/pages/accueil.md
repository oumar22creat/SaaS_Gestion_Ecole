# Page Accueil (`/`, `landing/index.html`)

Socle : [../SPEC.md](../SPEC.md). Maquette de référence visuelle : `../maquette/index.html`
(les écarts sont listés en fin de fiche).

**Objectif** : en 5 secondes, un directeur comprend ce que c'est, pour qui, combien. Il démarre
l'essai ou écrit sur WhatsApp. Un parent est aiguillé vers `/parents` dès le deuxième bloc.

## Balises

| Balise | Valeur |
|---|---|
| `title` | Logiciel de gestion scolaire pour les écoles du Mali \| School Manager |
| `meta description` | Notes, bulletins, absences et frais de scolarité au même endroit. Plateforme web et application pour les écoles privées du Mali. Essai gratuit 30 jours. |
| `canonical` / `og:url` | `https://schoolmanager.tech/` |
| `og:title` | School Manager : la gestion scolaire des écoles privées du Mali |
| `og:description` | Notes, absences et frais de scolarité au même endroit, sans cahier ni tableur. 30 jours d'essai gratuit. |
| WhatsApp de la page | W1 (en-tête mobile, pied de page) |
| `aria-current` | aucun lien de navigation (l'accueil n'est pas dans le menu) |

## Gabarit (ordre des sections)

0. En-tête commun · 1. Haut de page · 2. Aiguillage parents · 3. Faits · 4. Le constat ·
5. Trois publics · 6. Modules · 7. Côté parents · 8. Trois étapes · 9. Confiance ·
10. Tarifs · 11. Questions fréquentes · 12. Appel final · 13. Pied de page commun.
Fonds alternés : 1 nuit · 2 vert pâle · 3 papier · 4 papier · 5 papier-2 · 6 papier ·
7 papier-2 · 8 papier · 9 papier-2 · 10 papier · 11 papier-2 · 12 nuit.

---

## 1. Haut de page (`.hero`, fond nuit)

| Élément | Texte / valeur |
|---|---|
| Surtitre (`.eyebrow-dark`, or) | Pour les écoles privées du Mali |
| `h1` (`id="titre-hero"`) | Notes, absences et frais de scolarité. `<span class="gold">`Au même endroit, sans cahier ni tableur.`</span>` |
| Chapô | Une plateforme web pour la direction et le secrétariat, une application mobile pour les enseignants et les parents. Pensée pour l'enseignement fondamental, de la 1ère à la 9ème année, entièrement en français et en FCFA. |
| Bouton primaire `.btn-gold` | Essayer gratuitement 30 jours → `/register` |
| Bouton secondaire `.btn-outline-light` | icône WhatsApp + « Demander une démo sur WhatsApp » → W1 |
| Microtexte (`.micro`, 15 px, `--on-night-2`) | Sans engagement, aucun paiement demandé pour l'essai. Tarifs publics : à partir de 180 000 FCFA par an. |

Visuel (`.hero-visual`) :

- Capture principale `.shot.shot-main` : `/assets/captures/appel-540.webp` (srcset 360/540),
  **`fetchpriority="high"`, pas de `loading="lazy"`**. `alt` : « Capture de l'application :
  feuille d'appel de la classe de 6ème année A, avec pour chaque élève les boutons Présent,
  Absent, Retard et Départ anticipé. » Ratio 9/15, coins bas droits (collée au bas de la bande).
- Capture d'arrière-plan `.shot.shot-back` (≥ 900 px seulement) : `/assets/captures/accueil-540.webp`,
  `alt=""`, `figure aria-hidden="true"`, `loading="lazy"`, opacité .55, décalée de 48 px vers le haut.

Responsive :

| Largeur | Disposition |
|---|---|
| 320 | Une colonne. `h1` 32 px sur 5 à 6 lignes. Boutons pleine largeur, l'un sous l'autre. Capture principale sous le microtexte, largeur 68 vw (≈ 218 px), centrée, coupée en bas. Pas de capture d'arrière-plan. |
| 375 | Idem, capture ≈ 255 px. |
| 768 | Une colonne, boutons côte à côte (largeur auto). Capture 270 px centrée. |
| 1280 | Deux colonnes `1.1fr .9fr`, alignées en bas. `h1` 54 px, `max-width: 18ch`. Capture principale 270 px, capture d'arrière-plan 220 px à droite. |

## 2. Aiguillage parents (`.switch`)

« Vous êtes parent d'élève ? » + lien « Voir ce que l'application vous montre » + flèche →
`/parents`. Composant SPEC §6.9. À 320 px : deux lignes.

## 3. Faits (`.facts`, `aria-label="En bref"`)

Liste, dans cet ordre : Entièrement en français · Montants en FCFA · Fondamental, 1ère à 9ème
année · Pensée pour les connexions lentes · Édité à Bamako par LOGILAB.
320 / 375 : une colonne. 768 : deux à trois par ligne. 1280 : une seule ligne.

## 4. Le constat (`aria-labelledby="titre-constat"`)

| Élément | Texte |
|---|---|
| Surtitre | Le constat |
| `h2` | Les écoles ne manquent pas de rigueur. Elles manquent d'outils. |
| Paragraphe | Les notes vivent dans un tableur, les absences sur papier, les paiements dans un cahier. Personne ne voit l'ensemble au même moment. |

Photo (`.photo`) : `/assets/photos/classe-*`, ratio 4/3, `loading="lazy"`.
`alt` : « Trois élèves en uniforme partagent un manuel devant le tableau de leur classe. »
Légende : « Photo d'illustration : école d'Amanya, Ghana (Wikimedia Commons, CC0). »

Trois cartes « situation » (`.cards-3`, `<article class="card">`) :

Intitulé de problème en `h3 class="card-problem"` (pas de guillemets, aucune attribution : ce ne
sont pas des citations), sous le `h2` de la section.

| Intitulé `h3.card-problem` | Texte | Pastille |
|---|---|---|
| Les bulletins prennent des semaines | Les notes saisies pendant le trimestre alimentent directement le bulletin : moyennes, coefficients, rang et absences de la période. Il sort en PDF, à l'en-tête de votre école. | — |
| Le cahier d'appel se perd | L'enseignant fait l'appel sur son téléphone, toute la classe sur un seul écran. Les familles concernées sont prévenues dès que l'absence est saisie. | — |
| Qui a payé ce trimestre ? | Grille des frais par classe, échéanciers, facture par élève. Les impayés sont réunis sur un seul écran, et chaque famille voit ce qu'elle a réglé. | `.tag-plan` « Plans Standard et Premium » |

Responsive : 320 / 375 / 768 : titre, paragraphe, photo (16/10), puis cartes en une colonne
(768 : une colonne aussi, cartes plus aérées). 1280 : en haut, titre + paragraphe à gauche
(`1.1fr`) et photo à droite (`.9fr`) ; dessous, les 3 cartes en 3 colonnes.

## 5. Une plateforme, trois publics (fond papier-2)

| Élément | Texte |
|---|---|
| Surtitre | Une plateforme, trois publics |
| `h2` | Chacun voit ce qui le concerne, et seulement cela. |

Trois cartes `.role` (icône, `h3`, pastille, coches) :

| Icône | `h3` | Pastille | Coches |
|---|---|---|---|
| `i-chart` | Direction et secrétariat | Sur le web | Élèves, classes et matières, import par fichier CSV · Bulletins PDF à l'en-tête de l'école · Frais de scolarité et suivi des impayés · Annonces et messagerie interne |
| `i-calendar` | Enseignants | Application | Feuille d'appel tactile · Saisie des notes depuis le téléphone · Emploi du temps de la semaine |
| `i-users` | Parents et élèves | Application | Absences et retards, dès qu'ils sont saisis · Notes, moyenne et rang · Échéancier et reçus · Emploi du temps |

Rail de captures (`.shots-rail`, `aria-label="Écrans de l'application"`) sous les cartes,
précédé du titre `h3` « Dans l'application » :

| Image | `alt` | Légende |
|---|---|---|
| `/assets/captures/notes-*` | Capture de l'application : liste des évaluations de l'enseignant, avec la date et le barème sur 20. | Les évaluations de l'enseignant, avec date et barème. |
| `/assets/captures/emploi-du-temps-*` | Capture de l'application : emploi du temps du jeudi, avec l'heure, la matière, la salle et la classe de chaque cours. | L'emploi du temps du jour, cours par cours. |
| `/assets/captures/connexion-*` | Capture de l'application : écran de connexion avec les champs Sous-domaine, E-mail et Mot de passe. | Une seule connexion pour les enseignants, les parents et les élèves. |

Lien `.more` : « Voir toutes les fonctionnalités » → `/fonctionnalites`.

Responsive : 320 / 375 : cartes en une colonne, rail défilant (72 % de largeur par capture,
indication « Faites glisser pour voir les autres écrans. »). 768 : cartes en une colonne, rail
en 3 colonnes (captures ≈ 200 px). 1280 : cartes en 3 colonnes, rail en 3 colonnes (260 px).
Futur : voir SPEC §9 (une capture par carte, rail supprimé).

## 6. Les modules (`id="modules"`)

| Élément | Texte |
|---|---|
| Surtitre | Fonctionnalités |
| `h2` | 20 modules, 8 rôles. L'essentiel, ici. |

Six cartes `.module` :

| Icône | `h3` | Texte | Pastille |
|---|---|---|---|
| `i-book` | Scolarité, notes et bulletins | Élèves, classes et matières, import CSV. Moyennes calculées automatiquement, appréciations, coefficients, bulletins PDF. | — |
| `i-calendar` | Absences et emploi du temps | Feuille d'appel, motifs et justificatifs. Les conflits de salle ou d'enseignant sont signalés. | — |
| `i-coins` | Frais de scolarité | Grille par classe, échéanciers, factures, reçus, suivi des impayés. | Plans Standard et Premium |
| `i-chat` | Communication | Notification au parent dès qu'une absence est saisie. Annonces et messagerie interne sur la plateforme web. | — |
| `i-bus` | Cantine, transport, bibliothèque | Menus et réservations, lignes et arrêts de bus, catalogue et emprunts. | Standard (en option) et Premium |
| `i-chart` | Statistiques de direction | Effectifs, taux de présence, moyennes par classe, évolution des résultats, export CSV. | — |

Lien `.more` : « Voir toutes les fonctionnalités » → `/fonctionnalites`.
Responsive : 320 / 375 : 1 colonne. 768 : 2 colonnes. 1280 : 3 colonnes.

## 7. Côté parents (`id="parents"`, fond papier-2)

| Élément | Texte |
|---|---|
| Surtitre | Pour les parents |
| `h2` | La vie scolaire de votre enfant, sur votre téléphone. |
| Coches `.ticks-lg` | Vous êtes prévenu d'une absence dès que l'école la saisit, avec la date et le motif. · Les notes matière par matière, la moyenne et le rang dans la classe. · Ce qui a été payé, ce qui reste dû, le reçu de chaque paiement. · Plusieurs enfants sous un seul compte. · Vous choisissez les notifications que vous recevez. |
| Encadré `.notice` | **L'application est gratuite pour les parents.** Elle fonctionne avec le compte que votre école vous remet. Si votre école n'est pas encore inscrite, parlez-en à votre directeur. |
| Badges | Rangée stores complète (SPEC §6.15) : App Store + Google Play bientôt + note. |
| Lien `.more` | Tout savoir sur l'application pour les parents → `/parents` |
| Lien de partage `.share` | icône WhatsApp + « Envoyer School Manager à mon directeur » → W9, texte masqué « (WhatsApp, nouvel onglet) » |

Photo : `/assets/photos/eleves-lecture-*`, ratio 4/5 (≥ 900 px) ou 16/11 (dessous).
`alt` : « Deux écolières en uniforme lisent ensemble un livre devant leur école. »
Légende : « Photo d'illustration : Ghana (Wikimedia Commons, CC0). »

Responsive : 320 / 375 / 768 : texte, encadré, badges, liens, puis photo. 1280 : texte à
gauche (`1.1fr`), photo à droite (`.9fr`), centrés verticalement.

## 8. Démarrer en trois étapes

| Élément | Texte |
|---|---|
| Surtitre | Mise en place |
| `h2` | Ouvrir votre espace en trois étapes. |

| N° | `h3` | Texte |
|---|---|---|
| 1 | Créez votre école | Sur schoolmanager.tech, en quelques minutes. Votre école obtient son propre espace et sa propre adresse : votre-ecole.schoolmanager.tech. |
| 2 | Importez classes et élèves | Par fichier CSV, sans tout ressaisir à la main. |
| 3 | Ouvrez les accès | Créez les comptes de vos enseignants, puis remettez aux parents leurs identifiants. |

Boutons : `.btn-green` « Ouvrir mon espace d'essai » → `/register` ; `.btn-outline` icône
WhatsApp + « Besoin d'aide pour démarrer ? » → W2.
Responsive : < 760 px une colonne ; au-delà 3 colonnes.

## 9. Confiance (`id="securite"`, fond papier-2)

| Élément | Texte |
|---|---|
| Surtitre | Confiance |
| `h2` | Les données de votre école restent les vôtres. |

| Icône | `h3` | Texte |
|---|---|---|
| `i-shield` | Un espace par école | Aucune autre école ne peut voir vos données. L'isolation est imposée par la base de données elle-même. |
| `i-users` | Chacun son périmètre | Un enseignant fait l'appel et saisit les notes, la comptabilité gère les règlements, la direction voit l'ensemble. |
| `i-lock` | Connexions protégées | HTTPS obligatoire, mots de passe jamais stockés en clair, sauvegardes chiffrées, actions sensibles journalisées. |
| `i-export` | Vos données vous suivent | Si vous arrêtez, vous pouvez demander l'export de vos données avant leur suppression. |

Lien `.more` : « Comment nous protégeons vos données » → `/securite`.
Responsive : 320 / 375 : 1 colonne. 768 : 2 colonnes. 1280 : 4 colonnes.

## 10. Tarifs (`id="tarifs"`)

| Élément | Texte |
|---|---|
| Surtitre | Tarifs |
| `h2` | Des prix affichés. Choisissez selon votre nombre d'élèves. |

Trois cartes `.plan` **allégées** (sans liste de coches, le détail est sur `/tarifs`) :

| `h3` | Effectif | Prix | Équivalence | Bouton `.btn-outline` |
|---|---|---|---|---|
| Essentiel | Jusqu'à 150 élèves | 180 000 FCFA par an | Soit 15 000 FCFA par mois. | Démarrer l'essai → `/register` |
| Standard | Jusqu'à 800 élèves | 750 000 FCFA par an | Soit 62 500 FCFA par mois. | Choisir Standard → `/register` |
| Premium | Effectif illimité | dès 1 800 000 FCFA par an | Tarif dégressif selon l'effectif. | Voir le plan Premium → `/tarifs#plan-premium` |

Note (`.note`) : « Abonnement annuel. 30 jours d'essai gratuit, sans engagement. » + lien
« Comparer les plans en détail » → `/tarifs#comparer`.
Responsive : < 900 px une colonne ; ≥ 900 px trois colonnes.

## 11. Questions fréquentes (fond papier-2, conteneur `--narrow`)

| Élément | Texte |
|---|---|
| Surtitre | Questions fréquentes |
| `h2` | Vos questions, nos réponses. |

| Question (`summary`) | Réponse |
|---|---|
| Les parents doivent-ils payer ? | Non. C'est l'école qui s'abonne. L'application est gratuite pour les parents, qui se connectent avec les identifiants remis par l'école. |
| L'application est-elle disponible sur Android ? | Pas encore pour tout le monde. Elle est en test sur Google Play et sera annoncée ici dès sa sortie publique. Elle est déjà disponible sur iPhone, dans l'App Store. |
| Que se passe-t-il après les 30 jours d'essai ? | Pour continuer, vous choisissez le plan adapté à votre nombre d'élèves. Sans abonnement, votre espace passe en lecture seule : vos données restent consultables, mais vous ne pouvez plus en ajouter. |
| Faut-il une bonne connexion internet ? | Non. L'application ne télécharge que ce qu'elle affiche : quand vous ouvrez les notes, elle charge les notes, rien de plus. |
| Que deviennent nos données si nous arrêtons ? | Vous pouvez demander l'export de vos données. Elles ne sont supprimées que 90 jours après la fin du service, ce qui vous laisse le temps de les récupérer. |
| Qui édite School Manager ? | School Manager est édité par LOGILAB, à Bamako (Mali). Écrivez-nous sur WhatsApp au +223 79 82 79 79 ou par e-mail à oumarcoulou44@gmail.com. (numéro et adresse en liens : W1 et `mailto:`) |

Lien sous la FAQ : « D'autres questions sur les tarifs » → `/tarifs#questions`.

## 12. Appel final (`.closer`)

| Élément | Texte |
|---|---|
| `h2` | Ouvrez votre espace avant le prochain trimestre. |
| Paragraphe | 30 jours d'essai gratuit, sans engagement. Aucun paiement demandé pour commencer. |
| `.btn-gold` | Essayer gratuitement 30 jours → `/register` |
| `.btn-outline-light` | icône WhatsApp + « Écrire sur WhatsApp » → W1 |

## 13. Pied de page commun

SPEC §6.2, WhatsApp W1.

## 14. Données structurées (avant `</body>`)

```html
<script type="application/ld+json">
{
  "@context": "https://schema.org",
  "@graph": [
    {
      "@type": "Organization",
      "@id": "https://schoolmanager.tech/#logilab",
      "name": "LOGILAB",
      "url": "https://schoolmanager.tech/",
      "logo": "https://schoolmanager.tech/apple-touch-icon.png",
      "email": "oumarcoulou44@gmail.com",
      "address": { "@type": "PostalAddress", "addressLocality": "Bamako", "addressCountry": "ML" },
      "contactPoint": {
        "@type": "ContactPoint",
        "contactType": "customer support",
        "telephone": "+223 79 82 79 79",
        "email": "oumarcoulou44@gmail.com",
        "availableLanguage": "fr"
      }
    },
    {
      "@type": "SoftwareApplication",
      "name": "School Manager",
      "applicationCategory": "EducationalApplication",
      "operatingSystem": "Web, iOS",
      "inLanguage": "fr",
      "url": "https://schoolmanager.tech/",
      "publisher": { "@id": "https://schoolmanager.tech/#logilab" },
      "offers": {
        "@type": "AggregateOffer",
        "priceCurrency": "XOF",
        "lowPrice": "180000",
        "offerCount": "3"
      }
    }
  ]
}
</script>
```

## Écarts avec la maquette (volontaires)

- Emplacements hachurés « capture à produire » supprimés ; cartes publics sans image + rail de captures existantes.
- Pastille « Android : en test » remplacée par le badge « Bientôt sur Google Play » atténué.
- Lien d'aiguillage, liens « suite » et pied de page pointent vers les vraies pages (plus d'ancres de démonstration).
- Bouton de l'en-tête « Essai gratuit » conservé en or ; anneau de focus vert sur fond clair.
- Microtexte du haut de page : « aucun paiement demandé » ajouté ; paragraphe de l'appel final : « Tous les modules pendant 30 jours » retiré (faux, l'essai est sur Essentiel).
- Étape 3 renommée « Ouvrez les accès » (les comptes sont créés par l'école, il n'y a pas d'invitation par e-mail).
- Section confiance : « exportées et remises » remplacé par « vous pouvez demander l'export » (conforme à la politique de confidentialité).
- Premium : bouton « Voir le plan Premium » au lieu de « Démarrer l'essai » (l'essai démarre sur Essentiel, le prix Premium se discute).
