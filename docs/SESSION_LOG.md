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

## [2026-09-14] — Session (Phase 2.5 — infrastructure de notifications)
**Tâche(s) réalisée(s) :** Nouveau package `notification/` : `NotificationType` (liste
fermée), `NotificationDispatcher` (point d'appel unique), `NotificationGateway`/
`LoggingNotificationGateway`, `NotificationPreference` (table `notification_preferences`,
`school_id` + RLS) + `GET/PUT /api/v1/notification-preferences`. Suppression des trois
gateways ad hoc (`ParentNotificationGateway`, `HomeworkNotificationGateway`,
`MessageNotificationGateway`) et migration de `AttendanceService`/`LessonService`/
`MessagingService` vers le registre unique.
**Décisions prises (et pourquoi) :** Voir ADR-020 (nouveau) — résumé : préférences par
utilisateur (pas par rôle, plus simple avec le modèle `User` existant), absence de ligne =
activé par défaut. Événements sans compte utilisateur réel (absence, devoir — élèves/parents
sans portail, ADR-010) : `recipientUserIds` vide, filtrage par préférence en no-op,
comportement de log inchangé. Un message dans une conversation-annonce déclenche maintenant
`ANNOUNCEMENT` plutôt que `NEW_MESSAGE` (types distincts, opt-out séparé possible).
`NEW_GRADE`/`NEW_DOCUMENT`/`SUBSCRIPTION_ALERT` : type prêt côté registre, mais aucun
appelant ne les déclenche encore (non demandé dans cette tâche).
**Problèmes rencontrés / points de vigilance :** Aucun nouveau. Tests existants
(`AttendanceTest`, `LessonTest`) migrés du mock de l'ancienne gateway spécifique vers un mock
de `NotificationGateway` avec `argThat` sur le type/corps de l'événement. Isolation
cross-tenant testée au niveau du filtre Hibernate (comme `TenantIsolationTest`), pas via un
endpoint — `notification-preferences` n'expose aucun paramètre id/tenant côté client (toujours
scopé à l'utilisateur courant du JWT), donc pas de surface d'attaque cross-tenant côté API.
Suite complète : 85/85 tests passent.
**Prochaine étape :** ROADMAP.md Phase 2.6 (dashboard Super-Admin — dernière sous-phase de la
Phase 2).

## [2026-09-14] — Session (Phase 2.6 — dashboard Super-Admin)
**Tâche(s) réalisée(s) :** `PlatformDashboardService`/`PlatformDashboardController`
(`GET /api/v1/admin/dashboard/summary`, réservé `SUPER_ADMIN`) : établissements par statut,
MRR/ARR (somme des prix des plans des abonnements `ACTIVE`), taux de churn/conversion.
**Décisions prises (et pourquoi) :** Voir ADR-021 (nouveau) — résumé : churn/conversion sont
des taux cumulés depuis l'origine, pas des cohortes par période (aucun historique
d'événements d'abonnement conservé, construire cet historique est hors périmètre et
disproportionné pour ce MVP). Écran Web Super-Admin explicitement différé : l'endpoint
backend est prêt et testé, mais aucun frontend ne le consomme encore.
**Problèmes rencontrés / points de vigilance :** Aucun nouveau. Test d'accès RBAC ajouté
(un utilisateur staff standard reçoit 403 sur cet endpoint). Suite complète : 87/87 tests
passent.
**Prochaine étape :** Phase 2 backend entièrement terminée et testée. Deux sujets restent
explicitement ouverts avant la Phase 3 (voir docs/ARCHITECTURE.md "Points ouverts") : l'écran
Web Super-Admin et l'intégration FCM réelle. Sinon, passer à ROADMAP.md Phase 3 (à détailler
en tâches fines, comme fait pour les Phases 1 et 2 avant de les démarrer).

