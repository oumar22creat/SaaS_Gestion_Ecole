# DESIGN — Système de design et guidelines UI/UX

Objectif de ce document : que chaque écran généré par Claude Code (Web Angular et Mobile
Ionic) suive les mêmes règles visuelles, sans que tu aies à répéter le style à chaque prompt.
Sans ce cadrage, un agent qui code écran par écran produit des interfaces incohérentes
(boutons différents, espacements différents, hiérarchie visuelle qui change d'une page à l'autre).

## 0. Contrainte spécifique à ce projet : le white-label
Ce n'est pas une seule appli avec un seul branding — c'est une plateforme SaaS où **chaque
établissement doit pouvoir afficher son logo et ses couleurs**. Le design ne doit donc jamais
coder une couleur "en dur" dans un composant : tout passe par des **tokens** (variables)
resolues au runtime selon le tenant connecté.

## 1. Principes de design du produit
- **Densité d'information maîtrisée** : les utilisateurs (profs, parents, direction) consultent
  souvent sur mobile, en mouvement, entre deux cours — priorité à la rapidité de lecture et de
  saisie sur l'esthétique décorative.
- **Cohérence avant créativité** : un composant (bouton, carte, tableau) a une seule apparence
  dans toute l'application, quel que soit le rôle ou l'écran.
- **Accessibilité par défaut** : contrastes suffisants (WCAG AA minimum), tailles de cible
  tactile ≥ 44px sur mobile, jamais d'information portée uniquement par la couleur.
- **Mobile-first pour les usages fréquents** (feuille d'appel, consultation de notes,
  notifications), desktop-first pour les usages de configuration/gestion (administration,
  paramétrage d'établissement, import de données).

## 2. Design tokens (à définir une seule fois, dans le code, pas par écran)

### Couleurs
- **Couleurs neutres** (fixes, ne changent jamais avec le tenant) : fond, texte, bordures,
  états de succès/erreur/avertissement. Ce sont ces couleurs qui garantissent la lisibilité
  quel que soit le branding de l'établissement.
- **Couleur primaire du tenant** : chargée dynamiquement (logo + couleur principale +
  couleur secondaire de l'établissement), appliquée via variables CSS
  (`--tenant-primary`, `--tenant-secondary`) injectées au chargement de l'app selon le
  sous-domaine.
- Ne jamais utiliser la couleur du tenant pour du texte critique ou des messages
  d'erreur/succès — réserver ces usages aux couleurs neutres sémantiques.

### Typographie
- Une seule famille de police pour toute l'application (lisible, support multilingue si
  expansion internationale prévue).
- Échelle limitée : titres (2-3 tailles), corps de texte, texte secondaire/légende. Ne pas
  laisser un agent de code inventer une taille de police par écran.

### Espacement et layout
- Grille d'espacement en multiples de 4px (4, 8, 12, 16, 24, 32...).
- Rayon de bordure unique pour tous les composants (cartes, boutons, champs).
- Largeur de contenu maximale sur desktop pour éviter des tableaux/formulaires étalés sur
  des écrans larges.

## 3. Bibliothèque de composants

### Web (Angular)
**Décidé : Angular Material**, personnalisé avec les tokens du tenant définis en section 2.
Ne pas introduire Tailwind ou une autre bibliothèque en parallèle — un seul système de
composants pour tout le Web.

### Mobile (Ionic)
- Utiliser les composants natifs Ionic (`ion-card`, `ion-list`, `ion-button`...) qui
  s'adaptent déjà à iOS/Android, et les recolorer via les variables CSS Ionic
  (`--ion-color-primary`, etc.) mappées sur les tokens du tenant.
- Éviter de reconstruire des composants custom qui dupliquent ce qu'Ionic offre déjà —
  ça complique la maintenance et casse les comportements natifs attendus (retour arrière,
  gestes, accessibilité VoiceOver/TalkBack).

### Documentation des composants
- Si le temps le permet, mettre en place **Storybook** (Angular) pour cataloguer les
  composants communs (bouton, carte élève, badge de statut d'absence...) et éviter que
  Claude Code recrée une variante légèrement différente à chaque nouvel écran.

## 4. Theming multi-tenant — implémentation
1. Au chargement de l'app (Web ou Mobile), résoudre le tenant (sous-domaine ou sélection
   utilisateur).
2. Appeler `/api/v1/tenants/current/branding` → `{ logoUrl, primaryColor, secondaryColor, name }`.
3. Injecter ces valeurs comme variables CSS sur `:root` (Web) ou variables Ionic (Mobile).
4. Mettre en cache localement (le temps de la session) pour éviter un flash sans branding
   au démarrage.
5. Fallback sur un branding neutre par défaut si l'appel échoue (jamais d'écran cassé).

## 5. Écrans clés par rôle (à cadrer avant de les coder un par un)

