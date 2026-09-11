# MOCKUPS — Maquettes basse fidélité des écrans clés

Réfère-toi à `docs/DESIGN.md` (principes, tokens, bibliothèques de composants) avant de lire
ce document — il ne le répète pas.

Ce document couvre les 6-7 écrans clés listés dans `docs/DESIGN.md` §5, **avant** leur
développement réel en Phase 1+. Conformément à `docs/DESIGN.md` §7 ("aucun outil de design
n'est bloquant pour démarrer : à défaut de maquettes, décrire précisément la disposition
attendue"), ce sont des wireframes texte/ASCII, pas du code Angular/Ionic — développer les
écrans eux-mêmes (feuille d'appel, notes, emploi du temps, dashboards...) est un livrable de
Phase 1/2 (voir `docs/ROADMAP.md`), pas de la Phase 0.

Chaque fiche précise : plateforme cible, objectif, zones/hiérarchie, composants
Material/Ionic à utiliser (jamais de composant custom qui duplique la bibliothèque choisie),
et les points d'attention du tableau de `docs/DESIGN.md` §5.

---

## 1. Enseignant — Feuille d'appel
**Plateforme : Mobile (Ionic).** Mobile-first, doit se faire en quelques secondes entre deux
cours, gros boutons tactiles (cible ≥ 44px).

```
┌─────────────────────────────┐
│ ion-toolbar : "3e A — Maths" │
├─────────────────────────────┤
│ ion-list (un ion-item par    │
│ élève) :                     │
│  [Photo] Nom Prénom   ✓ ✗ ⏰ │  ← ion-segment ou 3 ion-button
│  [Photo] Nom Prénom   ✓ ✗ ⏰ │    (présent/absent/retard),
│  ...                         │    déjà coché "présent" par défaut
├─────────────────────────────┤
│ ion-button "Valider l'appel" │  ← pleine largeur, en bas, fixe
└─────────────────────────────┘
```
Composants : `ion-list`/`ion-item`, `ion-avatar`, `ion-segment` (ou 3 `ion-button` par
ligne), `ion-button` (pleine largeur, sticky en bas). Pas de formulaire à remplir champ par
champ : tout coché par défaut sur "présent", l'enseignant ne touche que les exceptions.

## 2. Enseignant — Saisie de notes
**Plateforme : Web (Angular Material).** Saisie rapide au clavier, tabulation entre élèves
(usage assis, souvent après les cours).

```
┌───────────────────────────────────────────┐
│ Titre : "Contrôle du 12/09 — 3e A — Maths" │
├───────────────────────────────────────────┤
│ mat-table                                  │
│  Nom Prénom        | Note /20 | Absent     │
│  ------------------|----------|--------    │
│  Nom Prénom        | [input]  | [checkbox] │  ← tabulation verticale entre inputs
│  Nom Prénom        | [input]  | [checkbox] │
│  ...                                       │
├───────────────────────────────────────────┤
│ [Enregistrer]                    (mat-button, en bas à droite) │
└───────────────────────────────────────────┘
```
Composants : `mat-table` avec une colonne `matInput` éditable par ligne, `mat-checkbox`
pour "absent" (note non applicable), `mat-button` "Enregistrer". Ordre de tabulation =
ordre des lignes, pas besoin de cliquer chaque champ à la souris.

## 3. Parent — Tableau de bord enfant(s)
**Plateforme : Mobile (Ionic).** Vue synthétique — dernières notes, absences, devoirs — pas
un menu à 15 entrées.

```
┌─────────────────────────────┐
│ ion-toolbar : "Bonjour, [Parent]" │
│ (sélecteur d'enfant si > 1 : ion-segment) │
├─────────────────────────────┤
│ ion-card "Dernières notes"   │
│  Maths: 14/20 · Français: 16/20 │
├─────────────────────────────┤
│ ion-card "Absences récentes" │
│  Aucune absence cette semaine│
├─────────────────────────────┤
│ ion-card "Devoirs à venir"   │
│  Maths pour lundi · SVT pour mardi │
└─────────────────────────────┘
```
Composants : `ion-card` (une par bloc d'info, 3 max), `ion-segment` pour changer d'enfant.
Pas de navigation profonde : chaque carte peut mener à un écran de détail (hors scope
Phase 0), mais le dashboard lui-même reste une vue de synthèse en un seul écran.

## 4. Élève — Emploi du temps
**Plateforme : Mobile (Ionic).** Vue jour/semaine claire, lisible sur petit écran.

```
┌─────────────────────────────┐
│ ion-segment : [Jour] [Semaine] │
├─────────────────────────────┤
│ Vue "Jour" :                 │
│  08h-09h  Maths      Salle 12│  ← ion-item, couleur de fond = matière
│  09h-10h  Français   Salle 4 │
│  10h-10h15  — Pause —        │
│  ...                         │
└─────────────────────────────┘
```
Composants : `ion-segment` (bascule jour/semaine), `ion-list`/`ion-item` pour la vue jour.
Vue semaine (si le temps le permet en Phase 1+) : grille simple, une colonne par jour — pas
de librairie de calendrier tierce tant que la vue liste suffit.

## 5. Direction/Admin — Dashboard établissement
**Plateforme : Web (Angular Material).** Densité d'info plus élevée acceptable,
desktop-first.

```
┌──────────────────────────────────────────────────────┐
│ Titre : "Tableau de bord — [Établissement]"           │
├───────────────┬───────────────┬──────────────────────┤
│ mat-card       │ mat-card       │ mat-card             │
│ "Effectifs"    │ "Taux présence"│ "Moyenne générale"   │
│  842 élèves    │  96,4 %        │  13,2/20             │
├───────────────┴───────────────┴──────────────────────┤
│ mat-table "Alertes" (absences répétées, notes en baisse) │
└──────────────────────────────────────────────────────┘
```
Composants : `mat-grid-list` ou `mat-card` en rangée (3-4 indicateurs clés), `mat-table`
pour le détail/alertes en dessous. Largeur de contenu max desktop (voir tokens
`--content-max-width`) pour ne pas étaler les tableaux sur un écran large.

## 6. Super-Admin — Dashboard SaaS
**Plateforme : Web uniquement (Angular Material), jamais exposé sur mobile grand public.**
Metrics MRR, churn, tenants.

```
┌──────────────────────────────────────────────────────┐
│ Titre : "Console Super-Admin"                          │
├───────────────┬───────────────┬──────────────────────┤
│ mat-card "MRR" │ mat-card "Churn"│ mat-card "Tenants actifs" │
│  12 400 €      │  2,1 %          │  38                  │
├──────────────────────────────────────────────────────┤
│ mat-table "Établissements" (nom, plan, statut paiement) │
└──────────────────────────────────────────────────────┘
```
Composants : identiques à l'écran 5 (mêmes patterns carte/tableau, cohérence avant
créativité). Accès restreint au rôle Super-Administrateur (RBAC, voir Phase 1.2) — aucune
route mobile ne doit exposer cet écran.

## 7. Secrétaire — Import élèves (wizard)
**Plateforme : Web (Angular Material).** Feedback clair ligne par ligne en cas d'erreur
d'import.

```
┌──────────────────────────────────────────────────────┐
│ mat-stepper horizontal :                                │
│  [1. Déposer le fichier] → [2. Vérifier] → [3. Confirmer] │
├──────────────────────────────────────────────────────┤
│ Étape 2 "Vérifier" :                                    │
│  mat-table (une ligne par élève importé)                │
│   Ligne 12 : Nom manquant        ⚠ (mat-icon warn)      │
│   Ligne 27 : Classe inconnue "3F" ⚠                      │
│   ... (lignes valides en gris clair, pas de surcharge)   │
├──────────────────────────────────────────────────────┤
│ [Retour]                          [Confirmer l'import]   │
└──────────────────────────────────────────────────────┘
```
Composants : `mat-stepper` (3 étapes), `mat-table` pour la prévisualisation ligne par ligne,
icônes d'état (`mat-icon` avec la couleur sémantique `--color-warning`/`--color-danger`,
jamais la couleur seule — texte explicite à côté, voir `docs/DESIGN.md` §6).

---

## Patterns communs qui se dégagent (à réutiliser partout ailleurs)
- **Carte de synthèse** (`mat-card` / `ion-card`) : un chiffre ou une liste courte, jamais
  plus de 3-4 par écran.
- **Tableau éditable/consultable** (`mat-table` Web ; liste `ion-list` Mobile) : toujours la
  même structure colonne/ligne, jamais de tableau custom.
- **Feedback d'erreur explicite** : texte + icône colorée, jamais la couleur seule (accessibilité,
  `docs/DESIGN.md` §6).
- **Action principale toujours visible** : bouton pleine largeur en bas sur Mobile (feuille
  d'appel), bouton en bas à droite sur Web (formulaires).