## [2026-09-14] — Session (Phase 3 détaillée en sous-tâches + Phase 3.1 — vie scolaire)
**Tâche(s) réalisée(s) :** Détail de ROADMAP.md Phase 3 en sous-phases 3.1 à 3.7 (comme fait
pour les Phases 1 et 2 avant de les démarrer), avec deux points bloquants identifiés et
documentés (fournisseur mobile money pour 3.3, dépendance portail parent pour 3.4). Puis
implémentation de 3.1 (vie scolaire) : nouveau package `discipline` — incidents (multi-élèves),
sanctions rattachées à un incident+élève, convocations, observations, historique par élève,
statistiques par classe/période.
**Décisions prises (et pourquoi) :** Voir ADR-022 (nouveau) — résumé : "Exclusions" du cahier
§17 = un type de sanction (`EXPULSION`), pas une entité séparée ; RBAC différencié (`TEACHER`
peut constater/déclarer, seuls `ADMIN`/`DIRECTION`/`VIE_SCOLAIRE` décident une sanction ou une
convocation) ; notification automatique aux parents explicitement hors périmètre (pas de
compte parent, ADR-010).
**Problèmes rencontrés / points de vigilance :** Aucun nouveau. Deux sous-phases suivantes
(3.3 Comptabilité/mobile money, 3.4 Cantine) ont un point bloquant nécessitant une décision de
l'utilisateur avant de commencer — ne pas deviner silencieusement un fournisseur de paiement
ou improviser un portail parent (voir docs/ARCHITECTURE.md "Points ouverts"). Suite complète :
90/90 tests passent.
**Prochaine étape :** ROADMAP.md Phase 3.2 (statistiques avancées) — aucun point bloquant,
peut démarrer directement. Poser les questions bloquantes de 3.3/3.4 à l'utilisateur avant
de les aborder.

## [2026-09-14] — Session (Phase 3.2 — statistiques avancées)
**Tâche(s) réalisée(s) :** `AdvancedStatisticsService`/`Controller` —
`GET /api/v1/statistics/advanced/results-evolution[.csv]` (moyenne par mois calendaire, classe
ou classe+matière). Nouvelle table plateforme `notification_log` (non RLS, comme
subscriptions/invoices) alimentée par `NotificationDispatcher` à chaque envoi réel, exposée
comme `notificationsSentCount` dans le dashboard Super-Admin (2.6/ADR-021).
**Décisions prises (et pourquoi) :** Voir ADR-023 (nouveau) — résumé : regroupement par mois
calendaire plutôt que par trimestre (aucune période pédagogique paramétrable n'existe,
ADR-010) ; CSV = seul format d'export construit. "Stockage utilisé"/"utilisateurs actifs"
cross-tenant (cahier §18) **non implémentés** : les tables sources (`documents`, `users`)
sont RLS-protégées par tenant à raison — un total agrégé plateforme demanderait soit des
compteurs dénormalisés soit un rôle `BYPASSRLS`, une décision qui touche l'invariante RLS du
projet et doit être posée à l'utilisateur, pas tranchée seul.
**Problèmes rencontrés / points de vigilance :** Aucun nouveau. Suite complète : 92/92 tests
passent.
**Prochaine étape :** ROADMAP.md Phase 3.5 (bibliothèque) ou 3.6 (transport scolaire) — aucun
point bloquant sur ces deux-là, à choisir dans cet ordre plutôt que 3.3/3.4 qui restent
bloquées sur une décision utilisateur (fournisseur mobile money, portail parent).

## [2026-09-14] — Session (déblocage 3.3/3.4 + Phase 3.3 — comptabilité et frais scolaires)
**Tâche(s) réalisée(s) :** Question posée à l'utilisateur sur les deux points bloquants
identifiés en fin de session précédente. Réponses : (1) fournisseur mobile money — décision
reportée, construire 3.3 sans paiement en ligne réel ; (2) réservation cantine — saisie côté
staff pour cette passe, pas de portail parent. Puis implémentation de 3.3 : nouveau package
`schoolfees` — grille tarifaire par classe, génération de factures en masse (élèves actifs),
paiements partiels/totaux avec recalcul automatique du statut, reporting consolidé par
classe/période.
**Décisions prises (et pourquoi) :** Voir ADR-024 (nouveau) — résumé : package nommé
`schoolfees` (le cahier §26 ne nomme pas ce module, et `billing` désigne déjà la facturation
SaaS plateforme — mélanger les deux serait confus) ; pas de concept de "niveau" distinct de
la classe ; `FeePaymentMethod.MOBILE_MONEY` existe pour tracer un paiement reçu hors-ligne,
sans gateway réel branché.
**Problèmes rencontrés / points de vigilance :** Aucun nouveau. Suite complète : 94/94 tests
passent.
**Prochaine étape :** ROADMAP.md Phase 3.4 (cantine, maintenant débloquée — saisie staff) puis
3.5 (bibliothèque) et 3.6 (transport scolaire).