| Rôle | Écran prioritaire | Points d'attention |
|---|---|---|
| Enseignant | Feuille d'appel | Mobile-first, doit se faire en quelques secondes entre deux cours, gros boutons tactiles |
| Enseignant | Saisie de notes | Saisie rapide au clavier, tabulation entre élèves |
| Parent | Tableau de bord enfant(s) | Vue synthétique : dernières notes, absences, devoirs — pas un menu à 15 entrées |
| Élève | Emploi du temps | Vue jour/semaine claire, lisible sur petit écran |
| Direction/Admin | Dashboard établissement | Densité d'info plus élevée acceptable, desktop-first |
| Super-Admin | Dashboard SaaS | Metrics (MRR, churn, tenants) — desktop uniquement, jamais exposé sur mobile grand public |
| Secrétaire | Import élèves (wizard) | Feedback clair ligne par ligne en cas d'erreur d'import |

Recommandation : maquetter ces 6-7 écrans clés (même en basse fidélité) **avant** de
lancer Claude Code sur le développement des écrans secondaires — ce sont eux qui fixent
les patterns (carte, tableau, formulaire) réutilisés partout ailleurs.

## 6. Accessibilité (non négociable, pas une option "si le temps le permet")
- Contraste texte/fond ≥ 4.5:1 (WCAG AA).
- Navigation clavier complète sur le Web (admin, direction utilisent souvent un clavier).
- Labels explicites sur tous les champs de formulaire (pas seulement des placeholders).
- Messages d'erreur explicites et associés au champ concerné, pas juste une bordure rouge.

## 7. Outils recommandés
- **Figma** (ou équivalent) pour les maquettes des écrans clés du tableau ci-dessus, même
  rapides — un agent de code produit un meilleur résultat à partir d'une image de référence
  que d'une description textuelle seule.
- **Storybook** pour documenter les composants une fois qu'ils existent.
- Aucun outil de design n'est bloquant pour démarrer : à défaut de maquettes, décrire
  précisément la disposition attendue dans le prompt (zones, hiérarchie, actions principales)
  avant de laisser Claude Code générer le HTML/Angular.

## 8. Comment prompter Claude Code pour le design
- Toujours préciser : rôle utilisateur concerné, contexte d'usage (mobile en mouvement vs
  desktop assis), et renvoyer à ce fichier (`docs/DESIGN.md`) plutôt que de redécrire les
  tokens à chaque fois.
- Exemple de prompt : *"Crée l'écran de feuille d'appel pour le rôle Enseignant (mobile,
  Ionic). Respecte docs/DESIGN.md — utilise les composants Ionic standards recolorés avec
  les tokens du tenant, pas de composants custom. Priorité : validation de la présence de
  toute la classe en moins de 30 secondes."*
- Si Claude Code propose une solution custom qui s'écarte de la bibliothèque de composants
  choisie (section 3), le signaler explicitement — sinon l'incohérence s'accumule écran
  après écran sans qu'on s'en rende compte avant la fin du projet.

## 9. Icônes de l'application (Material Symbols Rounded)

**Décision révisée le 2026-09-20.** Les emojis servaient auparavant d'identifiants visuels de
module. Ils ont été remplacés par **Material Symbols Rounded** pour trois raisons :

1. Un emoji est dessiné par le système d'exploitation : la même interface n'a pas le même
   aspect sur macOS, Windows et Android.
2. Un emoji porte ses propres couleurs, qu'aucun token ne peut surcharger. Il contredit donc
   la contrainte white-label de la section 0 : une icône doit prendre la couleur du tenant.
3. Les graisses et les tailles optiques des emojis sont incohérentes entre eux, ce qui
   empêche un rendu homogène.

### Deux jeux d'icônes, un par application (et pourquoi)
| Application | Jeu | Enregistrement |
|---|---|---|
| Web (Angular Material) | **Material Symbols Rounded** | `MatIconRegistry.setDefaultFontSetClass` dans `web/src/app/app.config.ts` |
| Mobile (Ionic) | **Ionicons** | `addIcons(APP_ICONS)` dans `mobile/src/app/app.config.ts` |

Le mobile ne réutilise pas Material Symbols parce que c'est une police **téléchargée depuis
Google Fonts au démarrage**. Acceptable dans un navigateur, inacceptable dans une application
Capacitor : un enseignant qui fait l'appel dans une salle mal couverte verrait des libellés
d'icônes à la place des icônes. Ionicons est embarqué dans le paquet de l'application, donc
disponible hors connexion. Le **sens** des icônes reste aligné entre les deux applications,
seul le dessin diffère.

### Règles
- Un seul jeu d'icônes par application, celui du tableau ci-dessus. Ne jamais ajouter une
  deuxième bibliothèque ni dessiner un SVG à la main pour un pictogramme courant.
- L'enregistrement se fait une seule fois, dans le `app.config.ts` de l'application concernée.
  Aucun composant ne redéclare la police ni ne réimporte une icône.
