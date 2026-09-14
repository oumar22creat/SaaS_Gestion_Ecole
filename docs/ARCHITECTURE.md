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

**Implémentation (Phase 1.1) et pièges rencontrés — à connaître avant de créer toute
nouvelle entité scopée `school_id` :**
- Mécanisme : `com.schoolsaas.common.TenantScopedEntity` (classe de base JPA), filtre
  Hibernate `tenantFilter` activé pour chaque requête par
  `com.schoolsaas.tenant.TenantContextInterceptor` (résout le tenant depuis le JWT, l'en-tête
  `X-Tenant-Id`, ou le sous-domaine), plus une politique RLS PostgreSQL sur chaque table
  scopée (voir `V2__create_users_table.sql` comme modèle).
- **Piège 1 — `@Filter` sur une `@MappedSuperclass` n'est pas fiable en Hibernate.**
  Chaque entité concrète DOIT redéclarer elle-même
  `@Filter(name = "tenantFilter", condition = "school_id = :tenantId")` sur sa classe (le
  `@FilterDef` correspondant, lui, est déclaré une seule fois — voir
  `com.schoolsaas.common.package-info.java`).
- **Piège 2 — ordre des intercepteurs MVC.** `TenantContextInterceptor` doit s'exécuter
  APRÈS `OpenEntityManagerInViewInterceptor` (enregistré par Spring Boot), sinon
  l'EntityManager partagé n'est pas encore lié au thread de la requête et l'activation du
  filtre n'a aucun effet durable. D'où `.order(100)` dans `TenantWebConfig`.
- **Piège 3 — `EntityManager#find()`/`getReference()` (donc `Repository#findById`/
  `getReferenceById`) NE PASSENT PAS par les filtres Hibernate.** Un id valide dans un autre
  tenant serait sinon renvoyé tel quel — exactement l'exemple `/api/students/42` du
  cahier-des-charges.md §2.2. Fixé structurellement (pas au cas par cas) via
  `com.schoolsaas.common.TenantScopedRepositoryImpl`, classe de base de TOUS les repositories
  (`@EnableJpaRepositories(repositoryBaseClass = ...)` sur `SchoolSaasApplication`), qui
  réimplémente ces deux méthodes en JPQL.
- **Piège 4 — Row-Level Security ne protège RIEN pour un rôle superutilisateur
  PostgreSQL**, y compris avec `FORCE ROW LEVEL SECURITY` (comportement PostgreSQL, aucune
  exception possible). Le rôle créé par `POSTGRES_USER` dans docker-compose EST
  superutilisateur. Solution : deux rôles distincts, voir `backend/docker/postgres-init/
  01-create-app-role.sh` — un rôle admin superutilisateur pour Flyway (migrations DDL) et un
  rôle applicatif restreint pour le backend au runtime (`spring.datasource.*` vs
  `spring.flyway.*` dans `application.yml`). Preuve du mécanisme :
  `TenantIsolationTest#rowLevelSecurityAloneBlocksAccessForANonSuperuserRole`.
- **Piège 5 — variable de session PostgreSQL (`set_config`) et connexions JDBC.** Par
  défaut, Hibernate peut relâcher puis réacquérir une connexion physique différente entre
  deux transactions `@Transactional` distinctes au sein d'une même requête (OSIV garde
  l'EntityManager/Session ouvert, pas forcément la même connexion physique) — la variable de
  session RLS posée dans une transaction pourrait donc ne plus être visible dans une
  transaction suivante. Fixé via `hibernate.connection.handling_mode:
  DELAYED_ACQUISITION_AND_HOLD` dans `application.yml` (une seule connexion physique tenue
  pour toute la durée de la requête).
- Le filtre Hibernate reste le mécanisme **principal et obligatoire** (CLAUDE.md règle 2) ;
  RLS est un filet de sécurité best-effort en plus, pas un substitut.

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
  /schoolclass
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