## [2026-09-14] — Session (Phase 3.4 — cantine)
**Tâche(s) réalisée(s) :** Nouveau package `canteen` — menus du jour (+ variante régime
particulier), réservations de repas saisies par le personnel (`SECRETARY`/`ADMIN`/`DIRECTION`),
facturation à la consommation réelle (comptage des réservations × prix donné à la génération),
paiements partiels/totaux, suivi des impayés (vue à plat tous élèves).
**Décisions prises (et pourquoi) :** Voir ADR-025 (nouveau) — résumé : pas de tarif persisté
(cahier §19.1 dit explicitement "liée à la consommation réelle", donc calculé à la demande) ;
réutilise `FeePaymentMethod` du package `schoolfees` plutôt que de dupliquer un enum identique ;
`reservedByUserId` porte toujours un compte staff, jamais parent.
**Problèmes rencontrés / points de vigilance :** Aucun nouveau. Suite complète : 96/96 tests
passent.
**Prochaine étape :** ROADMAP.md Phase 3.5 (bibliothèque) puis 3.6 (transport scolaire) — deux
derniers modules complémentaires sans point bloquant avant 3.7 (domaine personnalisé/branding
avancé).

## [2026-09-14] — Session (Phase 3.5 — bibliothèque)
**Tâche(s) réalisée(s) :** Nouveau package `library` — catalogue d'ouvrages, emprunts/retours,
réservations/liste d'attente FIFO (libérée automatiquement au retour), et
`LibraryOverdueReminderJob` (relances automatiques quotidiennes pour les retards, via le
registre centralisé de notifications — nouveau `NotificationType.LIBRARY_OVERDUE`).
**Décisions prises (et pourquoi) :** Voir ADR-026 (nouveau) — résumé : "en retard" calculé à
la lecture (`dueDate < aujourd'hui`), pas un statut stocké ; c'est le premier job du projet à
devoir interroger une table métier RLS-protégée pour tous les tenants (contrairement à
`TenantAccessLifecycleJob`, qui ne touche que des entités plateforme sans RLS) — résolu en
itérant tenant par tenant avec `TenantContext`/`TenantSessionConfigurer`, jamais par
contournement RLS. Répond concrètement à la question laissée ouverte par l'ADR-023.
**Problèmes rencontrés / points de vigilance :** Piège de test repéré et corrigé avant
commit : le premier jet du test de relance appelait `job.runFor(...)`, qui parcourt TOUS les
tenants de la suite (partagés via le conteneur Testcontainers "singleton") — sous la suite
complète, cela déclenchait des dizaines d'appels `send()` d'autres tenants, faisant échouer
`verify(...).send(argThat(...))` ("too many actual invocations"). Corrigé en appelant
directement `remindOverdueLoansFor(tenantId, ...)` pour le seul tenant du test. Suite
complète : 99/99 tests passent.
**Prochaine étape :** ROADMAP.md Phase 3.6 (transport scolaire) puis 3.7 (domaine personnalisé
et branding avancé) — derniers items de la Phase 3.

## [2026-09-14] — Session (Phase 3.6 — transport scolaire)
**Tâche(s) réalisée(s) :** Nouveau package `transport` — lignes et arrêts de bus (ordonnés),
affectation d'un élève à un circuit (une seule active à la fois), facturation forfaitaire du
service, paiements partiels/totaux.
**Décisions prises (et pourquoi) :** Voir ADR-027 (nouveau) — résumé : facturation en forfait
périodique (pas "à la consommation" comme la cantine, le cahier §19.2 ne le demande pas) ;
générer une facture exige une affectation active. Troisième duplication quasi identique du
couple Invoice/Payment (après schoolfees en 3.3, canteen en 3.4) — décision explicite de NE
PAS extraire d'abstraction générique maintenant (les trois modules calculent le montant dû
différemment ; une factorisation propre demanderait une conception à part, plus risquée à
faire rétroactivement sur des modules déjà livrés que ce que la tâche justifie), documentée
pour être reconsidérée si un 4ᵉ cas apparaît.
**Problèmes rencontrés / points de vigilance :** Aucun nouveau. Suite complète : 101/101 tests
passent.
**Prochaine étape :** ROADMAP.md Phase 3.7 (domaine personnalisé et branding avancé, plan
Premium) — dernier item de la Phase 3.

