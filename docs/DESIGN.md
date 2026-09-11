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