### ADR-007 — Nom de package pour le domaine "classes scolaires" (décidé)
**Décision** : le package Java du domaine "classes scolaires" s'appelle `schoolclass`
(et non `class`).
**Raison** : `class` est un mot réservé du langage Java, invalide comme nom de package.

### ADR-008 — Comptes Super-Administrateur hors tenant (décidé, Phase 1.2)
**Décision** : les comptes Super-Administrateur vivent dans une table séparée
(`platform_admins`, entité `com.schoolsaas.auth.PlatformAdmin`), PAS dans la table `users`
scopée par tenant. Connexion via un endpoint séparé (`/api/v1/admin/auth/login`), namespace
cohérent avec `docs/API_CONVENTIONS.md` ("Endpoints d'administration SaaS").
**Raison** : cahier-des-charges.md §2.5/§5 dit explicitement que ce rôle "n'appartient à
aucun établissement" — rendre `school_id` nullable sur `users` pour ce seul cas aurait
affaibli la contrainte NOT NULL/RLS pour TOUS les autres rôles, pour un cas qui est
structurellement différent (aucune notion de tenant courant à cette identité).
**Simplification associée** : pas de tables `roles`/`permissions` dynamiques (contrairement
à la liste indicative de `docs/DATA_MODEL.md`) — un rôle fixe par utilisateur
(`com.schoolsaas.auth.Role`, énumération), contrôle d'accès via `@PreAuthorize` par rôle sur
chaque endpoint. Suffisant pour les 9 rôles fixes du cahier des charges ; à revoir seulement
si un besoin réel de permissions granulaires/personnalisables apparaît.

### ADR-009 — Abonnement, paiement Stripe et blocage progressif (décidé, Phase 1.4)
**Décision** : `plans`, `subscriptions`, `invoices` sont des entités plateforme (comme
`tenants`), PAS des `TenantScopedEntity` — pas de `school_id`/RLS, filtrage par tenant fait
manuellement dans les repositories (`SubscriptionRepository#findByTenantId`,
`InvoiceRepository#findAllByTenantIdOrderByCreatedAtDesc`), exactement comme `Tenant`
lui-même (voir ADR-001). Un seul `Subscription` par tenant pour le MVP (pas d'historique de
changement de plan/upgrade-downgrade, hors périmètre de ROADMAP.md 1.4).

**Devise** : XOF (Franc CFA, marché cible retenu), devise "zéro décimale" chez Stripe — voir
le commentaire de `V4__create_plans_table.sql`. `price_cents`/`amount_due` sont envoyés tels
quels à l'API Stripe (pas de `*100`), contrairement à une devise comme EUR/USD.

**Essai gratuit** : automatique à la création du tenant (`TenantRegistrationService` appelle
`SubscriptionService#createTrialSubscription`), sur le plan par défaut configuré
(`app.billing.default-trial-plan-code`, `ESSENTIEL`) — aucun choix de plan n'est demandé à
l'inscription (l'assistant de configuration qui le proposerait, cahier-des-charges.md §20.2,
n'est pas construit, voir ROADMAP.md 1.3). Changement de plan possible après coup via
`POST /api/v1/billing/checkout`.

