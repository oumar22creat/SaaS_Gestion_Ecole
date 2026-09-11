# ROADMAP — Backlog du projet

Règle d'usage : ne travailler que sur **une tâche non cochée à la fois**, dans l'ordre.
Cocher `[x]` uniquement quand codé + testé + committé. Ajouter une entrée dans
`SESSION_LOG.md` à chaque tâche terminée.

---

## Phase 0 — Setup du projet

- [x] Structure de repo : mono-repo GitHub avec `backend/`, `web/`, `mobile/`
- [x] CI/CD : GitHub Actions
- [x] Bibliothèque UI Web : Angular Material
- [x] Créer `README.md` fonctionnel à jour (déjà rédigé, à ajuster une fois les projets initialisés)
- [x] Initialiser le squelette Spring Boot (Java, Maven/Gradle, structure par domaine)
- [x] Initialiser PostgreSQL via docker-compose (environnement local, avec Redis)
- [x] Configurer Flyway pour les migrations versionnées
- [x] Initialiser le squelette Angular avec Angular Material installé et thème de base
- [x] Initialiser le squelette Ionic/Capacitor
- [ ] Mettre en place le pipeline GitHub Actions de base (build + tests sur chaque push)
- [ ] Configurer les environnements dev / staging / prod (fichiers de config séparés)
- [ ] Définir les design tokens (couleurs neutres, typographie, espacement) — voir `docs/DESIGN.md`
- [ ] Implémenter le chargement dynamique du branding tenant (logo + couleurs) au démarrage Web et Mobile
- [ ] Maquetter (même basse fidélité) les 6-7 écrans clés listés dans `docs/DESIGN.md` §5

## Phase 1 — MVP SaaS

### 1.1 Fondations multi-tenant
- [ ] Table `tenants` (établissements)
- [ ] Colonne `school_id` sur toutes les tables métier + contrainte FK
- [ ] Activer PostgreSQL Row-Level Security sur ces tables
- [ ] Middleware/filtre Spring qui résout le tenant courant (sous-domaine ou header) et
      l'injecte dans le contexte de requête
- [ ] Hibernate Filter global appliquant `school_id` automatiquement
- [ ] Tests d'isolation : un utilisateur du tenant A ne doit rien voir du tenant B

### 1.2 Authentification et rôles
- [ ] Tables `users`, `roles`, `permissions`
- [ ] Inscription / connexion avec JWT (access token + refresh token)
- [ ] RBAC appliqué côté serveur sur chaque endpoint
- [ ] Rôle Super-Administrateur (hors tenant, gestion de la plateforme)
- [ ] Rôles Administrateur, Direction, Enseignant, Élève, Parent, Vie scolaire,
      Secrétaire, Comptable

### 1.3 Onboarding self-service
- [ ] Formulaire public d'inscription d'un établissement
- [ ] Création automatique du tenant + compte Administrateur initial
- [ ] Assistant de configuration (wizard) : établissement → classes/matières →
      import élèves/parents (CSV) → enseignants → emploi du temps → activation

### 1.4 Abonnement (base)
- [ ] Tables `plans`, `subscriptions`, `invoices`
- [ ] Essai gratuit automatique à la création du tenant
- [ ] Intégration Stripe : checkout + webhook de confirmation de paiement
- [ ] Blocage progressif en cas d'échec de paiement (lecture seule puis suspension)

### 1.5 Élèves, parents, enseignants, classes, matières
- [ ] CRUD élèves (avec import CSV en masse)
- [ ] CRUD parents/tuteurs + association aux élèves
- [ ] CRUD enseignants
- [ ] CRUD classes et matières + affectation enseignant/classe/matière

### 1.6 Emploi du temps
- [ ] CRUD emploi du temps (classe, enseignant, matière, salle, horaire)
- [ ] Détection des conflits (salle/enseignant/classe déjà occupés)

### 1.7 Absences
- [ ] Feuille d'appel (interface rapide, pensée mobile)
- [ ] Motifs, justificatifs, historique
- [ ] Notification au parent

### 1.8 Notes
- [ ] CRUD évaluations et notes
- [ ] Calcul automatique des moyennes (par matière, par classe)

### 1.9 Dashboard établissement
- [ ] Statistiques de base : effectifs, taux de présence, moyennes

**Critère de sortie de Phase 1** : un établissement peut s'inscrire seul, configurer ses
classes/élèves/enseignants, gérer emploi du temps + absences + notes, et payer un abonnement —
le tout sans qu'aucune donnée ne fuite vers un autre tenant.

---

## Phase 2 — Pédagogie et communication
*(à détailler en tâches fines une fois la Phase 1 terminée)*

- [ ] Bulletins scolaires (génération, export PDF)
- [ ] Cahier de textes et devoirs
- [ ] Bibliothèque de documents
- [ ] Notifications push (FCM)
- [ ] Messagerie interne
- [ ] Dashboard Super-Admin (nombre de tenants, MRR, churn — métriques de base)

## Phase 3 — Administration avancée et modules complémentaires
*(à détailler en tâches fines une fois la Phase 2 terminée)*

- [ ] Vie scolaire (incidents, sanctions, convocations)
- [ ] Statistiques avancées
- [ ] Comptabilité et frais scolaires + paiement local (mobile money)
- [ ] Cantine
- [ ] Bibliothèque (emprunts/retours)
- [ ] Transport scolaire
- [ ] Domaine personnalisé et branding avancé (plan Premium)

## Phase 4 — Innovation et scalabilité
*(à détailler en tâches fines une fois la Phase 3 terminée)*

- [ ] QR code pour la présence
- [ ] Signature électronique des bulletins
- [ ] API publique + webhooks pour intégrations tierces
- [ ] Passage à Kubernetes si la charge le justifie
- [ ] Facturation à l'usage / marketplace de modules

---

## Notes de priorisation
- Ne pas commencer la Phase 2 tant que la Phase 1 n'est pas testée de bout en bout par un
  scénario complet (créer un établissement → l'utiliser → payer → suspendre → réactiver).
- Le détail des Phases 2 à 4 sera éclaté en sous-tâches (comme la Phase 1) juste avant de
  les démarrer, pour éviter un backlog trop lointain et déjà obsolète.
