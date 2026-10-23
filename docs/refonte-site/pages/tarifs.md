# Page Tarifs (`/tarifs`, `landing/tarifs.html`)

Socle : [../SPEC.md](../SPEC.md). Faits : SPEC §2 (tarifs et contenu des plans repris de
`landing/index.html`, essai d'après `docs/ARCHITECTURE.md`).

**Objectif** : un directeur trouve son plan en fonction de son nombre d'élèves, voit le prix
annuel et mensuel, comprend comment se passe l'essai et ce qui arrive après. Aucune mention de
moyen de paiement.

## Balises

| Balise | Valeur |
|---|---|
| `title` | Tarifs en FCFA, à partir de 180 000 FCFA par an \| School Manager |
| `meta description` | Trois plans selon le nombre d'élèves : Essentiel 180 000, Standard 750 000, Premium dès 1 800 000 FCFA par an. 30 jours d'essai gratuit, sans engagement. |
| `canonical` / `og:url` | `https://schoolmanager.tech/tarifs` |
| `og:title` | Les tarifs de School Manager, en FCFA |
| `og:description` | Choisissez selon votre nombre d'élèves. 30 jours d'essai gratuit, sans engagement. |
| WhatsApp de la page | W4 ; W5 pour le devis Premium |
| `aria-current="page"` | lien « Tarifs » |

## Gabarit

0. En-tête · 1. Introduction · 2. Les trois plans · 3. Comparer les plans · 4. Comment se passe
l'essai · 5. Questions fréquentes · 6. Appel final · 7. Pied de page.
Fonds : 1 papier · 2 papier · 3 papier-2 · 4 papier · 5 papier-2 · 6 nuit.

---

## 1. Introduction (`.page-intro`, sans visuel)

| Élément | Texte |
|---|---|
| Surtitre | Tarifs |
| `h1` | Des prix affichés, en FCFA. Choisissez selon votre nombre d'élèves. |
| Chapô | L'abonnement est payé par l'école, une fois par an. L'application est gratuite pour les parents. Vous pouvez essayer School Manager gratuitement pendant 30 jours, sans engagement. |

Pas de bouton dans l'introduction : les cartes suivent immédiatement.

## 2. Les trois plans (`aria-labelledby="titre-plans"`)

`h2` masqué visuellement (`.sr-only`, `id="titre-plans"`) : « Les trois plans ».

| | Essentiel (`id="plan-essentiel"`) | Standard (`id="plan-standard"`) | Premium (`id="plan-premium"`) |
|---|---|---|---|
| `h3` | Essentiel | Standard | Premium |
| Effectif | Jusqu'à 150 élèves | Jusqu'à 800 élèves | Effectif illimité |
| Prix | 180 000 / FCFA par an | 750 000 / FCFA par an | dès 1 800 000 / FCFA par an |
| Équivalences | Soit 15 000 FCFA par mois, ou 1 200 FCFA par élève et par an. | Soit 62 500 FCFA par mois, ou environ 940 FCFA par élève et par an. | Tarif dégressif selon l'effectif, défini avec vous. |
| Coches | Scolarité, notes et bulletins · Absences et emploi du temps · Application mobile pour les enseignants, les parents et les élèves · Support par e-mail | Tout le plan Essentiel · Frais de scolarité et facturation · Statistiques de direction · Cantine, transport, bibliothèque en option | Tous les modules, cantine, transport et bibliothèque compris · Statistiques avancées et exports · Accompagnement à la reprise de vos données · Support par e-mail |
| Bouton `.btn-outline` pleine largeur | Démarrer l'essai gratuit → `/register` | Choisir Standard → `/register` | icône WhatsApp + « Demander un devis » → W5 (texte masqué « (WhatsApp, nouvel onglet) ») |

Sous les cartes, paragraphe `.note` (15,5 px, `--ink-2`, `max-width: 70ch`) :

> Tous les montants sont en francs CFA, par établissement et par an. Un effectif qui dépasse de
> peu un palier peut être facturé à l'élève, à 1 000 FCFA par élève et par an.

Puis une ligne : « Le support par e-mail se fait à l'adresse » + lien `oumarcoulou44@gmail.com`
(`mailto:`) + « . »

| Largeur | Disposition |
|---|---|
| 320 / 375 | Cartes empilées dans l'ordre Essentiel, Standard, Premium ; montant 26 px ; bouton pleine largeur. |
| 768 | Cartes empilées, `max-width: 560px` centrées (3 colonnes trop étroites pour les coches). |
| 1280 | 3 colonnes égales, boutons alignés en bas. |

## 3. Comparer les plans (`id="comparer"`, fond papier-2)

| Élément | Texte |
|---|---|
| `h2` (`id="titre-comparer"`) | Comparer les plans en détail. |

Tableau `.compare` (SPEC §6.16) dans `.table-scroll` (`role="region"`, `aria-label="Tableau : comparaison des plans"`, distinct du nom de la section).
`<caption class="sr-only">Fonctionnalités incluses dans chaque plan</caption>`.
En-têtes de colonnes : « Fonctionnalité » · « Essentiel » · « Standard » · « Premium ».

| Fonctionnalité (`th scope="row"`) | Essentiel | Standard | Premium |
|---|---|---|---|
| Effectif maximal | 150 élèves | 800 élèves | Illimité |
| Élèves, familles, classes, import CSV | Inclus | Inclus | Inclus |
| Notes, moyennes et bulletins PDF | Inclus | Inclus | Inclus |
| Absences et emploi du temps | Inclus | Inclus | Inclus |
| Application mobile (enseignants, parents, élèves) | Inclus | Inclus | Inclus |
| Frais de scolarité et facturation | Non inclus | Inclus | Inclus |
| Statistiques de direction | Non inclus | Inclus | Inclus |
| Cantine, transport, bibliothèque | Non inclus | En option | Inclus |
| Statistiques avancées et exports | Non inclus | Non inclus | Inclus |
| Accompagnement à la reprise des données | Non inclus | Non inclus | Inclus |
| Support par e-mail | Inclus | Inclus | Inclus |
| Prix annuel | 180 000 FCFA | 750 000 FCFA | dès 1 800 000 FCFA |

« Inclus » = icône `i-check` verte + `<span class="sr-only">Inclus</span>` ; « Non inclus » =
icône `i-minus` `--ink-2` + `<span class="sr-only">Non inclus</span>` ; « En option », effectifs
et prix en texte visible. Dernière ligne (prix) en Outfit 600.

| Largeur | Disposition |
|---|---|
| 320 | 4 colonnes : libellé ≈ 120 px (retour à la ligne), 3 colonnes de 56 px centrées ; « En option » en 13 px sur deux lignes ; aucun défilement attendu. Si le contenu déborde (police agrandie), le conteneur défile, pas la page. |
| 375 / 768 | Tableau pleine largeur (pas d'en-tête collant, voir SPEC §6.16). |
| 1280 | `max-width: 900px`, centré. |

## 4. Comment se passe l'essai (`aria-labelledby="titre-essai"`)

| Élément | Texte |
|---|---|
| Surtitre | Essai gratuit |
| `h2` | 30 jours pour essayer, avec vos vraies classes. |

Étapes `.steps` :

| N° | `h3` | Texte |
|---|---|---|
| 1 | Vous ouvrez votre espace | En quelques minutes, sur schoolmanager.tech. Aucun paiement n'est demandé. |
| 2 | Vous l'utilisez pendant 30 jours | Sur la plateforme web et sur l'application, avec vos classes, vos élèves et vos enseignants. L'essai fonctionne avec le plan Essentiel. |
| 3 | Vous choisissez votre plan | Celui qui correspond à votre nombre d'élèves. Sans abonnement à la fin de l'essai, votre espace passe en lecture seule. |

Boutons : `.btn-green` « Ouvrir mon espace d'essai » → `/register` ; `.btn-outline` icône
WhatsApp + « Une question avant de commencer ? » → W4.

## 5. Questions fréquentes (`id="questions"`, fond papier-2, conteneur `--narrow`)

| Élément | Texte |
|---|---|
| Surtitre | Questions fréquentes |
| `h2` | Avant de vous abonner. |

| Question | Réponse |
|---|---|
| Comment choisir mon plan ? | Selon votre nombre d'élèves : jusqu'à 150, Essentiel ; jusqu'à 800, Standard ; au-delà, ou si vous voulez tous les modules, Premium. Si vous dépassez de peu un palier, les élèves en plus peuvent être facturés 1 000 FCFA par élève et par an. |
| L'essai donne-t-il accès à tous les modules ? | L'essai fonctionne avec le plan Essentiel : scolarité, notes, bulletins, absences, emploi du temps et application mobile. Pour voir la cantine, le transport ou la bibliothèque, demandez une démonstration sur WhatsApp. (« demandez une démonstration sur WhatsApp » en lien W4) |
| Que se passe-t-il après les 30 jours d'essai ? | Pour continuer, vous choisissez un plan. Sans abonnement, votre espace passe en lecture seule : vous consultez vos données, mais vous ne pouvez plus en ajouter. L'accès est ensuite suspendu. |
| Comment se fait le paiement ? | L'abonnement est annuel et payé par l'école. Pour les modalités de règlement, écrivez-nous sur WhatsApp ou par e-mail : nous vous répondons directement. (« WhatsApp » en lien W4, « e-mail » en lien `mailto:`) |
| Les parents paient-ils quelque chose ? | Non. L'application est gratuite pour les parents. Ils se connectent avec les identifiants remis par l'école. |
| Que deviennent nos données si nous arrêtons ? | Vous pouvez demander l'export de vos données. Elles ne sont supprimées que 90 jours après la fin du service, ce qui vous laisse le temps de les récupérer. |
| Qu'est-ce que l'accompagnement à la reprise des données ? | Inclus dans le plan Premium : nous vous aidons à importer dans School Manager les informations que vous tenez aujourd'hui dans vos fichiers. |

## 6. Appel final (`.closer`)

| Élément | Texte |
|---|---|
| `h2` | Ouvrez votre espace avant le prochain trimestre. |
| Paragraphe | 30 jours d'essai gratuit, sans engagement. Aucun paiement demandé pour commencer. |
| `.btn-gold` | Essayer gratuitement 30 jours → `/register` |
| `.btn-outline-light` | icône WhatsApp + « Poser une question sur WhatsApp » → W4 |

## Points de vigilance (agent-relecteur)

- Aucune carte mise en avant, aucun « le plus choisi », aucune réduction ni date limite.
- Aucun moyen de paiement nommé (ni espèces, ni virement, ni mobile money, ni carte, ni Stripe).
- Retirés du site actuel : « Domaine personnalisé à votre nom », « Bulletins et notifications à
  votre charte », « Interlocuteur dédié et engagement de service », « Support par e-mail et
  chat », « payables à la rentrée, au rythme de trésorerie », « Essai de 30 jours sur tous les plans ».
- Questions ouvertes : SPEC §11, points 4 à 9.