**Paiement** : Stripe Checkout (mode `subscription`) + webhook
(`POST /api/v1/billing/webhooks/stripe`, authentifié uniquement par la signature
`Stripe-Signature`, pas de JWT). `com.schoolsaas.billing.StripeGateway` est la seule
frontière avec le SDK Stripe (`StripeGatewayImpl`) — permet aux tests de simuler Stripe sans
appel réseau (Mockito, voir CLAUDE.md règle 4). Traitement webhook idempotent par upsert
(`stripe_invoice_id`/`stripe_subscription_id`), pas de table d'événements déjà traités
(Stripe peut redélivrer, rejouer ces mises à jour d'état est sans effet de bord).

**Blocage progressif** (cahier-des-charges.md §4.2 : "lecture seule, puis blocage") : piloté
par `Tenant.status` (`TRIAL → ACTIVE → READ_ONLY → SUSPENDED`/`CANCELLED`, déjà défini par
ADR-001), pas par un statut séparé — `SubscriptionStatus` (`TRIALING/ACTIVE/PAST_DUE/
CANCELED`) reste l'état "facturation" côté Stripe. `TenantAccessInterceptor` (MVC, après
`TenantContextInterceptor`) bloque les écritures en `READ_ONLY` et tout en `SUSPENDED`/
`CANCELLED`, sauf `/api/v1/auth/**`, `/api/v1/billing/**` et `/api/v1/tenants/register` (pour
pouvoir encore se connecter et payer). La transition elle-même est faite par
`TenantAccessLifecycleJob`, un job planifié horaire (pas un webhook) : la bascule "fin
d'essai sans carte" n'a par nature aucun événement Stripe correspondant. Délais de grâce
(`app.billing.past-due-grace-days` = 3, `read-only-grace-days` = 7) choisis comme point de
départ raisonnable, **à confirmer avec le porteur de projet** — même traitement que la
grille tarifaire de V4.

**Hors périmètre 1.4 (explicitement différé, pas oublié)** : périodicité annuelle (le prix
souscrit via Stripe Checkout est implicitement mensuel, pas de colonne `billing_cycle`),
changement de plan avec proratisation, moyens de paiement locaux mobile money (voir ADR-003),
back-office Super-Admin pour gérer `plans`/`stripe_price_id` (`/api/admin/plans`, Phase 2).

### ADR-010 — Élèves, parents, enseignants, classes, matières (décidé, Phase 1.5)
**Décision** : packages `student`, `parent`, `teacher`, `schoolclass`, `subject` (le domaine
`parent` du cahier-des-charges.md §26 n'était pas listé dans la liste initiale de CLAUDE.md —
liste manifestement non exhaustive, corrigée à l'usage). Toutes les nouvelles entités sont
des `TenantScopedEntity` classiques (filtre Hibernate + RLS, voir ADR-001) — pas d'exception.

**Simplifications assumées pour ce MVP** (aucune n'est dans les 4 tâches de ROADMAP.md 1.5,
ajoutables plus tard sans réécriture) :
- **Pas d'année scolaire** (`academic_years`, pourtant listée dans docs/DATA_MODEL.md/cahier
  §6) : une seule génération "courante" de classes/élèves par établissement. À introduire
  quand la promotion/rollover d'année deviendra un besoin réel (Phase 2+).
- **`Teacher`/`Parent` sont des fiches métier, PAS des comptes `User`** : contrairement à
  `Student`/`Parent` cités comme rôles de connexion au cahier-des-charges.md §5, aucun compte
  de connexion n'est provisionné avec ces fiches (le portail parent/élève n'existe pas
  encore). Un Admin crée séparément un compte `User` (Role.PARENT/STUDENT) s'il faut un accès
  portail — non connecté à la fiche métier pour l'instant, voir "hors périmètre" ci-dessous.
- **Un seul enseignant par couple (classe, matière)** (`class_subject_assignments`, contrainte
  UNIQUE) — pas de co-intervention.