## [2026-09-15] — Session (Phase 3.7 — domaine personnalisé, branding avancé, feature flags, templates de notification)
**Tâche(s) réalisée(s) :** Repris une implémentation déjà en cours mais non testée
(`TenantSettingsController`, `PlanFeature*`, migrations V50/V51) : ajouté les tests manquants
(`TenantSettingsTest`, `PlanFeatureInterceptorTest`) exigés par CLAUDE.md règle 4, ce qui a
révélé un bug réel — les mutations de branding/bulletin/domaine personnalisé n'étaient jamais
persistées (setters appelés hors transaction, sans `save()`). Corrigé en extrayant
`TenantSettingsService` (`@Transactional` + `save()` explicite, même convention que
`TenantRegistrationService`/`SubscriptionService`) ; le contrôleur ne fait plus que déléguer.
Puis terminé le dernier item de la Phase 3.7 : templates de notification/e-mail
personnalisables par établissement (`NotificationTemplate`, migration V52,
`NotificationTemplateController/Service`, intégré à `NotificationDispatcher` sans changer sa
signature côté appelants), avec `NotificationTemplateTest` couvrant RBAC, validation du
placeholder `{message}`, et l'effet réel sur un envoi (via `LibraryOverdueReminderJob`).
**Décisions prises (et pourquoi) :** Voir ADR-028 (nouveau) — résumés : `GET /branding` public
et non enveloppé dans `ApiResponse` (compatibilité avec le contrat frontend déjà écrit,
`TenantBrandingService.init()`) ; feature flags par plan traités comme "inclus dès abonnement
actif sur ce plan", pas d'achat à la carte ; `PlanFeatureInterceptor` fail-closed sur
`/api/v1/{canteen,transport,library}` (a nécessité de mettre à jour
`TestAuthSupport`/CanteenTest/LibraryTest/TransportTest pour accorder explicitement le plan
Premium aux tenants de test, sinon 403).
**Problèmes rencontrés / points de vigilance :** Le bug de persistance ci-dessus n'aurait pas
été détecté sans écrire les tests d'abord — les endpoints répondaient 200 avec les bonnes
valeurs en mémoire (donc "semblaient" fonctionner en test manuel superficiel), seule une
relecture (GET après PUT, requête séparée) le révélait. Suite complète : 109/109 tests
passent.
**Prochaine étape :** Phase 3 terminée (tous les items non cochés restants sont des exclusions
de périmètre déjà documentées, voir ROADMAP.md). Prochaine session : détailler la Phase 4 en
tâches fines (cahier §26/§30), en commençant par le point le plus structurant à trancher avec
l'utilisateur (hébergement de production, cloud/région — voir ARCHITECTURE.md "Points
ouverts").

