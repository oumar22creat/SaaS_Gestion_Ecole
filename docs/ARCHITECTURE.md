# ARCHITECTURE — Décisions techniques

Ce fichier documente les décisions déjà prises. Objectif : que Claude Code ne re-débatte
pas d'un choix déjà tranché d'une session à l'autre. Toute nouvelle décision structurante
doit être ajoutée ici (format ADR court), pas seulement discutée en session.

---

### ADR-001 — Isolation multi-tenant
**Décision** : base de données partagée, discriminant `school_id` sur chaque table métier,
avec PostgreSQL Row-Level Security en filet de sécurité en plus du filtre applicatif.
**Raison** : coût d'infra et de maintenance bien plus faible qu'un schéma ou une base par
établissement, suffisant pour le volume visé en MVP.
**Révision possible** : si un client Enterprise exige une isolation physique (base dédiée),
prévoir une bascule au cas par cas, pas une réécriture globale.

### ADR-002 — Authentification
**Décision** : JWT avec access token (courte durée) + refresh token (longue durée, rotatif).
RBAC appliqué côté serveur uniquement (jamais de confiance dans un rôle envoyé par le client).

### ADR-003 — Paiement
**Décision** : Stripe pour les paiements internationaux/récurrents (abonnement SaaS),
+ un fournisseur de paiement mobile local pour les frais de scolarité (Phase 3) — fournisseur
exact à confirmer selon le marché cible.
**Statut** : fournisseur mobile money non encore choisi — à trancher avant la Phase 3.

### ADR-004 — Traitements asynchrones
**Décision** : les traitements lourds (génération de bulletins en masse, envois groupés
d'e-mails/notifications) passent par une file d'attente, pas par l'API synchrone.
**Statut** : technologie (RabbitMQ vs Kafka vs solution managée) à trancher en Phase 2,
pas bloquant pour le MVP.

### ADR-005 — Résolution du tenant côté client
**Décision** : sous-domaine par établissement (`ecole-x.monapp.com`) résolu via DNS wildcard.
**Alternative retenue en complément** : sélecteur d'établissement pour les comptes rattachés
à plusieurs tenants (ex. parent avec enfants dans deux écoles).

---

## Structure de repo (Phase 0)
```
/backend            → Spring Boot, organisé par domaine
  /auth
  /tenant
  /billing
  /student
  /teacher
  /class
  /timetable
  /attendance
  /grade
  ...
/web                 → Angular, structuré par feature module
/mobile              → Ionic/Capacitor
/docs                → ce dossier
CLAUDE.md
```

## Structure d'un module backend type
```
/student
  StudentController.java
  StudentService.java
  StudentRepository.java
  Student.java (entity)
  StudentDto.java
  StudentMapper.java
```

### ADR-006 — Repo, plateforme et bibliothèque UI (décidé)
**Décision** : mono-repo unique hébergé sur GitHub (`backend/`, `web/`, `mobile/` dans le
même dépôt) + GitHub Actions pour la CI/CD. Bibliothèque UI Web : Angular Material,
personnalisée avec les tokens du tenant (voir `docs/DESIGN.md`).
**Raison** : simplicité de navigation pour un agent de code (Claude Code) travaillant seul
sur le projet, un seul historique Git à suivre, un seul pipeline à maintenir.

---

## Points ouverts (à trancher avant d'y arriver, pas maintenant)
- Fournisseur mobile money ? (avant Phase 3)
- Hébergement de production (cloud choisi, région) ? (avant le premier déploiement staging)
- Kubernetes ou déploiement simple Docker Compose au démarrage ? (avant Phase 4, ou avant
  si le nombre de tenants grossit plus vite que prévu)