- **Import CSV élèves** : parseur volontairement simple (split virgule, pas de support des
  guillemets/virgules dans un champ) — suffisant pour un export tableur standard ; colonnes
  `studentNumber,firstName,lastName,birthDate,gender,className`, `className` résolu par
  correspondance exacte insensible à la casse sur le nom de classe existant (ignoré si non
  trouvé, l'élève est importé sans classe plutôt que rejeté).
- **Autorisations** : endpoints réservés aux rôles "staff" (ADMIN/DIRECTION/SECRETARY selon
  le module) — aucun accès `STUDENT`/`PARENT` (portail non construit, voir point précédent).

**Hors périmètre 1.5 (explicitement différé)** : portail élève/parent (lier `User` à
`Student`/`Parent`), photo/dossier administratif/pièces justificatives (module `document/`,
Phase 2), historique des changements de classe, permissions granulaires par enseignant sur
"ses" classes (un enseignant avec accès à un module a accès à tout le tenant pour l'instant).

### ADR-011 — Emploi du temps (décidé, Phase 1.6)
**Décision** : un seul entité `TimetableEntry` (package `timetable`, avec `Room`) représente
un créneau récurrent hebdomadaire — `schoolClassId`, `subjectId`, `teacherId`, `roomId`,
`dayOfWeek` (`java.time.DayOfWeek`), `startTime`/`endTime`. Pas de distinction séparée
"timetables"/"courses" comme suggéré par docs/DATA_MODEL.md : ROADMAP.md 1.6 ne demande
qu'"emploi du temps" comme une seule fonctionnalité CRUD.

**Détection de conflits** : à la création/modification, trois vérifications indépendantes
(même jour + chevauchement horaire) sur l'enseignant, la salle et la classe — première
correspondance trouvée renvoyée comme `409` (`TEACHER_ALREADY_BOOKED`/`ROOM_ALREADY_BOOKED`/
`CLASS_ALREADY_BOOKED`). Chevauchement calculé en mémoire sur le sous-ensemble déjà filtré par
jour+ressource (peu de lignes par jour en pratique), pas de requête SQL avec conditions
d'intervalle — suffisant pour le volume attendu, à revisiter seulement si la volumétrie le
justifie.

**Hors périmètre 1.6 (explicitement différé)** : pas de date de début/fin de validité ni
d'exceptions ponctuelles ("annulation exceptionnelle", "remplacement d'enseignant" du cahier
§9) — un créneau s'applique toutes les semaines sans interruption. Vues journalière/
hebdomadaire/par classe/par enseignant couvertes par un simple filtre query param
(`schoolClassId`/`teacherId`) sur la liste, pas des endpoints dédiés.

### ADR-012 — Absences et notification parent (décidé, Phase 1.7)
**Décision** : `AttendanceRecord` (package `attendance`) est scopé **par jour**, pas par
créneau/cours (`UNIQUE(student_id, date)`) — un seul statut par élève et par jour. Feuille
d'appel = `POST /api/v1/attendance/roll-call` (tout un `schoolClassId` + `date` + liste
d'élèves en une seule requête, pensé pour l'usage mobile "rapide" du cahier §10), upsert par
élève. Historique des modifications : `AttendanceRecordChange` capture un instantané de
l'état PRÉCÉDENT à chaque `update`, consultable via `GET /api/v1/attendance/{id}/history`.

**Notification au parent** : `ParentNotificationGateway` (interface, même pattern que
`StripeGateway` pour la facturation) est appelée dès qu'un enregistrement passe à un statut
≠ `PRESENT`. Implémentation par défaut `LoggingParentNotificationGateway` : trace
l'intention en log, **n'envoie rien réellement** — l'envoi FCM (ROADMAP.md Phase 2 §16)
n'est pas encore câblé (ni Redis ni FCM ne sont intégrés au backend à ce stade). Le champ
`parent_notified` empêche une re-notification en boucle sur des mises à jour ultérieures du
même enregistrement (limite connue : après une première notification, une correction
ultérieure ne redéclenche pas d'alerte même si le statut redevient "non présent" après un
passage par `PRESENT` — acceptable pour ce MVP).

**Hors périmètre 1.7 (explicitement différé)** : statistiques d'absences par élève/classe/
période (cahier §10 — couvert par le tableau de bord de ROADMAP.md 1.9), suivi par créneau/
cours plutôt que par jour, règles de notification configurables par établissement (une seule
règle fixe : notifier à chaque absence/retard/départ anticipé).

### ADR-013 — Notes et moyennes (décidé, Phase 1.8)
**Décision** : `Exam` (évaluation : classe, matière, barème `maxScore`, `coefficient`, date)
et `Grade` (note d'un élève, `UNIQUE(exam_id, student_id)`), package `grade`. `score` NULL
représente explicitement une absence à l'évaluation (colonne `absent` séparée, cahier
§11 "gestion des absences aux évaluations") — jamais une note de zéro.

