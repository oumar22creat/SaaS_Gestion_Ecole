# SESSION LOG

Historique des sessions de travail. À compléter à la fin de chaque session, même courte.
But : que la session suivante (toi ou Claude Code) comprenne le contexte en 30 secondes
sans avoir à relire tout le code.

Format d'une entrée :
```
## [AAAA-MM-JJ] — Session
**Tâche(s) réalisée(s) :**
**Décisions prises (et pourquoi) :**
**Problèmes rencontrés / points de vigilance :**
**Prochaine étape :**
```

---

## [2026-09-11] — Session
**Tâche(s) réalisée(s) :** Squelette Spring Boot (Maven, Java 21, structure par domaine) +
`docker-compose.yml` racine avec services PostgreSQL et Redis.
**Décisions prises (et pourquoi) :** Le domaine "classes scolaires" est nommé `schoolclass`
(et non `class`, mot réservé Java invalide comme nom de package) — voir ADR-007 dans
`docs/ARCHITECTURE.md`. Les 16 packages de domaine (`auth`, `tenant`, `billing`, `student`,
`teacher`, `schoolclass`, `timetable`, `attendance`, `grade`, `reportcard`, `homework`,
`document`, `messaging`, `notification`, `discipline`, `statistics`) sont créés vides
(`.gitkeep`) : le sous-découpage `controller/service/repository/entity/dto/mapper` sera
ajouté module par module au fur et à mesure du besoin réel, pas en amont pour tous les
domaines. Dépendances backend limitées à web/validation/actuator/test pour l'instant —
JPA, driver PostgreSQL et Flyway seront ajoutés à la tâche suivante (configuration Flyway)
plutôt qu'ici, pour garder un commit par tâche cohérent.
**Problèmes rencontrés / points de vigilance :** Le démon Docker n'était pas démarré sur la
machine de dev au moment du test ; `docker compose config` a validé la syntaxe mais le
démarrage réel des conteneurs (`docker compose up -d`) n'a pas pu être vérifié dans cette
session — à confirmer avant de coder la configuration Flyway (tâche suivante), qui a besoin
d'une vraie connexion PostgreSQL.
**Prochaine étape :** ROADMAP.md → Phase 0, tâche "Configurer Flyway pour les migrations
versionnées".

## [2026-09-11] — Session (suite)
**Tâche(s) réalisée(s) :**
- Correction du port PostgreSQL exposé par `docker-compose.yml` : `5433` au lieu de `5432`
  (le port 5432 était déjà occupé par une installation PostgreSQL native sur la machine de
  dev) ; port configurable via `DB_PORT`. `docker compose up -d` vérifié : les deux services
  démarrent `healthy`.
- Configuration Flyway : dépendances `flyway-core` + `flyway-database-postgresql` +
  `spring-boot-starter-data-jpa` + driver `postgresql`, datasource et
  `spring.jpa.hibernate.ddl-auto: validate` dans `application.yml` (Flyway/migrations reste
  la seule source de vérité du schéma, jamais Hibernate auto-DDL), dossier
  `src/main/resources/db/migration` créé vide (aucune table métier encore — ça, c'est
  Phase 1.1).
- Ajout de `backend/.env.example` (déjà référencé par le README mais jamais créé).
**Décisions prises (et pourquoi) :** Test d'intégration (`FlywayConfigurationTests`) basé sur
Testcontainers (`spring-boot-testcontainers` + `testcontainers-postgresql`) plutôt que sur le
`docker-compose` local : le test démarre son propre PostgreSQL jetable, donc `mvn test`
fonctionne partout (poste de dev sans docker-compose lancé, CI) sans dépendance à un service
pré-démarré manuellement.
**Problèmes rencontrés / points de vigilance :** Attention au suffixe de nom de classe de
test — `*IT` n'est PAS exécuté par Maven Surefire par défaut (seul Failsafe le ferait, non
configuré dans ce projet) ; utiliser `*Tests` ou `*Test` pour que `mvn test` les exécute
réellement (piège rencontré : un premier test nommé `FlywayMigrationIT` ne tournait jamais,
silencieusement).
**Prochaine étape :** ROADMAP.md → Phase 0, tâche "Initialiser le squelette Angular avec
Angular Material installé et thème de base".

## [2026-09-11] — Session (suite 2)
**Tâche(s) réalisée(s) :** Squelette Angular (`web/`) avec Angular Material installé, thème
Material 3 neutre (palettes `azure`/`blue`, pas de branding tenant), routing activé.
`AppComponent` remplace la page de bienvenue par défaut par un simple `<mat-toolbar>`, avec
un test vérifiant qu'il s'affiche.
**Décisions prises (et pourquoi) :**
- Angular CLI 22 génère par défaut avec le nouveau runner de tests **Vitest**. `CLAUDE.md`
  restreint les tests frontend à Jest ou Karma/Jasmine → régénéré avec
  `--test-runner=karma` pour rester conforme.
- Node.js 22+ requis (Angular CLI 22 ne fonctionne pas avec Node 20 malgré l'ancien
  prérequis du README) — README mis à jour en conséquence. La machine de dev a plusieurs
  versions de Node installées via `nvm` ; utiliser `nvm use 22` (ou supérieur) avant toute
  commande `ng`/`npm` dans `web/`.
- Tests Karma nécessitent Chrome installé sur la machine (mode headless en CI, mode normal
  en local) — ajouté aux prérequis du README.
**Problèmes rencontrés / points de vigilance :**
- Le générateur par défaut (`ng new` sans `--test-runner=karma`) a produit une erreur npm
  reproductible (`Cannot read properties of null (reading 'edgesOut')`, bug connu de
  l'arborist npm avec l'arbre de dépendances optionnelles de Vitest) — un signe de plus
  d'éviter Vitest ici pour l'instant, indépendamment de la contrainte CLAUDE.md.
