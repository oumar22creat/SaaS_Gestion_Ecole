# PROMPTS — Prompts prêts à l'emploi pour Claude Code

Un prompt par étape. Copie-colle, ajuste seulement si besoin. Le principe reste le même
partout : une tâche à la fois, référence aux docs plutôt que ré-explication, case cochée +
entrée SESSION_LOG.md à la fin.

## Prompt générique (à réutiliser tout au long du projet)

À utiliser dès que tu ne veux pas chercher un prompt spécifique ci-dessous — remplace
simplement `<TÂCHE>` par la ligne exacte de `docs/ROADMAP.md` :

> Lis CLAUDE.md, docs/ROADMAP.md et les dernières entrées de docs/SESSION_LOG.md.
> Prends uniquement cette tâche : "<TÂCHE>". Dis-moi comment tu comptes t'y prendre avant
> de coder. Une fois terminé et testé, coche la case dans ROADMAP.md et ajoute une entrée
> dans SESSION_LOG.md.

---

## Phase 0 — Setup du projet

> Lis CLAUDE.md et docs/ARCHITECTURE.md. On démarre la Phase 0 de docs/ROADMAP.md.
> Traite les tâches dans l'ordre où elles apparaissent, une par une, en t'arrêtant après
> chacune pour que je valide avant de passer à la suivante :
> 1. squelette Spring Boot (Java, structure par domaine) + docker-compose PostgreSQL/Redis
> 2. configuration Flyway
> 3. squelette Angular avec Angular Material installé (thème neutre par défaut, pas encore
>    de branding tenant)
> 4. squelette Ionic/Capacitor
> 5. pipeline GitHub Actions (build + tests à chaque push)
> 6. fichiers de configuration dev/staging/prod
> Coche chaque case dans ROADMAP.md au fur et à mesure et mets à jour README.md si une
> commande de démarrage change.

## Phase 1.1 — Fondations multi-tenant

> Lis CLAUDE.md, docs/ARCHITECTURE.md (ADR-001) et docs/DATA_MODEL.md.
> Implémente les fondations multi-tenant : table `tenants`, colonne `school_id` sur les
> tables métier déjà créées, activation de PostgreSQL Row-Level Security, middleware Spring
> qui résout le tenant courant (header `X-Tenant-Id` en local, sous-domaine en prod) et
> l'injecte dans le contexte de requête, Hibernate Filter global appliquant `school_id`
> automatiquement. Écris un test qui prouve qu'un utilisateur du tenant A ne peut rien lire
> du tenant B, même en forçant un ID dans l'URL. Ne passe à la suite que si ce test passe.

## Phase 1.2 — Authentification et rôles

> Lis CLAUDE.md et docs/API_CONVENTIONS.md. Implémente `users`, `roles`, `permissions`,
> l'authentification JWT (access + refresh token), le RBAC appliqué côté serveur sur chaque
> endpoint, et les rôles suivants : Super-Administrateur (hors tenant), Administrateur,
> Direction, Enseignant, Élève, Parent, Vie scolaire, Secrétaire, Comptable. Teste qu'un
> rôle ne peut pas accéder à un endpoint hors de son périmètre.

## Phase 1.3 — Onboarding self-service

> Lis docs/cahier-des-charges.md section 20. Implémente le formulaire public d'inscription
> d'un établissement (création automatique du tenant + compte Administrateur initial) et
> l'assistant de configuration (wizard) : établissement → classes/matières → import
> élèves/parents via CSV → enseignants → emploi du temps → activation. Chaque étape doit
> pouvoir être testée indépendamment.

## Phase 1.4 — Abonnement (base)

> Lis docs/ARCHITECTURE.md (ADR-003) et docs/cahier-des-charges.md section 4. Implémente
> `plans`, `subscriptions`, `invoices`, l'essai gratuit automatique à la création du tenant,
> l'intégration Stripe (checkout + webhook), et le blocage progressif en cas d'échec de
> paiement. Utilise le mode test Stripe — ne configure jamais de vraies clés de production.

## Phase 1.5 — Élèves, parents, enseignants, classes, matières

