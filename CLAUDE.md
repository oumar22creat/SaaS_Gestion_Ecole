# CLAUDE.md — Mémoire du projet

Ce fichier est lu automatiquement par Claude Code au démarrage de chaque session.
Il doit rester à jour et concis : c'est la "vérité" du projet, pas un journal.

## Projet
Plateforme SaaS multi-établissements de gestion scolaire (type PRONOTE fonctionnellement,
sans reprise de code ni d'identité graphique). Voir `docs/cahier-des-charges.md` pour la
spec fonctionnelle complète.

## Stack technique (ne pas dévier sans mettre à jour ce fichier)
- Frontend web : Angular
- Mobile : Ionic + Capacitor (Android/iOS)
- Backend : Spring Boot (Java)
- Base de données : PostgreSQL — **multi-tenant via colonne `school_id` + Row-Level Security**
- Cache / rate limiting : Redis
- Notifications : Firebase Cloud Messaging
- Paiement : Stripe (international) + un moyen de paiement mobile local (à confirmer)
- Conteneurisation : Docker + Nginx
- CI/CD : GitHub Actions
- Migrations DB : Flyway
- Structure de repo : **mono-repo** (`backend/`, `web/`, `mobile/` dans un seul dépôt GitHub)
- Bibliothèque UI Web : **Angular Material**, personnalisée avec les tokens du tenant (voir `docs/DESIGN.md`)

## Règles non négociables
1. **Toute nouvelle table métier doit avoir une colonne `school_id`** avec FK vers `tenants`,
   et une politique RLS PostgreSQL correspondante. Pas d'exception, pas de "je l'ajouterai plus tard".
2. Aucune requête JPA ne doit pouvoir retourner des données d'un autre tenant, même par erreur
   de code — le filtre tenant doit être appliqué au niveau global (Hibernate Filter), pas
   requête par requête.
3. Jamais de secret, clé API ou mot de passe en dur dans le code. Utiliser des variables d'environnement.
4. Toute fonctionnalité livrée doit avoir au moins un test (unitaire ou intégration) qui la couvre.
5. Ne pas sauter de phase du `docs/ROADMAP.md` : on ne commence pas le module Cantine si le
   module Notes n'est pas terminé et testé.
6. Une tâche = un commit (ou une petite série de commits liés). Pas de commit fourre-tout.
7. Mettre à jour `README.md` immédiatement si la tâche change une commande de démarrage,
   une variable d'environnement ou un prérequis (pas en fin de projet, dans le même commit).

## Documents de référence (à lire selon le besoin, pas tous à chaque fois)
- `docs/cahier-des-charges.md` → spec fonctionnelle complète (la référence pour CE QUE fait le produit)
- `docs/ARCHITECTURE.md` → décisions techniques figées (COMMENT c'est construit)
- `docs/DATA_MODEL.md` → entités, champs clés, relations
- `docs/API_CONVENTIONS.md` → conventions REST, format d'erreurs, auth
- `docs/DESIGN.md` → système de design, tokens, theming multi-tenant, écrans clés par rôle
- `docs/ROADMAP.md` → backlog des tâches, source de vérité de l'avancement
- `docs/SESSION_LOG.md` → historique des sessions passées (décisions, blocages)
- `docs/PROMPTS.md` → prompts prêts à l'emploi pour piloter Claude Code phase par phase
- `README.md` → instructions de démarrage local (clone, docker-compose, lancement des 3 apps)

## Comment démarrer une session de travail
1. Lire `docs/ROADMAP.md` pour identifier la prochaine tâche non cochée.
2. Lire les dernières entrées de `docs/SESSION_LOG.md` pour le contexte récent.
3. Annoncer la tâche choisie avant de coder.
4. Si une décision d'architecture n'est pas déjà tranchée dans `docs/ARCHITECTURE.md`,
   la poser à l'utilisateur plutôt que de trancher seul silencieusement.
5. Une fois la tâche terminée et testée :
   - cocher la case correspondante dans `docs/ROADMAP.md`
   - ajouter une entrée dans `docs/SESSION_LOG.md`
   - committer avec un message clair (`feat(students): ajout CRUD élèves avec filtre tenant`)

## Conventions de code
- Backend organisé par domaine fonctionnel (voir cahier des charges §26) :
  `auth/`, `tenant/`, `billing/`, `student/`, `teacher/`, `schoolclass/`, `timetable/`,
  `attendance/`, `grade/`, `reportcard/`, `homework/`, `document/`, `messaging/`,
  `notification/`, `discipline/`, `statistics/`.
- Chaque module backend : `controller/`, `service/`, `repository/`, `entity/`, `dto/`, `mapper/`.
- Endpoints REST : `/api/{ressource-au-pluriel}`, versionnés `/api/v1/...`.
- Angular : structure par feature module, un module = un domaine métier.
- Tests backend : JUnit + Mockito. Tests frontend : Jest ou Karma/Jasmine.
- **Tout endpoint retournant des données scoped-tenant doit avoir un test vérifiant
  qu'un utilisateur d'un autre tenant ne peut pas y accéder.**
