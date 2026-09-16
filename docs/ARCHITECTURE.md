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

**Note de fin de Phase 1 (backend)** : les 9 sous-phases de ROADMAP.md 1.1 à 1.9 sont
cochées côté API. Le frontend Web correspondant a été construit dans la session suivante —
voir ADR-015 (le critère de sortie complet de Phase 1, avec sa nuance frontend, est
documenté directement dans ROADMAP.md, sous le critère de sortie).

### ADR-015 — Frontend Web des modules 1.5-1.9, et deux bugs réels trouvés en le vérifiant (décidé)
**Décision** : shell authentifié Angular Material (sidenav filtré par rôle, voir
`web/src/app/shell/`), un service + une paire liste/dialog par ressource simple (Teacher/
Subject/Room/SchoolClass/Parent), écrans dédiés pour les flux non triviaux (import CSV élèves
en 2 étapes — pas 3, voir ci-dessous —, feuille d'appel, saisie de notes façon mockup #2,
dashboard façon mockup #5). `AuthTokenService`/`authInterceptor`/`authGuard` ajoutés dans
`core/` — jusqu'ici aucun écran n'avait besoin d'appeler l'API de façon authentifiée.

**Écarts assumés par rapport aux mockups de docs/MOCKUPS.md** :
- **Import élèves (§7)** : 2 étapes ("Déposer le fichier" → "Résultat"), pas 3. Le backend
  importe en une seule opération synchrone (`POST /api/v1/students/import`,
  ROADMAP.md 1.5) — il n'existe pas de phase "prévisualiser sans committer" côté API ; une
  étape "Confirmer" après coup aurait menti sur la possibilité d'annuler.
- **Feuille d'appel (§1)** : prévue Mobile/Ionic, livrée en Web — l'app Mobile n'a aucune
  authentification construite (seul le branding l'est, Phase 0), donc un écran Ionic
  serait inutilisable en pratique tant que ce prérequis n'existe pas.

**Bug 1 — RLS violée à l'inscription avec le rôle applicatif réel** (trouvé en testant le
frontend contre `docker-compose` + le rôle restreint, jamais avec Testcontainers en
superutilisateur qui contourne toujours RLS, ADR-001 Piège 4) :
`TenantRegistrationService#register` créait le compte Administrateur (`INSERT INTO users`,
`GenerationType.IDENTITY` ⇒ INSERT immédiat) **avant** d'appeler
`TenantSessionConfigurer#applyTenant` (qui pose `app.tenant_id`). La politique RLS sur
`users` n'a pas de clause `WITH CHECK` explicite — Postgres réutilise alors la clause
`USING` comme condition d'insertion, qui échoue tant que `app.tenant_id` n'est pas positionné
(`current_setting(..., true)` renvoie NULL). Corrigé en posant le contexte tenant AVANT le
premier INSERT, pas seulement avant `login()`. Test de régression :
`TenantRegistrationRlsTest`, qui recrée le rôle applicatif restreint réel (comme
`01-create-app-role.sh`) et échoue bien sans le correctif — piège de test rencontré en
l'écrivant : `@ServiceConnection` (sur `AbstractIntegrationTest`) enregistre un bean
`JdbcConnectionDetails` qui prime sur un simple `@DynamicPropertySource` positionnant
`spring.datasource.username/password` ; il faut fournir son propre bean
`JdbcConnectionDetails` (`@Primary`) pour vraiment changer le rôle de connexion runtime.

**Bug 2 — 500 au lieu de 404 pour un endpoint inexistant, sans aucun log serveur** : une
route API non mappée tombe sur `NoResourceFoundException` (repli ressources statiques de
Spring), que `GlobalExceptionHandler` avalait dans son handler générique `Exception.class`
(500 "INTERNAL_ERROR") — et ce handler générique ne loggait rien du tout, rendant le
diagnostic invisible côté serveur. Corrigé par un `@ExceptionHandler(NoResourceFoundException.class)`
dédié (404) et un `log.error(...)` dans le handler générique. Découvert car le frontend
(`TenantBrandingService`, Phase 0) appelle `GET /api/v1/tenants/current/branding`, un
endpoint **qui n'a jamais été implémenté côté backend** — voir "Points ouverts" ci-dessous.

---

### ADR-016 — Bulletins scolaires (décidé, Phase 2.1)
**Décision** : `ReportCard` (bulletin) + `ReportCardEntry` (ligne matière, moyenne et
coefficient **figés** au moment de la génération — un changement ultérieur du coefficient
d'une matière ne doit pas modifier un bulletin déjà généré). `period_label` reste un texte
libre ("Trimestre 1"), pas une entité période/année scolaire — même simplification que le
reste du projet (ADR-010) ; `periodFrom`/`periodTo` bornent seulement le calcul des
absences/retards. `POST /api/v1/report-cards/generate` réutilise directement
`GradeService#studentSubjectAverage` (ADR-013) — aucun recalcul de moyenne dupliqué — et les
matières prises en compte sont celles affectées à la classe (`class_subject_assignments`,
ADR-010), pas "toute matière ayant une évaluation". Régénération idempotente : les lignes
matière existantes sont entièrement remplacées, pas fusionnées (une évaluation supprimée
depuis ne doit pas laisser une moyenne obsolète).

**Export PDF** : Apache PDFBox (Apache-2.0), mise en page simple à une page, pas de gabarit
configurable par établissement pour ce MVP.

**Piège rencontré** : `deleteAllByReportCardId` dérivé de Spring Data (qui charge les lignes
puis appelle `entityManager.remove()` une par une) laisse le DELETE en attente de flush,
alors que les nouvelles lignes (`GenerationType.IDENTITY`) s'insèrent immédiatement à la
régénération — violation de la contrainte `UNIQUE(report_card_id, subject_id)`. Corrigé avec
une requête JPQL `@Modifying` de suppression en masse (DELETE direct, pas de cycle de vie
d'entité). Sans risque d'isolation cross-tenant malgré le bulk DML (qui ne respecte pas le
filtre Hibernate) : le `reportCardId` utilisé dans le WHERE est toujours déjà validé
appartenir au tenant courant par l'appelant.

**Hors périmètre 2.1 (explicitement différé)** : rang de classe et signature électronique —
cahier §12 les décrit comme conditionnés à un paramétrage d'établissement ("si activé") qui
n'existe pas encore (cahier §6, pas construit).

**Refactor associé** : extraction de `com.schoolsaas.common.NumberUtils#round2` — le même
arrondi à 2 décimales était dupliqué à l'identique dans `GradeService` et `DashboardService`
avant l'ajout de ce troisième usage dans `ReportCardService`.

### ADR-017 — Bibliothèque de documents (décidé, Phase 2.2)
**Décision** : `Document` (portée `SUBJECT`/`CLASS`/`SERVICE`, exactement une des trois
selon une contrainte CHECK) + `DocumentVisibleRole` (droits de consultation — aucune ligne =
visible par tout rôle staff du tenant, simplification MVP plutôt qu'une matrice de
permissions complète). Stockage derrière `StorageGateway` (même pattern que
`StripeGateway`/`ParentNotificationGateway`) : `LocalDiskStorageGateway` par défaut (dev/MVP,
pas de credentials S3) — brancher S3 reste un remplacement d'implémentation, pas un
changement d'appelants. `storage_key` est une clé opaque (UUID + nom assaini), jamais un
chemin de fichier exposé côté API ; protection anti-traversée de répertoire dans
`LocalDiskStorageGateway#resolveWithinRoot` (chemins normalisés + absolutisés avant
comparaison — piège rencontré : comparer un chemin racine non normalisé à un chemin résolu
normalisé peut échouer même pour une clé légitime).

**Suppression** = archivage (`archived = true`, sort des listes), pas de suppression
physique pour ce MVP — cahier §14 parle de "suppression contrôlée", cohérent avec un
archivage réversible plutôt qu'une perte de données irréversible.

**Hors périmètre 2.2 (explicitement différé)** : quota de stockage par tenant selon le plan
souscrit (cahier §14) — `Plan` n'a pas de champ de quota, ajouter cette contrainte suppose
d'abord d'étendre le modèle de facturation (ADR-009), pas fait ici pour ne pas mélanger les
deux sujets.

### ADR-018 — Cahier de textes et devoirs (décidé, Phase 2.3)
**Décision** : une seule entité `Lesson` (package `homework`) regroupe contenu du cours ET
travail à faire — pas deux entités séparées "cours"/"devoir" : c'est ainsi qu'un vrai cahier
de textes fonctionne (une séance a un contenu, et *éventuellement* un devoir associé), et le
cahier-des-charges §13 les décrit comme un seul flux ("saisie du contenu... ajout du travail
à faire..."). Pièce jointe = simple `attachmentDocumentId` référençant le module `document`
(ADR-017), pas de gestion de pièces jointes dupliquée. Notification nouveau devoir : nouvelle
`HomeworkNotificationGateway`, même pattern que `ParentNotificationGateway` (ADR-012) — pas
consolidée avec elle ici, la généralisation est explicitement le travail de ROADMAP.md 2.5.

**Hors périmètre 2.3** : consultation élèves/parents (aucun portail construit, ADR-010).

### ADR-019 — Messagerie interne (décidé, Phase 2.4)
**Décision** : `Conversation` + `ConversationParticipant` (porte la confirmation de lecture
via `lastReadAt` — tous les messages antérieurs à cette date sont "lus", pas un accusé par
message individuel) + `Message`. Une **annonce** est une conversation comme une autre
(`is_announcement = true`), diffusée à la création à tous les utilisateurs actifs du tenant
— pas un mécanisme de diffusion séparé. Individuel vs groupe = juste le nombre de
participants, pas deux concepts différents. Accès contrôlé par appartenance
(`ConversationParticipant`) en plus du filtre tenant (RLS/Hibernate) : un utilisateur du même
tenant mais non participant reçoit `403 NOT_A_PARTICIPANT`, pas `404` (la conversation existe
bel et bien dans son tenant, contrairement à une fuite cross-tenant). Création d'annonce
réservée à ADMIN/DIRECTION. Recherche : simple `LIKE` insensible à la casse sur le contenu,
pas de moteur de recherche plein texte pour ce MVP.

**Troisième gateway de notification "log seulement"** (`MessageNotificationGateway`, après
`ParentNotificationGateway` et `HomeworkNotificationGateway`) — la consolidation en un
registre unique reste le travail explicite de ROADMAP.md 2.5.

### ADR-020 — Registre centralisé de notifications (décidé, Phase 2.5)
**Décision** : les trois gateways ad hoc (`ParentNotificationGateway` ADR-012,
`HomeworkNotificationGateway` ADR-018, `MessageNotificationGateway` ADR-019) sont supprimées
et remplacées par un unique package `notification` : `NotificationType` (liste fermée des
événements notifiables — `NEW_GRADE`, `ABSENCE`, `NEW_HOMEWORK`, `NEW_DOCUMENT`,
`NEW_MESSAGE`, `ANNOUNCEMENT`, `SUBSCRIPTION_ALERT`), `NotificationDispatcher` (point d'appel
unique pour tous les modules), `NotificationGateway`/`LoggingNotificationGateway` (même
pattern gateway que `StripeGateway` — brancher FCM reste un remplacement d'implémentation).
`NotificationDispatcher` applique les préférences utilisateur (`NotificationPreference`,
table `notification_preferences`, `school_id` + RLS comme toute table métier) avant d'appeler
la gateway : absence de ligne pour un (utilisateur, type) = activé par défaut. Endpoint
`GET/PUT /api/v1/notification-preferences` permet à chaque utilisateur de gérer ses propres
préférences (pas de paramétrage par rôle pour ce MVP — plus simple à raisonner avec le modèle
`User` existant).

**Événements sans compte utilisateur réel** (`ABSENCE`, `NEW_HOMEWORK`) : élèves et parents
n'ont pas de compte (ADR-010), donc `recipientUserIds` est vide et le filtrage par préférence
est un no-op — l'implémentation par défaut loggue simplement le contenu, comportement
inchangé par rapport aux gateways précédentes. Seule différence de comportement observable :
l'envoi d'un message dans une conversation-annonce déclenche désormais `ANNOUNCEMENT` plutôt
que `NEW_MESSAGE` (types distincts, permettant un opt-out séparé).

**Hors périmètre 2.5 (explicitement différé)** : intégration Firebase Cloud Messaging réelle
— aucun credential FCM disponible dans cet environnement, voir "Points ouverts" ci-dessous ;
`NEW_GRADE`/`NEW_DOCUMENT`/`SUBSCRIPTION_ALERT` ne sont émis par aucun appelant pour l'instant
(le type existe dans `NotificationType` et le registre est prêt à les recevoir, mais
`GradeService`/`DocumentService`/`TenantAccessLifecycleJob` ne les déclenchent pas encore —
ne pas anticiper leur contenu métier exact, non demandé dans cette tâche).

### ADR-021 — Dashboard Super-Admin (décidé, Phase 2.6)
**Décision** : `PlatformDashboardService`/`PlatformDashboardController` (package
`statistics`, distinct de `DashboardService` qui reste le tableau de bord *établissement*) —
endpoint `GET /api/v1/admin/dashboard/summary`, `@PreAuthorize("hasRole('SUPER_ADMIN')")`
(même mécanisme d'authentification que `AdminAuthController`/`PlatformAdmin`, ADR-008).
Compte les tenants par `TenantStatus` (TRIAL/ACTIVE/READ_ONLY/SUSPENDED/CANCELLED). MRR =
somme de `Plan.priceCents` pour chaque `Subscription` au statut `ACTIVE` ; ARR = MRR × 12 ;
`currency` prise sur le premier plan actif rencontré (tous les plans du catalogue sont en
XOF pour l'instant, voir ADR-009 — pas de gestion multi-devises simultanée pour ce MVP).

**Churn/conversion = taux cumulés, pas des cohortes par période** : `churnRate` =
abonnements `CANCELED` / total des abonnements jamais créés ; `conversionRate` = abonnements
`ACTIVE` / total. Aucune table d'historique d'événements d'abonnement n'existe pour calculer
un taux "sur les 30 derniers jours" ou par cohorte de trial — construire cet historique est
un sujet à part (event sourcing ou table d'audit dédiée), non demandé dans cette tâche et
disproportionné pour un MVP à ce stade. Documenté explicitement ici pour qu'un futur lecteur
ne prenne pas ces taux pour des cohortes temporelles.

**Hors périmètre 2.6** : ventilation temporelle (évolution MRR mois par mois), export, et
tout graphique — seuls les agrégats bruts demandés par le cahier des charges §5/§18 sont
exposés ; l'écran Web réservé au Super-Administrateur consommant cet endpoint n'est pas
construit dans cette tâche (aucun frontend Web Super-Admin n'existe encore, seul le shell
Angular établissement de la Phase 1 a été construit) — à faire dans une tâche frontend
séparée, comme pour toute l'API backend de la Phase 2.

### ADR-022 — Vie scolaire (décidé, Phase 3.1)
**Décision** : nouveau package `discipline` (cité nommément dans CLAUDE.md, jamais construit
avant cette tâche) : `Incident` (rattaché à une classe, plusieurs élèves possibles via
`IncidentStudent` — même pattern many-to-many que `ConversationParticipant`, ADR-019),
`Sanction` (toujours rattachée à un incident ET à un élève précis parmi ceux déclarés
concernés — validé côté service, `400 STUDENT_NOT_IN_INCIDENT` sinon), `Convocation`
(élève et/ou parent, statut `SCHEDULED`/`DONE`/`CANCELLED`/`NO_SHOW`), `Observation`
(positive/négative, indépendante d'un incident). "Exclusions" (cahier §17, bullet séparé de
"Sanctions et punitions") n'est PAS une entité à part : c'est `SanctionType.EXPULSION`, un
type de sanction comme un autre — les deux bullets du cahier décrivent le même concept
fonctionnel à deux niveaux de sévérité, pas deux workflows différents.

**RBAC différencié par acte** (le rôle `VIE_SCOLAIRE` existe depuis l'ADR-008 mais n'était
utilisé par aucun endpoint avant cette tâche) : `TEACHER` peut déclarer un incident ou une
observation (il est souvent le témoin direct), mais seuls `ADMIN`/`DIRECTION`/`VIE_SCOLAIRE`
peuvent décider une sanction ou une convocation — décision disciplinaire réservée à
l'autorité de l'établissement, pas à qui a constaté les faits.

**"Historique disciplinaire par élève"** (cahier §17) = vue agrégée
(`StudentDisciplineHistoryResponse` : sanctions + observations + convocations triées), pas
une nouvelle entité — les incidents eux-mêmes n'y figurent pas directement (ils sont
consultés par classe/période via `GET /discipline/incidents`, un élève y apparaît via
`IncidentStudent`). **Statistiques de vie scolaire** = agrégat par classe et période
(nombre d'incidents par sévérité, sanctions par type), pas de ventilation par élève ni
d'export — cohérent avec le niveau d'agrégation de `DashboardService` (ADR-014).

**Hors périmètre 3.1** : notification automatique aux parents lors d'une sanction/convocation
— aucun compte parent (ADR-010), le registre de notifications (ADR-020) n'est pas branché
ici, non demandé dans cette tâche (contrairement aux absences, où la notification est un
point du cahier §10 explicitement demandé en 1.7).

### ADR-023 — Statistiques avancées (décidé, Phase 3.2)
**Décision** : `AdvancedStatisticsService`/`AdvancedStatisticsController` (package
`statistics`) ajoutent `GET /api/v1/statistics/advanced/results-evolution[.csv]` — moyenne
normalisée sur 20 regroupée par **mois calendaire** de la date d'évaluation (`Exam.examDate`),
pour une classe entière ou une seule matière. Regroupement par mois calendaire, pas par
trimestre/période pédagogique : aucune notion de période paramétrable n'existe (ADR-010), et
en créer une pour ce seul indicateur serait anticiper un futur module de paramétrage
établissement (cahier §6) non demandé ici. Export CSV = seul format construit pour cette
passe (`period,average,gradeCount`) ; PDF/autres rapports "exportables" du cahier §18 restent
hors périmètre, comme le cahier ne liste pas de format précis.

**"Usage global" du dashboard Super-Admin (cahier §18)** : nouvelle table plateforme
`notification_log` (comme `subscriptions`/`invoices` — pas de `school_id` RLS-protégé,
`tenant_id` simple colonne nullable) alimentée par `NotificationDispatcher` (ADR-020) à
chaque envoi réel ; exposée comme `notificationsSentCount` dans
`PlatformDashboardSummaryResponse` (ADR-021). **Stockage utilisé** et **utilisateurs actifs**
ne sont PAS implémentés dans cette passe : `documents.size_bytes` et `users` sont des tables
métier RLS-protégées par tenant (à raison, CLAUDE.md règle 1) — en obtenir un total agrégé
*à travers tous les tenants* pour le Super-Admin demande soit une table de compteurs
plateforme dénormalisée mise à jour par l'application (comme `notification_log`), soit un
rôle Postgres avec `BYPASSRLS` restreint à des requêtes d'agrégat en lecture seule. Les deux
options changent une invariante du projet martelée depuis l'ADR-001/`TenantRegistrationRlsTest`
("le rôle applicatif ne contourne jamais RLS") ou ajoutent un mécanisme de synchronisation
nouveau : décision à trancher explicitement avec l'utilisateur, pas silencieusement (voir
"Points ouverts").

### ADR-024 — Comptabilité et frais scolaires (décidé, Phase 3.3)
**Décision** : nouveau package `schoolfees` (nom non listé au cahier §26, qui ne nomme pas ce
module — `billing` était déjà pris par la facturation SaaS plateforme, un nom différent évite
de mélanger deux domaines de facturation distincts : revenu de l'éditeur vs. frais payés par
les familles). `FeeSchedule` (grille tarifaire, rattachée à une classe — **pas** de concept
de "niveau" séparé de la classe, aucun n'existe ailleurs dans le modèle, voir ADR-010 ; une
ligne par échéance porte l'échéancier via son propre `dueDate`) → `StudentFeeInvoice` (générée
en masse pour les élèves actifs d'une classe, idempotent) → `FeePayment` (paiement partiel ou
total, saisi manuellement, statut de la facture recalculé automatiquement :
PENDING→PARTIALLY_PAID→PAID). Reporting consolidé par classe/période (total dû/payé/impayé +
liste des factures impayées) pour Direction/Comptable.

**Paiement mobile money (cahier §4.3) : questionné à l'utilisateur, réponse "pas encore,
décision à prendre plus tard"** — `FeePaymentMethod.MOBILE_MONEY` existe pour tracer un
paiement reçu hors-ligne via ce canal (saisie manuelle par le personnel, avec référence libre,
ex. identifiant de transaction Orange Money), mais **aucun fournisseur n'est intégré** :
pas de paiement en ligne réel déclenché depuis l'app pour les frais de scolarité. Rester
distinct de `StripeGateway` (ADR-009), qui ne concerne que l'abonnement SaaS de l'établissement
à la plateforme, jamais les frais que les familles paient à l'établissement.

### ADR-025 — Cantine (décidé, Phase 3.4)
**Décision** : nouveau package `canteen` (nom du cahier §26). `Menu` (un par jour, variante
"régime particulier" comme un champ optionnel du même menu — pas de profil allergène par
élève, donnée de santé hors périmètre) ; `MealReservation` (une par élève et par jour,
`UNIQUE(student_id, date)`) ; `CanteenInvoice` (générée à la demande à partir du nombre de
réservations sur une période × un prix par repas donné au moment de la génération — pas
d'abonnement forfaitaire ni de grille de prix persistée, cahier §19.1 dit "facturation liée à
la consommation réelle", donc calculée, pas un tarif fixe) ; `CanteenPayment` (même mécanique
que `FeePayment`/ADR-024 : paiement partiel/total, statut recalculé automatiquement, réutilise
`FeePaymentMethod` du package `schoolfees` plutôt que de dupliquer un enum identique).

**"Réservation par les parents" (cahier §19.1) → questionné à l'utilisateur, réponse "saisie
côté staff pour cette passe"** : `reservedByUserId` sur `MealReservation` porte toujours un
compte staff (`ADMIN`/`DIRECTION`/`SECRETARY`), jamais un compte parent — cohérent avec
l'absence de portail parent (ADR-010). Le vrai portail parent, s'il est construit un jour,
resterait un appelant supplémentaire du même `CanteenService.reserveMeal`, pas une réécriture.

**Suivi des impayés** (cahier §19.1) = `GET /canteen/invoices/unpaid`, toutes factures non
soldées/non annulées tous élèves confondus — vue à plat pour ce MVP, pas de regroupement par
classe/famille (pas demandé, cohérent avec le niveau d'agrégation des autres modules de cette
phase).

### ADR-026 — Bibliothèque (décidé, Phase 3.5)
**Décision** : nouveau package `library` (nom du cahier §26). `Book` (`barcode` = clé de scan
obligatoire, `isbn` optionnel) ; `BookLoan` (`returnedAt IS NULL` = actif, "en retard" calculé
à la lecture — `dueDate < aujourd'hui && actif` — pas un statut stocké séparément, même
principe que `AttendanceRecord`) ; `BookReservation` (liste d'attente FIFO = simplement l'ordre
de `reservedAt`, pas de colonne de position stockée). Retourner un ouvrage libère
automatiquement la réservation la plus ancienne en attente (`FULFILLED`), sans notification —
le cahier ne demande de notification que pour les retards, pas pour la disponibilité.

**"Relances automatiques" (cahier §19.3) = `LibraryOverdueReminderJob`**, nouveau
`@Scheduled` (quotidien, 08h) qui réutilise le registre centralisé (`NotificationDispatcher`,
ADR-020) — nouveau `NotificationType.LIBRARY_OVERDUE`. Aucun compte élève/parent (ADR-010) :
comme pour les absences/devoirs, `recipientUserIds` est vide, la relance est juste loggée.
**Premier job de ce projet à devoir interroger une table métier RLS-protégée pour TOUS les
tenants** (contrairement à `TenantAccessLifecycleJob`, ADR-009, qui ne touche que
`subscriptions`/`tenants`, des entités plateforme sans RLS) : résolu en itérant tenant par
tenant, en activant explicitement `TenantContext`/`TenantSessionConfigurer` pour chacun avant
de l'interroger — exactement ce qu'une requête HTTP normale fait, jamais de contournement RLS.
C'est la réponse concrète à la question laissée ouverte par l'ADR-023 : itérer par tenant
plutôt que `BYPASSRLS`, dès qu'un vrai job (pas juste un dashboard de lecture) en a besoin.

### ADR-027 — Transport scolaire (décidé, Phase 3.6)
**Décision** : nouveau package `transport` (nom du cahier §26). `BusRoute` + `BusStop`
(ordonnés par `sequenceOrder`) ; `StudentTransportAssignment` (une seule affectation active
par élève, `UNIQUE(student_id)`, même principe que `school_class_id` sur `Student`) ;
`TransportInvoice`/`TransportPayment` — forfait périodique (le cahier §19.2 dit juste
"facturation du service", pas "à la consommation réelle" comme la cantine §19.1, donc un
montant donné à la génération plutôt qu'un comptage d'usage). Générer une facture exige que
l'élève ait une affectation active (`404 ASSIGNMENT_NOT_FOUND` sinon) : on ne facture pas un
service auquel l'élève n'est pas inscrit.

**Troisième duplication quasi identique du couple Invoice/Payment** (après
`FeeSchedule`/`StudentFeeInvoice`/`FeePayment` en 3.3, `CanteenInvoice`/`CanteenPayment` en
3.4) — **pas d'abstraction générique extraite malgré ce précédent** (`NumberUtils`, extrait
après sa 3ᵉ duplication) : contrairement à une fonction de calcul pure, un "Invoice/Payment"
générique devrait couvrir trois relations différentes à la source du montant dû (une grille
tarifaire, un comptage de consommation, un forfait saisi à la main) — une factorisation
propre demanderait une conception à part (table polymorphe ou "billable reference"), plus
risquée à faire rétroactivement sur des modules déjà livrés et testés que ce que cette tâche
justifie. Réutilise seulement ce qui est trivial à partager sans risque : l'enum
`FeePaymentMethod` (package `schoolfees`). À reconsidérer explicitement si un 4ᵉ module de
facturation apparaît (cantine/scolarité/transport couvrent déjà les cas identifiés par le
cahier des charges).

### ADR-028 — Domaine personnalisé, branding et modèle de bulletin, feature flags par plan (Phase 3.7)
**Décision** : trois axes de personnalisation par établissement (cahier §2.3/§2.4/§4.1), tous
portés directement par `Tenant` (pas de table séparée — champs isolés, pas de relation) :
- Branding (`logoUrl`/`primaryColor`/`secondaryColor`) : `GET /api/v1/tenants/current/branding`
  est volontairement **public** (`SecurityConfig` + `TenantAccessInterceptor`) et renvoie
  `TenantBrandingResponse` **non enveloppé** dans `ApiResponse` (pas de `{"data": ...}`) — il
  doit rester compatible avec `web/src/app/branding/tenant-branding.model.ts`
  (`TenantBrandingService.init()` fait `http.get<TenantBranding>(...)`), appelé au démarrage
  avant toute connexion. Résout enfin le point ouvert laissé par ADR-015 (Phase 1). Écriture
  (`PUT`) réservée à `ADMIN`/`DIRECTION`.
- Modèle de bulletin (`reportCardHeader`/`reportCardLegalMentions`) : consommé par
  `ReportCardPdfExporter`, à défaut de configuration un en-tête générique (nom de
  l'établissement) et pas de mentions légales — comportement inchangé pour un tenant qui n'a
  jamais configuré son modèle.
- Domaine personnalisé (`customDomain`, colonne `UNIQUE`) : résolu par `TenantResolver` par
  correspondance exacte sur `request.getServerName()`, après l'en-tête `X-Tenant-Id` mais
  avant le sous-domaine — le provisioning DNS/certificat réel reste hors périmètre applicatif,
  seule la résolution une fois le domaine reçu l'est. Écriture gate-keepée par
  `PlanFeature.CUSTOM_DOMAIN` (plan Premium uniquement, `403 FEATURE_NOT_INCLUDED` sinon).

**Feature flags par plan** (`PlanFeature` : `CANTEEN`/`TRANSPORT`/`LIBRARY`/`CUSTOM_DOMAIN`,
colonnes booléennes sur `plans`, vérifiées par `PlanFeatureService.tenantHasFeature`) :
simplification assumée — "en option" (cahier §4.1, plan Standard) traité comme équivalent à
"inclus" dès que le tenant a un abonnement actif sur ce plan ; aucun mécanisme d'achat
d'option à la carte ni de dérogation individuelle par tenant (feature flag manuel, cahier
§2.5) n'existe. `PlanFeatureInterceptor` bloque tout le préfixe `/api/v1/{canteen,transport,
library}` (fail-closed : aucun abonnement = aucune fonctionnalité incluse), au même rang
d'exécution que `TenantAccessInterceptor` (après `TenantContextInterceptor`, voir
`TenantWebConfig`). Les tests des trois modules (Cantine/Transport/Bibliothèque, Phases
3.4-3.6) ont dû être mis à jour pour accorder explicitement le plan Premium via
`TestAuthSupport.grantAllPlanFeatures` — un tenant de test créé par
`TestAuthSupport.createActiveTenant` n'a par défaut aucun abonnement.

**Logique métier extraite dans `TenantSettingsService`** (pas directement dans
`TenantSettingsController`) pour suivre la convention du module (`TenantRegistrationService`,
`SubscriptionService`) : chaque mutation est `@Transactional` et appelle explicitement
`tenantRepository.save(tenant)` — piège découvert par `TenantSettingsTest` : muter les
setters d'une entité récupérée hors d'une transaction ne suffit pas à persister le
changement (pas de flush automatique sans démarcation transactionnelle), le contrôleur seul
renvoyait un résultat correct en mémoire mais rien n'était réellement écrit en base.

**Templates de notification/e-mail personnalisables** (dernier item de ROADMAP.md 3.7) :
nouvelle entité `NotificationTemplate` (school_id, `NotificationType`, `titleOverride`
nullable, `bodyTemplate` nullable avec placeholder obligatoire `{message}` si renseigné),
`UNIQUE(school_id, type)`. `NotificationDispatcher.dispatch(...)` reste appelé exactement
comme avant par chaque module (`AttendanceService`, `LessonService`, `MessagingService`,
`LibraryOverdueReminderJob`) — signature inchangée — mais résout désormais, juste avant
d'appeler `NotificationGateway`, un éventuel template pour le tenant courant : `titleOverride`
remplace le titre calculé par l'appelant, `bodyTemplate` l'enveloppe (le message calculé par
l'appelant vient remplacer `{message}`). Aucun template configuré pour un (tenant, type) =
comportement strictement inchangé (comme `NotificationPreference`, même principe de "ligne
absente = valeur par défaut"). CRUD réservé `ADMIN`/`DIRECTION` via
`GET/PUT /api/v1/tenants/current/notification-templates[/{type}]`
(`NotificationTemplateController`, dans le module `notification`, pas `tenant` : c'est un
paramétrage de notification, pas de branding visuel).

### ADR-029 — Login exige désormais le sous-domaine (bug RLS corrigé)
**Découvert** : en testant manuellement la Phase 3.7 en local contre un vrai backend
(rôle applicatif restreint, pas le superutilisateur Testcontainers), `POST /api/v1/auth/login`
échouait de façon reproductible après chaque redémarrage à froid du backend, mais réussissait
dès qu'un appel scopé à un tenant avait été fait juste avant sur la même connexion.

**Cause réelle** : `AuthService.login(email, password)` faisait une recherche **globale** par
e-mail (`UserRepository.findByEmail`) — nécessaire car l'e-mail n'est unique que par
établissement (cahier §21, `UNIQUE(school_id, email)` sur `users`), pas globalement, donc le
tenant n'est jamais connu à l'avance côté login. Mais `users` impose
`FORCE ROW LEVEL SECURITY` (`school_id = current_setting('app.tenant_id', true)::bigint`,
voir ADR-001) — sans avoir positionné `app.tenant_id` pour CETTE requête précise, le résultat
de `findByEmail` dépendait entièrement de la valeur laissée par une **requête précédente sur
la même connexion poolée** (HikariCP) : `TenantSessionConfigurer.applyTenant` pose
`app.tenant_id` au niveau de la session Postgres (`set_config(..., false)`, pas de la
transaction), et rien ne le réinitialise quand une requête ne résout aucun tenant. En
production, ça ne se voyait pas pour un Web servi par sous-domaine (`ecole.schoolsaas.example`
résout le tenant de la requête de login elle-même, avant le contrôleur) — mais ça restait
cassé pour le Mobile, qui tape une URL absolue fixe sans sous-domaine par tenant (voir
README.md "Environnements"), et pour tout dev local (`localhost` n'a pas de sous-domaine).
Le même piège existait dans `AuthService.resolvePrincipal` (chemin `/auth/refresh`,
recherche par `findById` — RLS s'applique même à une recherche par clé primaire).

**Options considérées** : (a) e-mail unique globalement — rejeté, casse le choix déjà fait
au cahier §21 (un utilisateur peut avoir un compte dans deux établissements) ; (b) ne rien
changer, contourner en dev — laisse le bug réel pour le Mobile en production ; (c) **ajouter
un champ sous-domaine/établissement au formulaire de connexion** (retenu, décision
utilisateur) — le login résout et applique explicitement le tenant (même pattern que
`TenantRegistrationService#register`, `TenantContext.set(...)` + `applyTenant(...)` dans un
`try/finally`) AVANT toute recherche dans `users`, déterministe indépendamment de l'état
laissé par une requête précédente sur la connexion.

**Implémentation** : nouveau DTO `TenantLoginRequest(subdomain, email, password)` — séparé du
`LoginRequest` du Super-Admin (`AdminAuthController`), qui n'a pas de tenant et ne doit pas en
exiger un. `AuthService.login(subdomain, email, password)` et `resolvePrincipal` (branche
`USER`, via `RefreshToken#tenantId` déjà connu, pas besoin d'un nouveau champ) appliquent
tous deux le tenant avant toute lecture de `users`. Web : champ "Sous-domaine" ajouté à
l'écran de connexion. Mobile : aucune authentification n'y est construite (Phase 0 seulement,
voir ROADMAP.md 1.5-1.9), donc rien à changer côté Mobile pour l'instant — mais la future
construction de l'auth Mobile devra prévoir ce champ dès le départ.

**Test de régression** : `AuthRlsTest` (nouveau, package `auth`) reproduit le rôle applicatif
restreint (même mécanisme que `TenantRegistrationRlsTest`) et vérifie login **puis** refresh
de bout en bout — invisible avec le superutilisateur Testcontainers par défaut (RLS toujours
contourné, ADR-001 Piège 4), donc un test dédié était nécessaire pour ne pas régresser
silencieusement.

## Points ouverts (à trancher avant d'y arriver, pas maintenant)
- **Fournisseur mobile money pour les frais de scolarité (ROADMAP.md 3.3, ADR-024)** — reste
  à trancher (réponse utilisateur : plus tard). Le modèle (`FeePaymentMethod.MOBILE_MONEY`)
  est prêt à recevoir un vrai fournisseur sans migration de schéma ; seule l'intégration
  d'un gateway de paiement en ligne (même pattern que `StripeGateway`) reste à construire.
- **Stockage utilisé et utilisateurs actifs cross-tenant (ROADMAP.md 3.2, ADR-023)** —
  nécessite soit des compteurs plateforme dénormalisés, soit un rôle DB `BYPASSRLS` restreint.
  Bloquant : à trancher avec l'utilisateur avant d'implémenter (touche à l'invariante RLS du
  projet), pas avant que le besoin business (dashboard Super-Admin) ne le justifie vraiment.
- **Écran Web Super-Admin non construit** — `GET /api/v1/admin/dashboard/summary` (ADR-021)
  existe côté backend et est testé, mais aucun frontend ne le consomme : le shell Angular
  actuel (Phase 1) est établi pour les rôles staff d'un tenant, pas pour `PlatformAdmin`
  (hors tenant). À construire comme une tâche frontend dédiée.
- **Firebase Cloud Messaging non câblé** — `LoggingNotificationGateway` (ADR-020) est la
  seule implémentation de `NotificationGateway` ; aucun credential FCM disponible dans cet
  environnement. Brancher FCM reste un remplacement d'implémentation, pas un changement
  d'appelants.
- **Quota de stockage documentaire par tenant** (cahier §14) — `Plan` n'a pas de champ de
  quota, `StorageGateway` (ADR-017) n'applique aucune limite. À trancher avant l'ouverture
  publique du module documents en dehors d'un cadre de démo/dev.
- ~~**`GET /api/v1/tenants/current/branding` n'existe pas côté backend**~~ — résolu par
  ADR-028 (Phase 3.7) : l'endpoint existe, colonnes `logo_url`/`primary_color`/
  `secondary_color` ajoutées à `tenants` (V50).
- Fournisseur mobile money ? (avant Phase 3)
- Hébergement de production (cloud choisi, région) ? (avant le premier déploiement staging)
- Kubernetes ou déploiement simple Docker Compose au démarrage ? (avant Phase 4, ou avant
  si le nombre de tenants grossit plus vite que prévu)
- Anti-abus sur l'inscription self-service (captcha, rate limiting Redis — voir
  cahier-des-charges.md §20.1) : Redis n'est pour l'instant câblé que dans
  `docker-compose.yml`, pas encore intégré au backend Spring Boot. À faire avant l'ouverture
  publique de `/api/v1/tenants/register` en dehors d'un cadre de démo/dev.