- Le pipeline GitHub Actions (tâche à venir) devra lancer `ng test` avec
  `--browsers=ChromeHeadless` (et `CHROME_BIN` si nécessaire selon l'image du runner) —
  la commande par défaut ouvre un vrai Chrome, ce qui ne marche pas en CI headless.
**Prochaine étape :** ROADMAP.md → Phase 0, tâche "Initialiser le squelette Ionic/Capacitor".

## [2026-09-11] — Session (suite 3)
**Tâche(s) réalisée(s) :** Squelette Ionic/Capacitor (`mobile/`), Angular standalone + Ionic
Angular 9, plateformes natives Android et iOS ajoutées (`android/`, `ios/`), thème Ionic
neutre par défaut (`src/theme/variables.scss`, palette Ionic standard, pas de branding
tenant).
**Décisions prises (et pourquoi) :**
- Le starter officiel `ionic start ... --capacitor` embarque lui aussi Vitest par défaut
  (même contrainte que pour `web/`, cf. entrée précédente) → écarté. À la place : socle
  Angular généré avec `--test-runner=karma` (identique à `web/`), puis Ionic Angular
  (`@ionic/angular`, `ionicons`) et Capacitor (`@capacitor/core`, `@capacitor/cli`,
  `@capacitor/android`, `@capacitor/ios`) ajoutés par-dessus manuellement (workflow standard
  "add Ionic to an existing Angular app").
- Projet Angular renommé `mobile` → `app` dans `angular.json` : c'est la convention attendue
  par l'intégration Capacitor de l'Ionic CLI (`ionic serve`/`ionic build` cherchent un projet
  nommé `app` par défaut, sinon il faut passer `--project=mobile` à chaque commande).
  `capacitor.config.ts` (`webDir`) mis à jour en conséquence (`dist/app/browser`).
- `ionic.config.json` créé à la main (le projet n'a pas été généré via `ionic start`, donc ce
  fichier — nécessaire pour que `ionic serve`/`ionic capacitor` fonctionnent — n'existait
  pas).
**Problèmes rencontrés / points de vigilance :**
- Build Gradle Android en échec en local (`Unsupported class file major version 69`) : le
  Java système de la machine est la version 25, trop récente pour la version de Gradle
  utilisée par le plugin Android de Capacitor. Ce n'est pas bloquant pour le squelette (les
  fichiers du projet Android sont bien générés) mais il faudra ouvrir le projet avec Android
  Studio (qui embarque son propre JDK compatible, typiquement 21) pour compiler réellement
  l'APK — ne pas essayer de "corriger" ça en changeant le Java système par défaut.