**Calcul des moyennes**, toujours normalisé sur 20 (`score / maxScore * 20`) pour rendre les
évaluations à barèmes différents comparables :
- Statistiques d'une évaluation (`GET /api/v1/exams/{id}/statistics`) : moyenne/min/max sur
  les notes non-absentes uniquement.
- Moyenne d'un élève dans une matière (`GET /api/v1/students/{id}/subjects/{id}/average`) :
  pondérée par le `coefficient` de chaque évaluation de cette matière.
- Moyenne de classe dans une matière (`GET /api/v1/classes/{id}/subjects/{id}/average`) :
  moyenne des moyennes-élèves (uniquement les élèves ayant au moins une note) — pas une
  moyenne pondérée par élève sur l'ensemble des notes brutes, pour que chaque élève compte
  pour un poids égal indépendamment de son nombre d'évaluations passées.

**Colonnes `DOUBLE PRECISION`** (pas `NUMERIC`) pour `max_score`/`score` : nécessaire pour
correspondre au mapping Hibernate par défaut d'un champ Java `double`/`Double`
(`ddl-auto: validate` échoue sinon au démarrage — piège découvert à l'exécution des tests).

**Hors périmètre 1.8 (explicitement différé)** : import de notes en masse (CSV, comme pour
les élèves — seule la saisie via API/JSON est couverte), historisation des notes (contrairement
aux absences, pas de table de changements ici — non listée explicitement dans ROADMAP.md 1.8),
`grade_items` (sous-questions/barème détaillé de docs/DATA_MODEL.md), rang de classe.

### ADR-014 — Dashboard établissement (décidé, Phase 1.9 — fin de la Phase 1)
**Décision** : un seul endpoint `GET /api/v1/dashboard/summary` (package `statistics`),
paramètres `from`/`to` optionnels (30 derniers jours par défaut) pour le taux de présence.
Trois métriques seulement, conformes à l'unique bullet de ROADMAP.md 1.9 ("effectifs, taux
de présence, moyennes") : effectifs (élèves/enseignants actifs, classes), taux de présence
(% de statuts `PRESENT` sur la période), moyenne générale (toutes notes non-absentes,
normalisées sur 20, tenant entier). Pas de ventilation par classe/matière ici — déjà
disponible via les endpoints du module `grade` (ADR-013) ; le dashboard n'agrège qu'un
chiffre global de chaque métrique.

**Hors périmètre 1.9 (explicitement différé)** : évolution des résultats dans le temps,
indicateurs de réussite, rapports exportables (cahier §18 — Phase 2), tableau de bord
Super-Admin (nombre de tenants/MRR/churn — ROADMAP.md Phase 2).

**Note de fin de Phase 1** : les 9 sous-phases de ROADMAP.md 1.1 à 1.9 sont cochées — API
backend complète et testée. Le critère de sortie de Phase 1 tel que formulé dans
ROADMAP.md implique cependant une utilisation de bout en bout (donc un frontend Web/Mobile
fonctionnel pour classes/élèves/enseignants/emploi du temps/absences/notes/dashboard), qui
n'a pas été construit dans cette session (seules les pages d'inscription/branding
existent) — voir la note ajoutée directement sous le critère de sortie dans ROADMAP.md.

---

## Points ouverts (à trancher avant d'y arriver, pas maintenant)
- Fournisseur mobile money ? (avant Phase 3)
- Hébergement de production (cloud choisi, région) ? (avant le premier déploiement staging)
- Kubernetes ou déploiement simple Docker Compose au démarrage ? (avant Phase 4, ou avant
  si le nombre de tenants grossit plus vite que prévu)
- Anti-abus sur l'inscription self-service (captcha, rate limiting Redis — voir
  cahier-des-charges.md §20.1) : Redis n'est pour l'instant câblé que dans
  `docker-compose.yml`, pas encore intégré au backend Spring Boot. À faire avant l'ouverture
  publique de `/api/v1/tenants/register` en dehors d'un cadre de démo/dev.