## [2026-09-16] — Session (test manuel Phase 3.7 + correction bug login/RLS)
**Tâche(s) réalisée(s) :** Test manuel de bout en bout de la Phase 3.7 (branding, domaine
personnalisé avec upgrade Premium via SQL, templates de notification avec effet réel observé
dans les logs sur un vrai événement d'absence). En cours de route, découvert que
`POST /api/v1/auth/login` échouait de façon reproductible contre un vrai Postgres avec rôle
applicatif restreint (fonctionnait par accident avec le superutilisateur Testcontainers).
Corrigé : `AuthService.login`/`resolvePrincipal` résolvent et appliquent désormais
explicitement le tenant (via un nouveau champ `subdomain`, DTO `TenantLoginRequest`) AVANT
toute recherche dans `users` (RLS), au lieu de dépendre du contexte laissé par une requête
précédente sur la même connexion poolée. Voir ADR-029 (nouveau) pour le détail complet.
**Décisions prises (et pourquoi) :** Ajout d'un champ sous-domaine au formulaire de connexion
(Web) plutôt qu'e-mail unique globalement (casserait cahier §21) ou contournement dev-only
(laisserait le bug réel pour le Mobile en production, qui tape une URL absolue fixe sans
sous-domaine par tenant) — décision utilisateur explicite après présentation des trois options.
**Problèmes rencontrés / points de vigilance :** Bug invisible en tests automatisés car
Testcontainers utilise par défaut un rôle superutilisateur Postgres, qui contourne TOUJOURS
Row-Level Security (ADR-001 Piège 4) — seul un test dédié avec un rôle restreint
(`AuthRlsTest`, nouveau) peut le détecter, comme `TenantRegistrationRlsTest` l'avait déjà fait
pour l'inscription. Suite complète : 110/110 tests backend, 38/38 tests Web passent.
**Prochaine étape :** Reprendre le test manuel Phase 3.7 (effet du placeholder `{message}` du
corps de template de notification, maintenant que `LoggingNotificationGateway` logue aussi le
corps) avec le login désormais fonctionnel. Puis Phase 4 (inchangé).

## [2026-09-17] — Session (authentification Mobile)
**Tâche(s) réalisée(s) :** Construit l'authentification Mobile (Ionic), en miroir du Web,
comblant l'écart documenté depuis la Phase 1.5-1.9 : `AuthTokenService`, `AuthService`
(login avec `subdomain`/`email`/`password`, contrat aligné sur ADR-029), `authGuard`,
`authInterceptor` (jeton Bearer + déconnexion au premier 401 authentifié), écran de connexion
Ionic (`ion-card`/`ion-input`/`ion-button`, composants natifs — docs/DESIGN.md §3), et un
écran d'accueil minimal (`HomePage`, email + déconnexion) pour avoir une route protégée à
tester de bout en bout. Utilisateur a choisi de démarrer par l'auth seule (pas les 9 modules
métier du Web, pas encore les 2 écrans mobile-first identifiés par docs/DESIGN.md) —
prochaine tâche à cadrer séparément.
**Décisions prises (et pourquoi) :** Code dupliqué (pas partagé) entre `web/` et `mobile/`
pour `auth-token.service.ts`/`jwt.util.ts`/`http-error.util.ts`/`api-response.model.ts`/
`auth.guard.ts`/`auth.interceptor.ts` — même principe déjà assumé pour les design tokens
(voir `mobile/src/theme/variables.scss`) : pas de package partagé entre les deux workspaces
Angular pour l'instant.
**Problèmes rencontrés / points de vigilance :** Aucun — le correctif login/RLS (ADR-029,
session précédente) a été fait juste avant, donc `TenantLoginRequest(subdomain, email,
password)` était déjà le bon contrat à répliquer côté Mobile dès le départ. Suite complète :
18/18 tests Mobile passent (nouveaux : `auth.guard.spec`, `auth.interceptor.spec`,
`login.page.spec`, `home.page.spec`).
**Prochaine étape :** Cadrer avec l'utilisateur quels écrans métier Mobile construire ensuite
— candidats naturels selon docs/DESIGN.md §5 (mobile-first) : Feuille d'appel et Saisie de
notes (rôle Enseignant).

## [2026-09-17] — Session (produit opérationnel Web + Mobile)
**Tâche(s) réalisée(s) :** Frontend staff des Phases 2–3 branché sur les APIs existantes
(bulletins, documents, cahier de textes, messagerie, notifications, discipline, stats
avancées, frais, cantine, bibliothèque, transport, abonnement, paramètres) + console
Super-Admin (`/admin`) + Mobile opérationnel (accueil tuiles 🏫, feuille d'appel ✅,
notes 📝, emploi du temps 🗓️). Design école (teal `#0f5c4c` / or `#c9a227`, canvas
crème) et emojis fixes par module (docs/DESIGN.md §9).
**Décisions prises (et pourquoi) :** Pas de portail élève/parent (ADR-010). Pas de Phase 4
(QR, e-sign, K8s). Accueil Web par rôle (`firstPathForRole`) pour ne pas envoyer un
enseignant sur le dashboard Direction. Emojis comme identifiants visuels, pas une
nouvelle bibliothèque d'icônes.
**Problèmes rencontrés / points de vigilance :** `GET /users` est ADMIN/DIRECTION — le
sélecteur de destinataires messagerie échoue en douceur pour les autres rôles. FCM/S3/
mobile money restent hors périmètre (gateways log / stockage local).
**Prochaine étape :** Phase 4 seulement si demandée ; sinon raffiner les écrans staff
après usage réel (reçus PDF, rang bulletin, FCM).