- `npm audit` signale 3 vulnérabilités modérées, toutes transitives via `@capacitor/cli`
  (dépendance dev uniquement, non embarquée dans l'app) → non corrigées maintenant, le fix
  proposé (`npm audit fix --force`) forcerait un downgrade cassant de `@capacitor/cli`, pas
  justifié pour un souci modéré côté outillage seul.
**Prochaine étape :** ROADMAP.md → Phase 0, tâche "Mettre en place le pipeline GitHub Actions
de base (build + tests sur chaque push)".

## [2026-09-11] — Session (suite 4)
**Tâche(s) réalisée(s) :** Finalisation du README (tous les squelettes backend/web/mobile
étant maintenant initialisés). Ajout de `springdoc-openapi-starter-webmvc-ui` au backend
pour que la doc Swagger promise par le README (`/swagger-ui/index.html`) soit réellement
disponible (requis aussi par `docs/API_CONVENTIONS.md` §"chaque endpoint documenté via
OpenAPI/Swagger").
**Décisions prises (et pourquoi) :** Le port 8080 par défaut du backend n'est pas changé
dans la config (reste la convention Spring Boot standard) — un conflit de port local
(rencontré sur cette machine de dev à cause d'un autre projet déjà lancé) est documenté
dans le README via `SERVER_PORT=<port> ./mvnw spring-boot:run`, sans toucher au défaut du
projet.
**Problèmes rencontrés / points de vigilance :** En testant le endpoint `/v3/api-docs`, la
première tentative a répondu avec une tout autre API ("EFFORT ATHLETIC") — ce n'était pas
notre backend mais un autre projet déjà démarré sur le port 8080 de la machine partagée.
Toujours vérifier le contenu de la réponse (pas juste le code HTTP) quand un port par défaut
très commun est utilisé sur une machine de dev partagée.
**Prochaine étape :** ROADMAP.md → Phase 0, tâche "Mettre en place le pipeline GitHub Actions
de base (build + tests sur chaque push)".

## [2026-09-11] — Session (suite 5)
**Tâche(s) réalisée(s) :** Pipeline GitHub Actions de base (`.github/workflows/ci.yml`),
3 jobs indépendants (backend/web/mobile) déclenchés sur chaque push et sur les PR vers
`main` : `./mvnw verify` (backend, Java 21 Temurin), `npm ci && npm run build && ng test
--browsers=ChromeHeadless` (web et mobile, Node 22).
**Décisions prises (et pourquoi) :** Pas de filtrage par chemin (`paths:`) pour l'instant —
les 3 jobs tournent à chaque push même si un seul dossier a changé. Le monorepo est encore
petit, l'optimisation n'apporte rien maintenant et ajoute de la complexité ; à reconsidérer
si les temps de CI deviennent gênants. Build Android/iOS natifs volontairement exclus du
pipeline (nécessiteraient Android SDK/Xcode sur le runner, hors scope Phase 0 — seule la
couche Angular/Ionic est buildée et testée en CI).
**Problèmes rencontrés / points de vigilance :** Les 3 jobs ont été validés en rejouant
localement exactement les commandes du workflow (`./mvnw -B verify`, `npm ci && npm run
build && npx ng test --watch=false --browsers=ChromeHeadless`) mais pas encore exécutés sur
un vrai runner GitHub Actions (pas de push effectué) — à confirmer au premier push.
**Prochaine étape :** ROADMAP.md → Phase 0, tâche "Configurer les environnements dev /
staging / prod (fichiers de config séparés)".

## [2026-09-11] — Session (suite 6)
**Tâche(s) réalisée(s) :** Fichiers de config séparés par environnement pour les 3 apps.
Backend : profils Spring `dev`/`staging`/`prod` (`application-{profil}.yml`), `dev` actif par
défaut via `SPRING_PROFILES_ACTIVE`. Web et Mobile : `src/environments/environment*.ts` +
configurations Angular (`fileReplacements` sur `production` et nouvelle configuration
`staging` ajoutée dans `angular.json`).
**Décisions prises (et pourquoi) :**
- Web (staging/prod) : `apiUrl` relative (`/api/v1`), en cohérence avec le reverse proxy
  Nginx documenté dans `docs/ARCHITECTURE.md`/`CLAUDE.md` (Web et API derrière le même
  domaine).
- Mobile (staging/prod) : `apiUrl` doit être absolue (une appli compilée n'a pas de "même
  origine" avec une API) — utilise un domaine placeholder `*.schoolsaas.example` (TLD
  réservé à la documentation, RFC 2606, pour ne pas laisser croire à une vraie URL) en
  attendant que l'hébergement staging/prod soit tranché (point ouvert dans
  `docs/ARCHITECTURE.md`).
- `dev`/`staging`/`prod` backend ont des différences réelles et vérifiées (pas juste des
  fichiers vides) : logs SQL + détails `/actuator/health` en dev, stacktraces d'erreur
  masquées et détails de santé cachés en staging/prod.
**Problèmes rencontrés / points de vigilance :** `environment.ts` n'est pour l'instant
consommé nulle part dans le code Web/Mobile (aucun service ne l'importe encore) — la
prochaine tâche (chargement dynamique du branding tenant) sera le premier vrai
consommateur de `environment.apiUrl`, ce qui permettra de vérifier que le mécanisme de
remplacement de fichier fonctionne bout en bout (pas seulement "le build ne plante pas").
**Prochaine étape :** ROADMAP.md → Phase 0, tâche "Définir les design tokens (couleurs
neutres, typographie, espacement)".

## [2026-09-11] — Session (suite 7)
**Tâche(s) réalisée(s) :** Design tokens définis (`docs/DESIGN.md` §2) : couleurs neutres
sémantiques (texte, bordure, succès/avertissement/danger, vérifiées ≥ 4.5:1 sur fond blanc),
`--tenant-primary`/`--tenant-secondary`/`--tenant-on-primary` (valeurs neutres par défaut,
prêtes à être écrasées au runtime), grille d'espacement 4px (`--space-1`…`--space-16`),
rayon de bordure unique (`--radius`), typographie (police unique Roboto, 3 tailles de
titre + corps + légende). Implémentés dans `web/src/styles/tokens.scss` (importé dans
`styles.scss`) et `mobile/src/theme/variables.scss`, mêmes valeurs dupliquées dans les deux
(pas de package partagé entre les deux workspaces Angular pour l'instant).
**Décisions prises (et pourquoi) :**
- `--mat-sys-primary`/`--mat-sys-on-primary` (Web) et `--ion-color-primary`/`-secondary`
  (Mobile) sont explicitement mappés sur `--tenant-primary`/`--tenant-secondary` : c'est le
  point d'accroche que la prochaine tâche (branding tenant dynamique) utilisera pour
  recolorer l'app au runtime, sans toucher aux composants.
- Limite assumée et documentée en commentaire : côté Web (Material 3), seuls
  primary/on-primary suivent le tenant — les rôles dérivés (primary-container, etc.) restent
  ceux du thème compilé au build (une régénération complète de palette au runtime dépasse le
  cadre du squelette Phase 0).
- Couleurs sémantiques success/warning/danger alignées à l'identique entre Web et Mobile
  (au lieu de garder les valeurs par défaut Ionic, différentes de celles choisies pour Web)
  pour une vraie cohérence cross-plateforme, conformément à l'esprit de docs/DESIGN.md §2.
**Problèmes rencontrés / points de vigilance :** Aucun. Tokens vérifiés par un test dans
chaque app (`web/src/styles/tokens.spec.ts`, `mobile/src/theme/tokens.spec.ts`) qui lit les
valeurs calculées via `getComputedStyle` — pas juste "le build ne plante pas".
**Prochaine étape :** ROADMAP.md → Phase 0, tâche "Implémenter le chargement dynamique du
branding tenant (logo + couleurs) au démarrage Web et Mobile".

## [2026-09-11] — Session (suite 8)
**Tâche(s) réalisée(s) :** Chargement dynamique du branding tenant (Web et Mobile), suivant
exactement le mécanisme décrit dans `docs/DESIGN.md` §4 : `TenantBrandingService` (dans
`app/branding/`, dupliqué à l'identique entre Web et Mobile) qui, au démarrage de l'app
(`provideAppInitializer`) : (1) applique immédiatement le branding en cache
(`sessionStorage`) ou neutre par défaut, sans jamais bloquer le premier rendu ; (2) appelle
`GET {apiUrl}/tenants/current/branding` en arrière-plan ; (3) si succès, recolore l'app
(`--tenant-primary`/`--tenant-secondary`/`--tenant-on-primary` sur `:root`, déjà câblés sur
les tokens Material/Ionic depuis la tâche précédente) et met en cache ; (4) si échec,
conserve silencieusement le branding neutre déjà affiché. Le nom et le logo du tenant
s'affichent dans la barre de titre (`mat-toolbar` Web, `ion-toolbar` Mobile).
**Décisions prises (et pourquoi) :**
- L'endpoint `/api/v1/tenants/current/branding` n'existe pas encore côté backend (la table
  `tenants` est une tâche de Phase 1.1) — l'appel échouera donc systématiquement pour
  l'instant, ce qui est le comportement correct et attendu : le fallback neutre (point 5 du
  §4) est exactement ce qui doit se produire tant que Phase 1.1 n'est pas faite. Aucun mock
  ni endpoint factice ajouté : l'intégration est réelle, elle s'activera d'elle-même une fois
  le backend prêt.
- Couleur de texte sur fond tenant (`--tenant-on-primary`) calculée par une fonction de
  luminance simple (`contrast-color.ts`) plutôt que codée en dur à blanc, pour éviter un
  texte illisible si un tenant choisit une couleur primaire claire.
- `provideAppInitializer` (API fonctionnelle moderne, remplace le token `APP_INITIALIZER`)
  ne bloque le rendu que le temps d'appliquer le cache/défaut (synchrone) ; l'appel réseau
  n'est pas attendu par l'initializer (fire-and-forget), pour ne jamais retarder le premier
  affichage à cause d'un réseau lent.
**Problèmes rencontrés / points de vigilance :** Aucun code partagé entre `web/` et
`mobile/` (deux workspaces Angular indépendants) → le service de branding est dupliqué à
l'identique dans les deux, comme déjà fait pour les design tokens. À surveiller : si ce
genre de duplication continue de grossir en Phase 1+, envisager un package partagé
(nx/workspace lib) plutôt que de continuer à dupliquer fichier par fichier.
**Prochaine étape :** ROADMAP.md → Phase 0, tâche "Maquetter (même basse fidélité) les 6-7
écrans clés listés dans docs/DESIGN.md §5" — dernière tâche de la Phase 0.

## [2026-09-11] — Session (suite 9)
**Tâche(s) réalisée(s) :** Maquettes basse fidélité des 7 écrans clés de `docs/DESIGN.md`
§5, dans un nouveau `docs/MOCKUPS.md` (référencé depuis `README.md` et `CLAUDE.md`).
**Décisions prises (et pourquoi) :** Choix de wireframes texte/ASCII (zones, hiérarchie,
composants Material/Ionic à utiliser, points d'attention), pas de code Angular/Ionic réel
pour ces écrans. Raison : `docs/DESIGN.md` §7 traite explicitement cette tâche comme un
travail de cadrage *avant* le développement des écrans ("à défaut de maquettes Figma,
décrire précisément la disposition attendue... avant de laisser Claude Code générer le
HTML/Angular") ; les écrans réels (feuille d'appel, notes, emploi du temps, dashboards,
wizard d'import) sont des livrables explicites de Phase 1/2 dans `docs/ROADMAP.md`. Coder
des pages maintenant aurait dupliqué/anticipé ce travail sans backend/données réelles
derrière, au risque de devoir tout refaire (règle CLAUDE.md : ne pas sauter de phase, pas
d'implémentation à moitié finie).
**Problèmes rencontrés / points de vigilance :** Aucun — tâche documentaire, pas de code ni
de test associé (n'entre pas dans le champ de la règle CLAUDE.md sur la couverture de test).
**Phase 0 terminée** : les 13 tâches de `docs/ROADMAP.md` sont cochées. **Prochaine étape :**
ROADMAP.md → Phase 1.1 (fondations multi-tenant) — table `tenants`, colonne `school_id`
sur les tables métier, Row-Level Security, Hibernate Filter, tests d'isolation.

## [2026-09-12] — Session (Phase 1.1 + 1.2)
**Tâche(s) réalisée(s) :** Fondations multi-tenant (1.1) et authentification/rôles (1.2),
traitées ensemble (il fallait une vraie table scopée — `users` — pour prouver le mécanisme
d'isolation). Table `tenants`, `TenantScopedEntity` (base JPA), `TenantContextInterceptor`
(résolution JWT/en-tête `X-Tenant-Id`/sous-domaine), filtre Hibernate `tenantFilter`, RLS
PostgreSQL sur `users`. Auth JWT complète : `User`/`PlatformAdmin` (séparés, voir ADR-008),
`Role` (énumération fixe), login/refresh/logout avec rotation du refresh token, RBAC via
`@PreAuthorize`. Enveloppe de réponse/erreur uniforme (`ApiResponse`/`GlobalExceptionHandler`,
voir docs/API_CONVENTIONS.md). Endpoint `/api/v1/users` (me, by id, liste paginée) ajouté
spécifiquement pour pouvoir écrire le test d'isolation exigé par CLAUDE.md sur un vrai
endpoint scoped-tenant.
**Décisions prises (et pourquoi) :** Voir ADR-001 (mis à jour avec les 5 pièges détaillés
ci-dessous) et ADR-008 dans `docs/ARCHITECTURE.md`.
**Problèmes rencontrés / points de vigilance — IMPORTANT, à lire avant de créer toute
nouvelle entité scopée `school_id` :** Le premier jet de l'isolation multi-tenant ne
fonctionnait PAS du tout malgré une implémentation qui semblait correcte sur le papier — le
test d'isolation (écrit avant de déclarer la tâche terminée, comme l'exige CLAUDE.md) a
détecté une vraie fuite cross-tenant. Cinq bugs distincts trouvés et corrigés dans la même
session (détail technique dans ADR-001) :
1. `@Filter` sur une `@MappedSuperclass` n'est pas hérité de façon fiable par Hibernate —
   chaque entité concrète doit le redéclarer elle-même.
2. `TenantContextInterceptor` s'exécutait AVANT `OpenEntityManagerInViewInterceptor` de
   Spring Boot (ordre d'intercepteurs MVC par défaut non garanti entre configs) —
   l'activation du filtre se faisait sur un EntityManager jetable, sans aucun effet sur les
   requêtes suivantes. Fixé avec `.order(100)`.
3. `Repository#findById`/`getReferenceById` contournent complètement les filtres Hibernate
   (délèguent à `EntityManager#find()`/`getReference()`) — fixé structurellement via une
   classe de base de repository (`TenantScopedRepositoryImpl`) qui repasse par du JPQL,
   plutôt que de compter sur chaque futur développeur pour s'en souvenir.
4. Row-Level Security ne protège RIEN pour un rôle superutilisateur PostgreSQL (le rôle créé
   par défaut via `POSTGRES_USER` dans docker-compose) — aucune exception possible, même
   avec `FORCE ROW LEVEL SECURITY`. Fixé en séparant le rôle Flyway (admin, migrations) du
   rôle applicatif runtime (restreint) — voir `backend/docker/postgres-init/
   01-create-app-role.sh`.
5. La variable de session PostgreSQL RLS pouvait se perdre entre deux transactions
   `@Transactional` distinctes d'une même requête (connexion JDBC physique relâchée puis
   réacquise différemment sous Open Session In View) — fixé via
   `hibernate.connection.handling_mode: DELAYED_ACQUISITION_AND_HOLD`.

Chacun de ces 5 points a désormais un test dédié qui échouerait si le bug revenait
(`TenantIsolationTest`, 4 tests). Testé aussi de bout en bout avec `docker compose up` réel
(pas seulement Testcontainers) pour confirmer que le rôle applicatif restreint fonctionne
correctement pour les opérations normales (login, /me) tout en étant protégé par RLS.

Bug d'infrastructure de test corrigé au passage : le pattern "Singleton Container" de
Testcontainers avec `@Container` sur un champ statique hérité ne partage PAS fiablement le
conteneur entre classes de test (JUnit l'arrête après chaque classe concrète, causant des
"Connection refused" aléatoires) — remplacé par un démarrage manuel dans un bloc statique,
sans `@Container`/`@Testcontainers`, conforme au pattern officiellement documenté.
**Prochaine étape :** ROADMAP.md → Phase 1.3 (onboarding self-service) — formulaire
d'inscription d'établissement, création automatique tenant + compte Administrateur initial,
assistant de configuration.

## [2026-09-12] — Session (Phase 1.3)
**Tâche(s) réalisée(s) :** Inscription self-service d'un établissement. Backend :
`POST /api/v1/tenants/register` (public, déjà autorisé dans `SecurityConfig`) — crée le
tenant (statut `TRIAL`) + le compte Administrateur initial, puis connecte immédiatement ce
dernier (réutilise `AuthService.login`). Web : page `/register` (Angular Material,
formulaire réactif), `RegistrationService`, `AuthTokenService` (stockage minimal des jetons
en `sessionStorage`, réutilisable par les futurs écrans protégés).
**Décisions prises (et pourquoi) :** L'assistant de configuration multi-étapes
(cahier-des-charges.md §20.2 : import classes/matières/élèves/enseignants/emploi du temps)
n'est PAS construit maintenant — il dépend fonctionnellement des modules Phase 1.5
(élèves/classes/matières) et 1.6 (emploi du temps), qui n'existent pas encore. Le construire
maintenant aurait signifié soit des appels vers des endpoints fictifs, soit sauter l'ordre
des phases (CLAUDE.md règle 5). Seule l'étape 1 ("informations établissement") est donc
livrée ; le reste sera ajouté incrémentalement quand 1.5/1.6 seront prêts. Anti-abus
(captcha, rate limiting Redis — cahier-des-charges.md §20.1) volontairement pas implémenté
non plus : Redis n'est pas encore câblé dans le backend (présent seulement dans
docker-compose) — noté comme point ouvert plutôt que bâclé.
**Problèmes rencontrés / points de vigilance :** Piège retrouvé une seconde fois (déjà vu en
1.1/1.2) : au moment de l'inscription, aucun contexte tenant n'est encore posé (endpoint
public, pas de JWT). `AuthService.login()` appelé juste après la création du compte a donc
dû être entouré d'un `TenantContext.set(...)` + `TenantSessionConfigurer.applyTenant(...)`
explicites pour que la recherche par email reste bornée au tenant qu'on vient de créer.
**Prochaine étape :** ROADMAP.md → Phase 1.4 (abonnement de base) — tables plans/
subscriptions/invoices, essai gratuit automatique, intégration Stripe (checkout + webhook),
blocage progressif en cas d'échec de paiement.

## [2026-09-14] — Session (Phase 1.4)
**Tâche(s) réalisée(s) :** Abonnement de base, les 4 tâches de ROADMAP.md 1.4 traitées
ensemble (décision explicite de l'utilisateur, plutôt que de les étaler comme pour 1.3).
Tables `plans` (migration déjà présente en local, non committée, complétée), `subscriptions`,
`invoices` — entités plateforme comme `tenants`, pas de `school_id`/RLS, filtrage manuel par
`tenant_id` dans les repositories. Essai gratuit automatique (30 jours, plan `ESSENTIEL` par
défaut) créé par `TenantRegistrationService` via `SubscriptionService`. Stripe Checkout
(`POST /api/v1/billing/checkout`) + webhook (`POST /api/v1/billing/webhooks/stripe`, signature
uniquement) via `StripeGateway`/`StripeGatewayImpl` (SDK `stripe-java` 33.4.2). Blocage
progressif porté par `Tenant.status` (déjà `TRIAL/ACTIVE/READ_ONLY/SUSPENDED/CANCELLED`,
défini dès la Phase 1.1 mais jamais encore branché) : `TenantAccessInterceptor` bloque les
écritures en `READ_ONLY` et tout en `SUSPENDED`/`CANCELLED` (sauf auth/billing/inscription),
`TenantAccessLifecycleJob` (planifié horaire) fait descendre les tenants d'un cran par
exécution selon des délais de grâce configurables.
**Décisions prises (et pourquoi) :** Voir ADR-009 (nouveau) dans `docs/ARCHITECTURE.md` pour
le détail complet — résumé : devise XOF (marché cible Franc CFA, "zéro décimale" chez Stripe,
donc pas de `*100`), un seul `Subscription` par tenant (pas d'historique de changement de
plan), aucun choix de plan à l'inscription (l'essai démarre automatiquement sur `ESSENTIEL`,
le changement se fait après coup via checkout), traitement webhook idempotent par upsert
plutôt qu'une table d'événements déjà traités. Délais de grâce (3 jours "past due", 7 jours
"lecture seule") et grille tarifaire XOF choisis comme point de départ raisonnable, **à
confirmer avec le porteur de projet** — même traitement que les prix déjà présents dans la
migration V4. Périodicité annuelle, proratisation de changement de plan, mobile money et
back-office Super-Admin pour les plans sont explicitement hors périmètre (Phase 2/3).
**Problèmes rencontrés / points de vigilance :**
1. Le SDK `stripe-java` récent a changé la structure de `Invoice` : plus de
   `getSubscription()` direct, l'id de l'abonnement est maintenant sous
   `invoice.getParent().getSubscriptionDetails().getSubscription()` — vérifié en inspectant
   le bytecode du jar (`javap`) plutôt que de deviner, avant d'écrire `StripeWebhookService`.
2. Mockito (mock maker inline) ne peut pas mocker `com.stripe.model.Event` sur cet
   environnement ("Could not modify all classes ... com.stripe.model.Event") — contourné en
   construisant de VRAIS objets `Event` via `ApiResource.GSON.fromJson(json, Event.class)`
   (le mécanisme interne réel du SDK, y compris le champ `api_version` qui doit correspondre
   à `Stripe.API_VERSION` sous peine de désérialisation silencieusement vide), voir
   `StripeWebhookServiceTest`. Seule `StripeGateway` (notre propre interface) reste mockée.
3. `AbstractIntegrationTest` partage un seul conteneur Postgres entre TOUTES les classes de
   test (pattern "Singleton Container", voir son javadoc) : `BillingCheckoutTest` mute
   `stripe_price_id` sur le plan `STANDARD` (pas `ESSENTIEL`, dont d'autres tests dépendent
   pour rester non-achetable) pour ne pas faire fuiter un état mutable entre classes.
**Prochaine étape :** ROADMAP.md → Phase 1.5 (élèves, parents, enseignants, classes,
matières) — CRUD élèves avec import CSV, CRUD parents/tuteurs + association aux élèves, CRUD
enseignants, CRUD classes/matières + affectations.

## [2026-09-14] — Session (Phase 1.5, suite)
**Tâche(s) réalisée(s) :** Les 4 tâches de ROADMAP.md 1.5 : CRUD élèves (`student/`) avec
import CSV en masse, CRUD parents/tuteurs (`parent/`) + association élève/parent
(`student_parents`), CRUD enseignants (`teacher/`), CRUD classes (`schoolclass/`) et matières
(`subject/`) + affectation enseignant/classe/matière (`class_subject_assignments`). Décision
explicite de l'utilisateur : enchaîner toute la Phase 1 (1.5 à 1.9) sans validation
intermédiaire entre chaque sous-tâche.
**Décisions prises (et pourquoi) :** Voir ADR-010 (nouveau) dans `docs/ARCHITECTURE.md` —
résumé : pas d'année scolaire pour ce MVP (simplification, non listée dans les tâches 1.5),
`Teacher`/`Parent` sont des fiches métier découplées de tout compte `User` (le portail élève/
parent n'existe pas), un seul enseignant par (classe, matière), import CSV avec parseur
simple (pas de support des champs contenant une virgule), endpoints réservés aux rôles staff.
**Problèmes rencontrés / points de vigilance :** Piège récurrent (déjà vu en 1.1) à chaque
fixture de test représentant les données d'un "tenant B" créées hors requête HTTP : sans
`TenantContext` actif, `TenantScopedEntity#assignTenantIfMissing` ne peut pas déduire
`school_id` → violation de contrainte NOT NULL si on oublie `setSchoolId(...)` avant le
premier `save(...)`. Centralisé dans un nouvel utilitaire de test partagé
`com.schoolsaas.TestAuthSupport` (tenant actif + connexion + `withTenant(...)`) pour éviter de
redécouvrir ce piège dans chaque nouvelle classe de test des phases suivantes.
**Prochaine étape :** ROADMAP.md → Phase 1.6 (emploi du temps) — CRUD emploi du temps
(classe, enseignant, matière, salle, horaire) + détection des conflits.

## [2026-09-14] — Session (Phase 1.6)
**Tâche(s) réalisée(s) :** Les 2 tâches de ROADMAP.md 1.6. Module `timetable/` : `Room`
(CRUD simple) et `TimetableEntry` (créneau récurrent hebdomadaire : classe/matière/
enseignant/salle/jour/horaires) avec détection de conflits (enseignant, salle, classe déjà
occupés sur un créneau qui chevauche le même jour) à la création ET à la modification.
**Décisions prises (et pourquoi) :** Voir ADR-011 (nouveau) dans `docs/ARCHITECTURE.md` —
résumé : une seule entité `TimetableEntry` (pas de séparation timetables/courses), pas de
date de début/fin ni d'exceptions ponctuelles (annulation, remplacement — hors périmètre
1.6), vues par classe/enseignant via query params plutôt que des endpoints dédiés.
**Problèmes rencontrés / points de vigilance :** Aucun nouveau — même piège école
(`setSchoolId` avant `save` pour les fixtures hors requête HTTP) déjà couvert par
`TestAuthSupport.withTenant`.
**Prochaine étape :** ROADMAP.md → Phase 1.7 (absences) — feuille d'appel, motifs/
justificatifs/historique, notification au parent.

## [2026-09-14] — Session (Phase 1.7)
**Tâche(s) réalisée(s) :** Les 3 tâches de ROADMAP.md 1.7. Module `attendance/` :
`AttendanceRecord` (un statut par élève et par jour), feuille d'appel en une requête
(`POST /api/v1/attendance/roll-call`), historique des modifications (`AttendanceRecordChange`,
instantané de l'état précédent à chaque `update`), notification parent via une passerelle
`ParentNotificationGateway`.
**Décisions prises (et pourquoi) :** Voir ADR-012 (nouveau) — résumé : granularité par jour
(pas par cours), notification parent implémentée comme un simple log (`LoggingParent-
NotificationGateway`) car ni Redis ni FCM ne sont câblés au backend (FCM est explicitement
Phase 2 dans ROADMAP.md §16) — pattern identique à `StripeGateway` pour permettre de
brancher la vraie implémentation plus tard sans toucher au service.
**Problèmes rencontrés / points de vigilance :** Aucun nouveau.
**Prochaine étape :** ROADMAP.md → Phase 1.8 (notes) — CRUD évaluations et notes, calcul
automatique des moyennes.

## [2026-09-14] — Session (Phase 1.8)
**Tâche(s) réalisée(s) :** Les 2 tâches de ROADMAP.md 1.8. Module `grade/` : `Exam`
(évaluation) et `Grade` (note, `score` NULL = absence explicite via colonne `absent`).
Calcul automatique des moyennes : statistiques d'évaluation (moyenne/min/max), moyenne
élève pondérée par coefficient dans une matière, moyenne de classe dans une matière.
**Décisions prises (et pourquoi) :** Voir ADR-013 (nouveau) — résumé : tout normalisé sur
20 pour comparer des barèmes différents, moyenne de classe = moyenne des moyennes-élèves
(poids égal par élève). Import CSV des notes hors périmètre (seule la saisie API est
couverte, contrairement aux élèves en 1.5).
**Problèmes rencontrés / points de vigilance :** Colonnes `max_score`/`score` créées en
`NUMERIC(5,2)` dans la migration ne correspondent PAS au mapping Hibernate par défaut d'un
champ Java `double`/`Double` (qui attend `DOUBLE PRECISION`/`float8`) — `ddl-auto: validate`
a fait échouer le démarrage du contexte Spring au premier lancement des tests
(`SchemaManagementException`). Corrigé en changeant le type de colonne en
`DOUBLE PRECISION`. À garder en tête pour toute future colonne numérique décimale : soit
`DOUBLE PRECISION` + champ Java `double`/`Double`, soit `NUMERIC` + `java.math.BigDecimal`
côté entité — jamais `NUMERIC` + `double`.
**Prochaine étape :** ROADMAP.md → Phase 1.9 (dashboard établissement) — statistiques de
base (effectifs, taux de présence, moyennes). Dernière tâche de la Phase 1 — son critère de
sortie (scénario complet inscription → usage → paiement → suspension → réactivation) sera à
vérifier une fois cette tâche terminée.

## [2026-09-14] — Session (Phase 1.9 — fin de la Phase 1)
**Tâche(s) réalisée(s) :** La tâche de ROADMAP.md 1.9. Module `statistics/` :
`GET /api/v1/dashboard/summary` (effectifs élèves/enseignants/classes, taux de présence sur
une période, moyenne générale). Décision explicite de l'utilisateur en début de session :
enchaîner tout Phase 1 (1.5 à 1.9) sans validation intermédiaire — les 9 sous-phases sont
maintenant cochées dans ROADMAP.md, 62 tests passent (toute la suite, isolation multi-tenant
comprise sur chaque nouveau module).
**Décisions prises (et pourquoi) :** Voir ADR-014 (nouveau) — trois métriques seulement,
conformes au bullet unique de ROADMAP.md 1.9 ; pas de ventilation détaillée (déjà couverte
par le module `grade`), pas de rapports exportables ni d'évolution temporelle (Phase 2).
**Point important à ne pas perdre de vue** : le critère de sortie de Phase 1 tel qu'écrit
dans ROADMAP.md ("un établissement peut s'inscrire seul, configurer... et payer un
abonnement") suppose une utilisation de bout en bout via une interface. Or les tâches 1.5 à
1.9 étaient formulées comme des livrables API ("CRUD X"), pas "(Web)" comme l'était 1.3 —
et c'est ce qui a été construit : API backend complète et testée, **sans écrans Angular/
Ionic** pour classes/élèves/enseignants/emploi du temps/absences/notes/dashboard (seules les
pages d'inscription/branding existent côté Web/Mobile). Une note a été ajoutée directement
dans ROADMAP.md sous le critère de sortie pour que la prochaine session ne parte pas du
principe que "Phase 1 cochée" = "utilisable de bout en bout par un vrai utilisateur".
**Problèmes rencontrés / points de vigilance :** Aucun nouveau (même piège colonnes
`DOUBLE PRECISION` que 1.8 — pas rencontré ici car pas de nouvelle colonne décimale).
**Prochaine étape :** Décision à prendre avec le porteur de projet avant de continuer :
(a) construire les écrans Web/Mobile pour les modules 1.5-1.9 avant de considérer la Phase 1
réellement "terminée" au sens du critère de sortie, ou (b) passer directement à
ROADMAP.md Phase 2 (bulletins, cahier de textes, documents, notifications push, messagerie,
dashboard Super-Admin) en acceptant que le frontend de Phase 1 reste à construire plus tard.

## [2026-09-14] — Session (frontend Web des modules 1.5-1.9)
**Tâche(s) réalisée(s) :** À la demande explicite de l'utilisateur ("construire le frontend
de ces écrans maintenant"), construction du frontend Web (Angular Material) pour tous les
modules Phase 1.5-1.9 : shell authentifié (sidenav filtré par rôle), connexion/déconnexion,
Élèves (+ import CSV en 2 étapes), Parents (+ association), Enseignants, Classes (+
affectation matière), Matières, Emploi du temps (+ salles), Absences (feuille d'appel +
historique), Notes (évaluations + saisie + moyennes), Dashboard établissement. Détail des
écarts vs docs/MOCKUPS.md dans ADR-015 (docs/ARCHITECTURE.md).
**Décisions prises (et pourquoi) :** Voir ADR-015. Résumé : feuille d'appel livrée en Web
(mockup prévoyait Mobile, mais l'app Mobile n'a aucune authentification construite) ; import
CSV en 2 étapes pas 3 (le backend importe en une seule opération, pas de phase "prévisualiser
sans committer") ; un service + une paire liste/dialog par ressource simple, écrans dédiés
pour les flux non triviaux.
**Problèmes rencontrés / points de vigilance — IMPORTANT** : la vérification "en vrai
navigateur contre un vrai backend" (workflow standard pour tout changement UI) a mis en
évidence deux bugs **backend** réels, invisibles jusqu'ici car tous les tests
d'intégration existants tournent avec le rôle superutilisateur par défaut de Testcontainers
(qui contourne TOUJOURS Row-Level Security, ADR-001 Piège 4) :
1. **L'inscription self-service violait RLS** avec le vrai rôle applicatif restreint
   (`docker-compose`/`school_saas`) : le compte Administrateur était inséré dans `users`
   avant que le contexte tenant (`app.tenant_id`) soit posé. Corrigé dans
   `TenantRegistrationService#register` ; test de régression `TenantRegistrationRlsTest`
   (recrée le rôle restreint réel, échoue bien sans le correctif — voir sa javadoc pour le
   piège `@ServiceConnection` vs `@DynamicPropertySource` rencontré en l'écrivant).
2. **Un endpoint inexistant renvoyait 500 au lieu de 404, sans aucun log serveur**
   (`NoResourceFoundException` avalée par le handler générique de `GlobalExceptionHandler`).
   Corrigé (handler dédié 404 + logging du handler générique) ; test de régression
   `GlobalExceptionHandlerTest`.
3. Le port 4200 (défaut Angular) était déjà occupé par un processus totalement étranger à ce
   projet sur cette machine — utilisé le port 4300 pour la vérification, sans y toucher.
4. **Découverte annexe, non corrigée (hors périmètre)** : `GET /api/v1/tenants/current/
   branding`, appelé par le frontend depuis la Phase 0, n'a jamais été implémenté côté
   backend — le branding dynamique par tenant ne fonctionne donc jamais réellement (retombe
   toujours sur le fallback neutre, sans casser l'écran). Noté dans "Points ouverts" de
   docs/ARCHITECTURE.md.
**Outillage** : `nvm use 22.23.1` nécessaire pour Angular CLI 22 (Node système en 16.20.2,
trop ancien). Vérification end-to-end faite avec Playwright piloté via Node 22 (pas de
skill `run` pré-existant pour ce repo, ni de `chromium-cli` disponible sur cette machine —
`channel: 'chrome'` utilisé à la place, contre l'app réellement servie par `ng serve` et le
backend réel + `docker-compose` Postgres/Redis).
**Prochaine étape :** ROADMAP.md Phase 2 (bulletins, cahier de textes, documents,
notifications push FCM, messagerie, dashboard Super-Admin), ou construire l'authentification
Mobile (Ionic) si la feuille d'appel doit être ramenée sur mobile comme prévu au départ.

## [2026-09-14] — Session (Phase 2.1 — bulletins scolaires)
**Tâche(s) réalisée(s) :** Sur demande de l'utilisateur ("la suite"), détail de ROADMAP.md
Phase 2 en tâches fines (2.1 à 2.6, même format que la Phase 1), puis réalisation de 2.1 :
module `reportcard/` — génération automatique des bulletins à partir des notes existantes,
appréciations (matière + professeur principal + décision conseil), récapitulatif absences/
retards, export PDF (Apache PDFBox).
**Décisions prises (et pourquoi) :** Voir ADR-016 (nouveau) — résumé : moyenne/coefficient
figés à la génération, période en texte libre (cohérent avec ADR-010), rang/signature
électronique différés (dépendent d'un paramétrage établissement qui n'existe pas). Extraction
de `NumberUtils.round2` (dupliqué 2x avant ce module, 3x aurait été trop).
**Problèmes rencontrés / points de vigilance :** Piège Spring Data avec
`deleteAllByReportCardId` dérivé (DELETE non flush avant les INSERT immédiats
`GenerationType.IDENTITY` à la régénération d'un bulletin) — corrigé avec un bulk DELETE
JPQL `@Modifying`. Détail complet dans ADR-016.
**Prochaine étape :** ROADMAP.md Phase 2.2 (bibliothèque de documents) — nécessaire avant
2.3/2.4 qui y référencent des pièces jointes.

## [2026-09-14] — Session (Phase 2.2 — bibliothèque de documents)
**Tâche(s) réalisée(s) :** Module `document/` — upload/consultation par matière/classe/
service, droits de consultation par rôle, archivage (suppression contrôlée), stockage
derrière `StorageGateway` (implémentation locale par défaut).
**Décisions prises (et pourquoi) :** Voir ADR-017 (nouveau) — résumé : aucune ligne de
`document_visible_roles` = visible par tout rôle staff (pas de matrice de permissions
complète), suppression = archivage réversible, quota de stockage par tenant explicitement
différé (le modèle `Plan` n'a pas de champ de quota).
**Problèmes rencontrés / points de vigilance :** Protection anti-traversée de répertoire
dans `LocalDiskStorageGateway` : comparer un chemin racine non normalisé
(`Path.of("./storage")`) à un chemin résolu normalisé/absolutisé rejetait à tort une clé de
stockage pourtant légitime — corrigé en normalisant/absolutisant les deux côtés avant
comparaison.
**Prochaine étape :** ROADMAP.md Phase 2.3 (cahier de textes et devoirs), qui référence des
documents comme pièces jointes.

## [2026-09-14] — Session (Phase 2.3 — cahier de textes et devoirs)
**Tâche(s) réalisée(s) :** Module `homework/` — `Lesson` (contenu de cours + travail à faire
regroupés dans une seule entité, pièce jointe référençant le module documents), notification
nouveau devoir.
**Décisions prises (et pourquoi) :** Voir ADR-018 (nouveau) — résumé : une seule entité
plutôt que deux (cours + devoir), nouvelle gateway de notification dédiée plutôt que
d'anticiper la généralisation prévue en 2.5.
**Problèmes rencontrés / points de vigilance :** Aucun nouveau.
**Prochaine étape :** ROADMAP.md Phase 2.4 (messagerie interne).

## [2026-09-14] — Session (Phase 2.4 — messagerie interne)
**Tâche(s) réalisée(s) :** Module `messaging/` — conversations individuelles/groupe/
annonces, messages avec pièce jointe optionnelle, confirmation de lecture, recherche.
**Décisions prises (et pourquoi) :** Voir ADR-019 (nouveau) — résumé : annonce = conversation
diffusée à tous à la création (pas un mécanisme séparé), lecture confirmée via un horodatage
par participant (pas un accusé par message), accès non-participant = 403 (pas 404, la
conversation existe bien dans le tenant). Troisième gateway "log seulement" du même genre —
consolidation explicitement prévue en 2.5.
**Problèmes rencontrés / points de vigilance :** Aucun nouveau. Ajout de
`UserRepository#findAllByActiveTrue` (nécessaire pour diffuser une annonce).
**Prochaine étape :** ROADMAP.md Phase 2.5 (infrastructure de notifications — consolidation
des gateways existants) puis 2.6 (dashboard Super-Admin).
