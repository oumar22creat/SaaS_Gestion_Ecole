# Page Sécurité et confidentialité (`/securite`, `landing/securite.html`)

Socle : [../SPEC.md](../SPEC.md). Faits : `landing/confidentialite.html` (référence juridique),
`docs/ROADMAP.md` (isolation, tests), `CLAUDE.md` (RLS, filtre Hibernate).

**Objectif** : rassurer un directeur (et un parent) avec des faits concrets, sans jargon
inutile. Le vocabulaire technique (PostgreSQL, sécurité au niveau des lignes) n'apparaît
qu'une fois, entre parenthèses, pour les lecteurs qui le cherchent. La politique de
confidentialité reste le texte de référence : cette page la résume et y renvoie.

## Balises

| Balise | Valeur |
|---|---|
| `title` | Sécurité et confidentialité des données scolaires \| School Manager |
| `meta description` | Chaque école a son espace, isolé par la base de données. Accès par rôle, échanges et sauvegardes chiffrés, export de vos données. Ce que nous faisons, concrètement. |
| `canonical` / `og:url` | `https://schoolmanager.tech/securite` |
| `og:title` | Les données de votre école restent les vôtres |
| `og:description` | Isolation par école, accès par rôle, sauvegardes chiffrées, export avant suppression. |
| WhatsApp de la page | W7 |
| `aria-current="page"` | lien « Sécurité » |

## Gabarit

0. En-tête · 1. Introduction · 2. Ce que nous faisons · 3. Où sont les données · 4. Ce que
nous ne collectons pas · 5. Vos droits · 6. Appel final · 7. Pied de page.
Fonds : 1 papier · 2 papier · 3 papier-2 · 4 papier · 5 papier-2 · 6 nuit.

---

## 1. Introduction (`.page-intro`, sans visuel)

| Élément | Texte |
|---|---|
| Surtitre | Sécurité et confidentialité |
| `h1` | Les données de votre école restent les vôtres. |
| Chapô | School Manager traite des données d'enfants : notes, absences, familles, paiements. Voici, concrètement, comment elles sont protégées. |
| Encadré `.notice` | **L'école reste maîtresse de ses données.** C'est elle qui décide ce qui est saisi et qui y accède. Aucune donnée n'est vendue, louée, ni utilisée à des fins publicitaires. |

## 2. Ce que nous faisons (`aria-labelledby="titre-mesures"`)

`h2` : « Quatre protections, expliquées simplement. »

Quatre blocs `.trust-item` (icône 32 px, `h3`, paragraphes) :

| Icône | `h3` | Texte |
|---|---|---|
| `i-shield` | Un espace par école | Chaque école a son propre espace. Ses données sont inaccessibles aux autres écoles, même en cas d'erreur dans l'application : l'isolement est appliqué par la base de données elle-même (PostgreSQL, sécurité au niveau des lignes), pas seulement par le code. Des tests automatiques vérifient, à chaque modification du logiciel, qu'un utilisateur d'une école ne peut rien voir d'une autre. |
| `i-users` | Chacun son périmètre | 8 rôles, du secrétariat à l'enseignant. Un enseignant voit ses classes, la comptabilité les règlements, la direction l'ensemble. Un parent ne voit que le dossier de ses enfants, un élève que le sien. Les droits sont vérifiés sur le serveur, pas dans le navigateur. Lien en fin de paragraphe : « Voir les 8 rôles » → `/fonctionnalites#roles`. |
| `i-lock` | Échanges et sauvegardes chiffrés | Toutes les connexions passent par HTTPS : l'application refuse toute connexion en clair. Les mots de passe ne sont jamais stockés en clair. Les sauvegardes sont chiffrées avant d'être écrites, et conservées 30 jours. Les actions sensibles sont journalisées. |
| `i-export` | Vos données vous suivent | Si vous arrêtez, vous pouvez demander l'export de vos données. Elles ne sont supprimées que 90 jours après la fin du service, ce qui vous laisse le temps de les récupérer. |

Responsive : 320 / 375 : 1 colonne. 768 : 2 colonnes. 1280 : 2 colonnes (textes longs ;
pas 4 colonnes comme sur l'accueil), `gap: 32px 48px`.

## 3. Où sont les données (fond papier-2, conteneur `--narrow`)

| Élément | Texte |
|---|---|
| Icône | `i-server` |
| `h2` | Où sont hébergées les données. |
| Paragraphe 1 | Le serveur et la base de données sont hébergés chez Hostinger, dans l'Union européenne. Cloudflare gère le nom de domaine et le chiffrement des échanges. Les notifications envoyées sur le téléphone passent par Google (Firebase Cloud Messaging). |
| Paragraphe 2 | Les serveurs étant situés hors du Mali, les données y sont transférées. La liste complète des prestataires et des durées de conservation figure dans la politique de confidentialité. |
| Lien `.more` | Lire la politique de confidentialité → `/confidentialite.html` |

Ne pas écrire « aucun autre prestataire » sur cette page : la liste complète des sous-traitants
(dont Google Firebase Cloud Messaging pour les notifications) est dans la politique de confidentialité.

## 4. Ce que nous ne collectons pas

| Élément | Texte |
|---|---|
| Icône | `i-eye-off` |
| `h2` | Ce que nous ne collectons pas. |
| Liste `.ticks` (icône `i-minus` au lieu de `i-check`) | Aucune géolocalisation. · Aucun accès aux contacts, aux photos, au micro ni à la caméra du téléphone. · Aucun traceur publicitaire, aucun outil de mesure d'audience. · Ce site ne dépose aucun cookie de mesure d'audience ni de publicité. |

## 5. Vos droits (fond papier-2, conteneur `--narrow`)

| Élément | Texte |
|---|---|
| `h2` | Vos droits. |
| Paragraphe 1 | Vous pouvez demander à accéder aux données qui vous concernent, à les faire corriger, à les faire effacer, ou à en obtenir une copie. |
| Paragraphe 2 | **Adressez-vous d'abord à votre école** : c'est elle qui détient le dossier et peut vérifier qui demande quoi. Sans réponse, écrivez à oumarcoulou44@gmail.com (lien `mailto:`) : une réponse vous sera apportée sous 30 jours. |

## 6. Appel final (`.closer`)

| Élément | Texte |
|---|---|
| `h2` | Une question sur la protection des données ? |
| Paragraphe | Écrivez-nous : nous vous répondons directement. |
| `.btn-gold` | icône WhatsApp + « Poser ma question sur WhatsApp » → W7 |
| `.btn-outline-light` | Lire la politique de confidentialité → `/confidentialite.html` |

## Responsive récapitulatif

| Section | 320 / 375 | 768 | 1280 |
|---|---|---|---|
| 1 | Une colonne, encadré pleine largeur | Idem | Texte `max-width: 46rem` |
| 2 | Blocs empilés | 2 colonnes | 2 colonnes, conteneur 1000 px |
| 3 à 5 | Une colonne | Une colonne | Conteneur `--narrow` |