## [2026-09-20] — Vérification RBAC + tests par rôle
**Tâche(s) réalisée(s) :** Audit de la gestion des rôles (cahier §5 / ADR-008 / ADR-010) et
tests ciblés : matrice API des 8 rôles établissement + Super-Admin ; gardes Web
(`roleGuard`, `tenantWebGuard`) pour ADMIN vs SUPER_ADMIN ; accueil Mobile différencié
PARENT / TEACHER / STUDENT. Correction : lecture emploi du temps ouverte à `TEACHER`
(l'UI mobile l'affichait déjà, l'API renvoyait 403). Les routes Web staff ne sont plus
accessibles par simple URL hors rôle.
**Décisions prises (et pourquoi) :** Pas de portail parent/élève (ADR-010 inchangé) — PARENT
et STUDENT s'authentifient, voient un espace mobile de suivi, et restent 403 sur les APIs
staff. Super-Admin reste hors shell établissement (`/admin` uniquement).
**Problèmes rencontrés / points de vigilance :** Le JWT client ne fait qu'afficher/rediriger ;
`@PreAuthorize` reste la source de vérité. Lier `User` à une fiche `Parent`/`Student` est
toujours hors périmètre.
**Prochaine étape :** Portail parent/élève seulement si le porteur lève ADR-010.

## [2026-09-20] — Session (socle design + création des comptes du personnel)
**Tâche(s) réalisée(s) :** (1) Remplacement des emojis par **Material Symbols Rounded** dans
tout le Web (175 occurrences au départ, 0 restante) : barre latérale, 26 en-têtes de page,
cartes du tableau de bord, libellés de boutons, puces d'authentification et pseudo-éléments
CSS d'état. Police déclarée une seule fois via `MatIconRegistry.setDefaultFontSetClass` dans
`app.config.ts`. (2) `.empty-state` enrichi (titre + explication + action) sur les écrans
d'amorçage, et nouveau `.loading-state` en silhouette animée. (3) **Création des comptes du
personnel** : `POST /api/v1/users` + écran `/accounts`.
**Décisions prises (et pourquoi) :** `docs/DESIGN.md` §9 réécrit — la décision « emojis comme
identifiants visuels » est annulée, avec ses trois raisons : rendu dépendant du système,
couleurs non surchargeables (contradiction directe avec la contrainte white-label §0), et
graisses incohérentes. Décision validée explicitement par le porteur avant exécution. Rôles
Élève/Parent non attribuables à la création de compte : aucun portail ne leur est construit
(ADR-010), un tel compte n'aurait aucun écran.
**Problèmes rencontrés / points de vigilance :** Découvert en voulant tester chaque profil que
`TenantRegistrationService` était le **seul** code créant un `User` : la base ne contenait que
des `ADMIN`. Tout le RBAC (barre latérale filtrée, `roleGuard`, `firstPathForRole`) était écrit
et testé unitairement mais ne pouvait jamais s'exécuter en vrai, et l'application mobile,
conçue pour l'enseignant, n'avait aucun utilisateur possible. D'où la fonctionnalité (3).
Deux autres défauts corrigés au passage : le chargement du tableau de bord était rendu comme un
état vide (bordure pointillée, icône « boîte vide » alors que les données arrivaient), et
`setDefaultFontSetClass` retourne le registre, ce qui cassait `provideAppInitializer` en
silence (build rouge, ancien bundle servi).
**Tests :** 118/118 backend (dont `UserCreationTest`, 5 cas avec isolation cross-tenant),
54/54 Web. Les 6 profils (Administration, Direction, Enseignant, Secrétariat, Vie scolaire,
Comptabilité) ont été testés en conditions réelles dans le navigateur : page d'accueil par rôle
et barre latérale filtrée conformes.
**Prochaine étape :** Socle mobile (39 emojis restants, tokens Ionic), puis tableaux et
formulaires. L'écran `/accounts` ne gère que la création : désactivation, changement de rôle et
réinitialisation de mot de passe restent à faire.

## [2026-09-21] — Session (socle design mobile)
**Tâche(s) réalisée(s) :** Socle mobile en miroir du web : 45 emojis remplacés par des
**Ionicons** (0 restant), enregistrés une seule fois via `addIcons(APP_ICONS)` dans
`mobile/src/app/app.config.ts`. Champs `emoji` renommés en `icon` dans `HomeTile`
(`core/role-access.ts`) et `ATTENDANCE_STATUSES` (`attendance/attendance.model.ts`).
`IonIcon` ajouté aux 7 composants concernés. Médailles d'icônes stylées globalement dans
`styles.scss` (couleur du tenant, taille par contexte).
**Décisions prises (et pourquoi) :** Ionicons plutôt que Material Symbols sur mobile, alors
que le web utilise Material Symbols. Raison : Material Symbols est une police téléchargée
depuis Google Fonts au démarrage ; dans une application Capacitor utilisée dans une salle mal
couverte, l'enseignant verrait les libellés d'icônes à la place des icônes. Ionicons est
embarqué dans le paquet, donc hors ligne. `docs/DESIGN.md` §9 amendé en conséquence : un jeu
par application, pas un jeu pour les deux. Icônes retirées des `<ion-title>` : un titre de
barre mobile se lit mieux en texte seul.
**Problèmes rencontrés / points de vigilance :** Défaut de mise en page découvert en vérifiant
le rendu : `ion-router-outlet` est positionné par Ionic en `absolute; top: 0`, alors que
l'en-tête de marque vit dans `app.html`, **hors** du router-outlet. Le haut de *chaque* écran
mobile passait donc sous l'en-tête de 54px — visible sur la connexion, dont le médaillon était
rogné. Corrigé à la racine (`ion-app` en colonne, outlet en `flex: 1`), pas écran par écran.
Le défaut préexistait au passage aux icônes.
**Tests :** 29/29 mobile. Vérifié en viewport 390x844 contre un vrai backend avec un compte
Enseignant réel : connexion, accueil (tuiles), feuille d'appel.
**Prochaine étape :** Tableaux et formulaires (web), et sur `/accounts` la désactivation d'un
compte, le changement de rôle et la réinitialisation de mot de passe.

## [2026-09-21] — Session (gestion des comptes + primitives de tableau)
**Tâche(s) réalisée(s) :** Écran `/accounts` complété : désactivation/réactivation, changement
de rôle et réinitialisation de mot de passe (`PUT /users/{id}/status|role|password`, réservés
à ADMIN/DIRECTION). Colonne « Accès » avec badge d'état, ligne atténuée pour un compte
désactivé, menu d'actions par ligne. Côté primitives partagées : `.status-badge`
(`.status-ok`/`.status-off`) et `.table-scroll`, ce dernier appliqué aux 22 tableaux du Web.
**Décisions prises (et pourquoi) :** Les rôles Élève et Parent restent refusés à la création
et au changement de rôle — décision explicite du porteur, le temps que le portail existe ;
créer ces comptes aujourd'hui mènerait à un accueil mobile sans fonction. La réinitialisation
n'invalide pas les jetons déjà émis (15 minutes de validité résiduelle) : c'est documenté dans
le service, à revoir si le besoin de révocation immédiate apparaît.
**Problèmes rencontrés / points de vigilance :** J'avais écrit un garde-fou « dernier
administrateur » (interdire de retirer le dernier compte capable d'administrer) **et** une
garde anti-auto-ciblage. En écrivant le test, constaté que le premier est inatteignable :
l'auteur de l'action est forcément un administrateur actif et ne peut pas se cibler lui-même,
donc le compte d'administrateurs actifs vaut toujours au moins 2 quand la vérification
s'exécute. Le garde-fou et sa requête de dépôt ont été supprimés plutôt que livrés avec un
test qui ne les exerçait pas. La garde anti-auto-ciblage suffit et est testée.
Autre point : `.table-scroll` corrige un débordement réel — sans conteneur, un tableau à 5
colonnes faisait défiler la page entière, barre latérale comprise, sur écran étroit.
**Tests :** 122/122 backend (9 dans `UserCreationTest`), 54/54 Web. Vérifié dans le navigateur
contre le vrai backend : désactivation d'un enseignant puis connexion refusée (401),
auto-désactivation refusée (422), rôle Élève refusé (422), réactivation (200).
**Prochaine étape :** Portail parent/élève (lien `User ↔ Student/Parent`, endpoints filtrés
« mes enfants / moi », écrans mobiles), puis passe complète sur les formulaires Web.

## [2026-09-21] — Session (portail parent/élève)
**Tâche(s) réalisée(s) :** Migration V54 (`students.user_id`, `parents.user_id`, index uniques
partiels par tenant). `UserService.createFamilyAccount` + `POST /students/{id}/account` et
`POST /parents/{id}/account` pour ouvrir un accès. `PortalService` / `PortalController` :
`GET /portal/children`, `/portal/students/{id}/grades`, `/portal/students/{id}/attendance`.
Mobile : service portail, liste des enfants, dossier élève (onglets Notes / Absences), tuiles
d'accueil Parent et Élève désormais actives, règle `/portal` dans `canAccessMobilePath`.
**Décisions prises (et pourquoi) :** (1) Les rôles PARENT/STUDENT ne sont **pas** attribuables
via `POST /users` ; ils ne s'obtiennent qu'en ouvrant un accès depuis une fiche existante.
Un compte parent sans enfant rattaché devient impossible par construction. (2) Un accès refusé
répond 404 et non 403 : répondre "interdit" confirmerait à l'appelant que cet élève existe
dans l'établissement, ce qui est déjà une fuite. (3) Un élève n'a qu'un dossier : la liste est
court-circuitée et il arrive directement sur son suivi.
**Problèmes rencontrés / points de vigilance :** Le risque propre à ce portail n'est **pas**
l'isolation entre établissements — RLS la garantit déjà — mais l'isolation **entre familles du
même établissement** : deux parents partagent le même tenant, donc seul le contrôle applicatif
empêche l'un de lire le dossier de l'enfant de l'autre. D'où `requireAccessibleStudent`, qui
confronte systématiquement l'identifiant de l'URL à la liste calculée depuis le compte, et le
test `parentSeesOnlyTheirOwnChildren` qui tente explicitement l'accès croisé.
Trois tests mobiles sont tombés : ils figeaient l'ancien état (toutes les tuiles famille en
`path: null`). Assertions réécrites vers le comportement voulu, pas affaiblies.
**Tests :** 126/126 backend (dont `PortalAccessTest`, 4 cas), 29/29 mobile, 54/54 Web.
Vérifié contre un vrai backend avec une famille réelle (élève Fatoumata Sidibe, mère Aminata) :
parcours mobile accueil → Mes enfants → dossier, et accès croisé refusé en 404.
**Prochaine étape :** Emploi du temps de l'élève dans le portail (mockup non couvert), puis
passe complète sur les formulaires Web.

## [2026-09-21] — Session (mise en route guidée + fin du socle)
**Tâche(s) réalisée(s) :** (1) Assistant de configuration, dernier point non coché de la
Phase 1 : `GET /api/v1/onboarding/status` et liste de mise en route en tête du tableau de
bord. (2) Emploi du temps de l'élève dans le portail (`GET /portal/students/{id}/timetable`
+ troisième onglet mobile), qui couvre le dernier mockup famille. (3) Messages d'erreur sous
les champs de formulaire, via `core/form-error.util.ts`.
**Décisions prises (et pourquoi) :** Liste reprenable plutôt qu'assistant modal séquentiel :
un directeur est interrompu en permanence, et une liste qui renvoie vers les écrans existants
évite de dupliquer les formulaires, donc d'avoir deux chemins divergents pour créer une même
donnée. Avancement **déduit des données**, jamais stocké : une école qui supprime toutes ses
classes revoit l'étape, et un établissement configuré avant l'existence de la page apparaît
d'emblée à jour. Message d'erreur centralisé plutôt que réécrit par gabarit, pour qu'une même
règle de validation produise la même phrase partout.
**Problèmes rencontrés / points de vigilance :** Constaté que les 15 formulaires du Web
utilisaient `mat-form-field` sans **aucun** `mat-error` : la saisie invalide ne produisait
qu'une bordure rouge, sans dire quelle contrainte était violée, alors que docs/DESIGN.md §6
l'interdit explicitement. Le script d'insertion a par ailleurs échoué à mi-parcours sur un
fichier sans déclaration de classe, laissant une partie des composants sans la propriété
exposée ; rattrapé en repassant sur les composants dont le gabarit référençait `fieldError`.
**Tests :** backend complet au vert (dont `OnboardingStatusTest` 3 cas et `PortalAccessTest`
porté à 5), 54/54 Web, 29/29 mobile. Vérifié dans le navigateur : liste de mise en route sur
l'établissement de test (1/5, étape Élèves cochée) et messages d'erreur à la soumission d'un
formulaire vide.
**Prochaine étape :** Paiement mobile money (Orange Money), explicitement reporté par le
porteur. Reste aussi la Phase 4 (QR présence, signature électronique, API publique).
