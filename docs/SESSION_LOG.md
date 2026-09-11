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