> Implémente les CRUD élèves (avec import CSV en masse), parents/tuteurs (avec association
> aux élèves), enseignants, classes et matières (avec affectation enseignant/classe/matière).
> Respecte docs/API_CONVENTIONS.md pour le format des endpoints et des erreurs.

## Phase 1.6 — Emploi du temps

> Implémente le CRUD emploi du temps (classe, enseignant, matière, salle, horaire) avec
> détection des conflits (salle/enseignant/classe déjà occupés sur le même créneau). Ajoute
> un test qui vérifie qu'un conflit est bien rejeté.

## Phase 1.7 — Absences

> Lis docs/DESIGN.md section 5 (écran feuille d'appel). Implémente la feuille d'appel
> (mobile-first), les motifs et justificatifs, l'historique des modifications, et la
> notification au parent en cas d'absence.

## Phase 1.8 — Notes

> Implémente les CRUD évaluations et notes, le calcul automatique des moyennes (par matière,
> par classe), et la gestion des absences aux évaluations.

## Phase 1.9 — Dashboard établissement

> Implémente le dashboard établissement avec les statistiques de base : effectifs, taux de
> présence, moyennes. Une fois fait, exécute le scénario complet de bout en bout : créer un
> établissement → configurer classes/élèves/enseignants → gérer emploi du temps + absences +
> notes → payer un abonnement. Si tout fonctionne, la Phase 1 est terminée — dis-le moi
> explicitement pour qu'on passe à la Phase 2.

---

## Fin de Phase 1 — avant de démarrer la Phase 2

> On termine la Phase 1. Détaille dans docs/ROADMAP.md, sous la Phase 2, des tâches aussi
> précises que celles de la Phase 1 (actuellement la Phase 2 n'a que des lignes générales).
> Base-toi sur docs/cahier-des-charges.md sections 12 à 16 et 18. Ne code rien pour l'instant,
> propose juste le découpage et attends ma validation.

## Phase 2 (une fois détaillée) — reprendre le prompt générique

> Une fois la Phase 2 détaillée et validée, reprends le **prompt générique** en haut de ce
> fichier, tâche par tâche, comme pour la Phase 1.

## Fin de Phase 2 — avant de démarrer la Phase 3

> On termine la Phase 2. Détaille dans docs/ROADMAP.md, sous la Phase 3, des tâches aussi
> précises que celles de la Phase 1. Base-toi sur docs/cahier-des-charges.md section 19 et
> sur docs/ARCHITECTURE.md (point ouvert : fournisseur mobile money — pose-moi la question
> avant de choisir). Ne code rien, propose le découpage et attends ma validation.

## Fin de Phase 3 — avant de démarrer la Phase 4

> On termine la Phase 3. Détaille dans docs/ROADMAP.md, sous la Phase 4, des tâches aussi
> précises que celles de la Phase 1. Base-toi sur docs/cahier-des-charges.md section 23.
> Avant de commencer, pose-moi les points ouverts restants de docs/ARCHITECTURE.md
> (hébergement de production, Kubernetes ou non) — ce sont des prérequis à cette phase.
> Ne code rien, propose le découpage et attends ma validation.

---

## Prompts transverses (à utiliser à tout moment, hors séquence de phase)

**Reprendre après une pause / ne plus savoir où on en est :**
> Lis docs/ROADMAP.md et les 3 dernières entrées de docs/SESSION_LOG.md. Résume-moi où on
> en est et propose la prochaine tâche à traiter.

**Vérifier qu'aucune fuite multi-tenant ne s'est introduite récemment :**
> Vérifie que tous les endpoints ajoutés depuis la dernière fois filtrent bien par
> `school_id` (Hibernate Filter ou RLS). Liste tout endpoint qui y échapperait.

**Avant un déploiement staging :**
> Relis docs/ARCHITECTURE.md et docs/cahier-des-charges.md section 25. Dis-moi ce qui manque
> avant de pouvoir déployer en staging (variables d'environnement, secrets, migrations,
> pipeline CI) sans encore rien déployer.
