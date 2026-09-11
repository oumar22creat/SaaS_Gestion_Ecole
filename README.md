# Plateforme SaaS de Gestion Scolaire

Plateforme SaaS multi-établissements de gestion scolaire (Web + Mobile).
Voir `docs/cahier-des-charges.md` pour la spec fonctionnelle complète et `CLAUDE.md`
pour les règles de développement.

> Ce README doit être tenu à jour à chaque changement de setup (nouvelle variable
> d'environnement, nouvelle commande, nouveau service). Si une commande ci-dessous ne
> fonctionne plus, corrige ce fichier dans le même commit que le changement qui l'a cassée.

## Stack

| Composant | Techno |
|---|---|
| Backend | Spring Boot (Java) |
| Web | Angular + Angular Material |
| Mobile | Ionic + Capacitor |
| Base de données | PostgreSQL |
| Cache | Redis |
| CI/CD | GitHub Actions |

## Structure du repo

```
.
├── backend/          → API Spring Boot
├── web/              → Application Web Angular
├── mobile/           → Application Mobile Ionic/Capacitor
├── docs/             → Documentation du projet (spec, architecture, design, roadmap)
├── CLAUDE.md          → Règles de développement lues par Claude Code
└── docker-compose.yml → Environnement local (PostgreSQL, Redis)
```

## Prérequis

- Java 21+ et Maven (ou le wrapper `./mvnw` fourni dans `backend/`)
- Node.js 22+ et npm (requis par Angular CLI 22)
- Docker et Docker Compose
- Ionic CLI (`npm install -g @ionic/cli`) pour le mobile
- Google Chrome installé (utilisé en mode headless par les tests unitaires Web/Karma)

## Démarrage rapide (environnement local)

### 1. Cloner le repo
```bash
git clone <url-du-repo>
cd <nom-du-repo>
```

### 2. Lancer les services d'infrastructure (PostgreSQL, Redis)
```bash
docker compose up -d
```
Ceci démarre PostgreSQL sur le port `5433` (et non `5432`, pour éviter un conflit avec une
éventuelle installation PostgreSQL native sur la machine) et Redis sur le port `6379`.
Voir `docker-compose.yml` pour les identifiants par défaut (à ne jamais utiliser en production).
Le port PostgreSQL exposé peut être changé via la variable `DB_PORT` dans un fichier `.env`
à la racine du repo.

### 3. Lancer le backend
```bash
cd backend
./mvnw spring-boot:run
```
L'API est disponible sur `http://localhost:8080` (si ce port est déjà utilisé par un autre
projet sur ta machine, lance avec `SERVER_PORT=8090 ./mvnw spring-boot:run`).
Les migrations Flyway s'exécutent automatiquement au démarrage.
Documentation Swagger : `http://localhost:8080/swagger-ui/index.html`.

### 4. Lancer l'application Web
```bash
cd web
npm install
npm start
```
L'application est disponible sur `http://localhost:4200`.

### 5. Lancer l'application Mobile (en mode navigateur pour le développement)
```bash
cd mobile
npm install
ionic serve
```

## Variables d'environnement

Copier `backend/.env.example` vers `backend/.env` et ajuster :

| Variable | Description | Exemple local |
|---|---|---|
| `DB_URL` | URL de connexion PostgreSQL | `jdbc:postgresql://localhost:5433/school_saas` |
| `DB_USER` / `DB_PASSWORD` | Identifiants base de données | voir `docker-compose.yml` |
| `JWT_SECRET` | Clé de signature des tokens | générer une valeur aléatoire, ne jamais committer |
| `REDIS_URL` | URL Redis | `redis://localhost:6379` |
| `STRIPE_SECRET_KEY` | Clé API Stripe (mode test en local) | à récupérer sur le dashboard Stripe |
| `FCM_CREDENTIALS_PATH` | Chemin du fichier de credentials Firebase | voir la console Firebase du projet |

**Ne jamais committer de fichier `.env` réel.** Seul `.env.example` (sans valeurs sensibles)
doit être versionné.

## Tests

```bash
# Backend
cd backend && ./mvnw test

# Web
cd web && npm test

# Mobile
cd mobile && npm test
```

## Multi-tenant en local

Pour tester le multi-tenant en local sans configurer de vrais sous-domaines, utiliser le
header `X-Tenant-Id: <id-etablissement>` sur les appels API (voir `docs/ARCHITECTURE.md`
pour le détail du mécanisme). En production, le tenant est résolu via le sous-domaine.

## Documentation du projet

- [`docs/cahier-des-charges.md`](docs/cahier-des-charges.md) — spec fonctionnelle complète
- [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) — décisions techniques
- [`docs/DATA_MODEL.md`](docs/DATA_MODEL.md) — modèle de données
- [`docs/API_CONVENTIONS.md`](docs/API_CONVENTIONS.md) — conventions REST
- [`docs/DESIGN.md`](docs/DESIGN.md) — système de design
- [`docs/ROADMAP.md`](docs/ROADMAP.md) — backlog du projet (état d'avancement)
- [`docs/SESSION_LOG.md`](docs/SESSION_LOG.md) — historique des sessions de travail