- Ajouter une icône côté mobile = l'ajouter à `APP_ICONS`, pas l'importer dans le composant.
- Dans une feuille de style, utiliser la ligature directement
  (`content: 'inbox'; font-family: 'Material Symbols Rounded'`) plutôt qu'un caractère décoratif.
- Chaque module garde **une icône fixe**, réutilisée dans la navigation, le titre de page et
  les raccourcis. Ne pas inventer un autre pictogramme pour le même module.

| Icône | Module / action |
|---|---|
| `school` | Élèves, marque, établissement |
| `space_dashboard` | Tableau de bord |
| `monitoring` | Résultats, statistiques avancées |
| `family_restroom` | Parents / tuteurs |
| `co_present` | Enseignants |
| `groups` | Classes |
| `menu_book` | Matières |
| `calendar_month` | Emploi du temps |
| `meeting_room` | Salles |
| `how_to_reg` | Absences / feuille d'appel |
| `edit_note` | Notes / évaluations |
| `description` | Bulletins |
| `history_edu` | Cahier de textes |
| `folder_open` | Documents |
| `forum` | Messagerie |
| `campaign` | Annonce |
| `notifications` | Notifications |
| `gavel` | Vie scolaire |
| `payments` | Frais scolaires |
| `restaurant` | Cantine |
| `local_library` | Bibliothèque |
| `directions_bus` | Transport |
| `credit_card` | Abonnement |
| `palette` | Paramètres établissement |
| `shield` | Super-Admin |
| `login` / `logout` | Connexion / déconnexion |
| `add` / `search` / `upload` / `download` / `delete` | Actions génériques |
| `inbox` / `error` / `check_circle` | État vide / erreur / succès |

## 10. Système de design implémenté (état du code)
Cette section décrit ce qui existe réellement dans le code ; les sections 1 à 8 restent les
principes. Toute nouvelle page doit réutiliser ces fichiers plutôt que redéfinir un style local.

### Fichiers source
| Fichier | Rôle |
|---|---|
| `web/src/styles/tokens.scss` | tokens web (couleurs, espacement, rayons, ombres, typo, layout) + mapping des tokens système Angular Material (`--mat-sys-*`) |
| `web/src/styles/_components.scss` | primitives d'interface partagées et overrides globaux Angular Material |
| `web/src/app/core/_auth-layout.scss` | mixin de mise en page split-screen des écrans publics (login, inscription, admin) |
| `mobile/src/theme/variables.scss` | mêmes tokens, mappés sur les variables Ionic (`--ion-color-*`) |
| `mobile/src/styles.scss` | overrides globaux Ionic + primitives d'écran mobile |

Les tokens sont dupliqués entre les deux workspaces Angular (pas de package partagé pour
l'instant) : toute modification d'un token doit être appliquée dans les **deux** fichiers.

### Palette
- Marque par défaut : vert profond `#0f5c4c` (primaire) et or `#c9a227` (secondaire) — c'est
  aussi le défaut côté backend (`Tenant.java`, migration `V53`), pour qu'un établissement
  fraîchement inscrit n'apparaisse pas en bleu Ionic.
- Tous les dérivés de marque (`--tenant-primary-strong`, `-soft`, `-softer`, `-ring`) sont
  calculés en `color-mix()` : ils suivent automatiquement le branding chargé au runtime.
- Navigation en ardoise sombre `#0b231e`, indépendante du tenant, pour rester lisible quel
  que soit le logo et la couleur choisis par l'établissement.

### Typographie
Deux familles : **Inter** pour le corps de texte, **Outfit** pour les titres et les chiffres
mis en avant. Échelle fermée de `--font-size-display` (32px) à `--font-size-caption` (12,5px).

### Primitives web réutilisables
`.page-header` + `.page-emoji` (emoji du module en médaillon), `.surface` (carte élevée),
`.section-label` (intertitre en capitales), `.stat-grid` / `.stat-card`, `.filters-row`,
`.stack-form`, `.data-table`, `.empty-state`, `.pill`, et les bandeaux `.flash-error` /
`.flash-success`.

### Primitives mobiles réutilisables
`.screen-header` + `.screen-emoji`, `.card-item` (une ligne de liste = une carte),
`.section-label`, `.empty-state`, `.flash-error` / `.flash-success`.

### Patterns de mise en page
- **Écrans publics** (login, inscription, connexion Super-Admin) : split-screen — panneau de
  marque sombre à gauche (nom, promesse produit, liste de fonctionnalités), formulaire à
  droite ; empilés verticalement sous 960px.
- **Application connectée** : barre de marque du tenant en haut, barre latérale sombre de
  264px avec carte utilisateur et liens groupés par domaine ; sous 1024px la barre latérale
  passe en tiroir derrière un bouton menu.
- **Écrans de saisie terrain** (feuille d'appel, saisie de notes) : mobile-first, une carte
  par élève, cibles tactiles ≥ 44px, statut posé en un seul appui (pas de liste déroulante
  par élève), bouton d'enregistrement en bas avec le décompte des lignes.

