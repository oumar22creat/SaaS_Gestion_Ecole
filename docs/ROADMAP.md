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
- [x] Mettre en place le pipeline GitHub Actions de base (build + tests sur chaque push)
- [x] Configurer les environnements dev / staging / prod (fichiers de config séparés)
- [x] Définir les design tokens (couleurs neutres, typographie, espacement) — voir `docs/DESIGN.md`
- [x] Implémenter le chargement dynamique du branding tenant (logo + couleurs) au démarrage Web et Mobile
- [x] Maquetter (même basse fidélité) les 6-7 écrans clés listés dans `docs/DESIGN.md` §5

## Phase 1 — MVP SaaS

### 1.1 Fondations multi-tenant
- [x] Table `tenants` (établissements)
- [x] Colonne `school_id` sur toutes les tables métier + contrainte FK
- [x] Activer PostgreSQL Row-Level Security sur ces tables
- [x] Middleware/filtre Spring qui résout le tenant courant (sous-domaine ou header) et
      l'injecte dans le contexte de requête
- [x] Hibernate Filter global appliquant `school_id` automatiquement
- [x] Tests d'isolation : un utilisateur du tenant A ne doit rien voir du tenant B

### 1.2 Authentification et rôles
- [x] Table `users` (rôle fixe par utilisateur via énumération, pas de tables `roles`/
      `permissions` dynamiques — simplification MVP assumée, voir ADR-008)
- [ ] Inscription / connexion avec JWT (access token + refresh token) — connexion faite,
      inscription = tâche 1.3 (onboarding)
- [x] RBAC appliqué côté serveur sur chaque endpoint (`@PreAuthorize`, à reconduire sur
      chaque nouvel endpoint)
- [x] Rôle Super-Administrateur (hors tenant, gestion de la plateforme)
- [x] Rôles Administrateur, Direction, Enseignant, Élève, Parent, Vie scolaire,
      Secrétaire, Comptable

### 1.3 Onboarding self-service
- [x] Formulaire public d'inscription d'un établissement (Web) — `POST /api/v1/tenants/register`
- [x] Création automatique du tenant + compte Administrateur initial (connexion immédiate)
- [ ] Assistant de configuration (wizard) : établissement → classes/matières →
      import élèves/parents (CSV) → enseignants → emploi du temps → activation
      — dépend des modules 1.5 (classes/matières/élèves) et 1.6 (emploi du temps),
      pas encore construits ; à compléter étape par étape une fois ces modules prêts

### 1.4 Abonnement (base)
- [x] Tables `plans`, `subscriptions`, `invoices`
- [x] Essai gratuit automatique à la création du tenant
- [x] Intégration Stripe : checkout + webhook de confirmation de paiement
- [x] Blocage progressif en cas d'échec de paiement (lecture seule puis suspension)

### 1.5 Élèves, parents, enseignants, classes, matières
- [x] CRUD élèves (avec import CSV en masse)
- [x] CRUD parents/tuteurs + association aux élèves
- [x] CRUD enseignants
- [x] CRUD classes et matières + affectation enseignant/classe/matière

### 1.6 Emploi du temps
- [x] CRUD emploi du temps (classe, enseignant, matière, salle, horaire)
- [x] Détection des conflits (salle/enseignant/classe déjà occupés)

### 1.7 Absences
- [x] Feuille d'appel (interface rapide, pensée mobile)
- [x] Motifs, justificatifs, historique
- [x] Notification au parent

### 1.8 Notes
- [x] CRUD évaluations et notes
- [x] Calcul automatique des moyennes (par matière, par classe)

### 1.9 Dashboard établissement
- [x] Statistiques de base : effectifs, taux de présence, moyennes

**Critère de sortie de Phase 1** : un établissement peut s'inscrire seul, configurer ses
classes/élèves/enseignants, gérer emploi du temps + absences + notes, et payer un abonnement —
le tout sans qu'aucune donnée ne fuite vers un autre tenant.

**État au 2026-09-14** : les 9 sous-phases sont cochées — API backend complète et testée
(isolation multi-tenant comprise), **et** un frontend Web (Angular Material) construit et
vérifié de bout en bout contre un vrai backend (Postgres réel, rôle applicatif restreint,
pas seulement Testcontainers en superutilisateur) pour tous les modules 1.5-1.9 : Élèves
(+ import CSV), Parents (+ association), Enseignants, Classes (+ affectation matière),
Matières, Emploi du temps (+ salles), Absences (feuille d'appel + historique), Notes
(évaluations + saisie + moyennes), Dashboard établissement. Connexion/déconnexion, garde de
route et intercepteur HTTP (jeton + 401) ajoutés à cette occasion.

**Écart restant, explicitement différé** : le mockup "Feuille d'appel" (docs/MOCKUPS.md §1)
prévoyait Mobile/Ionic — livré ici en Web à la place, car l'app Mobile n'a **aucune
authentification** construite (seul le branding l'est, voir Phase 0). Construire l'auth
Mobile est un prérequis plus large, non demandé explicitement dans les tâches 1.5-1.9.
De même, les mockups "Parent — dashboard enfants" et "Élève — emploi du temps" restent hors
portée : ils supposent un portail élève/parent (compte `User` lié à un `Student`/`Parent`),
explicitement différé depuis l'ADR-010 (Phase 1.5).

**Deux bugs réels découverts en testant contre un vrai backend** (jamais visibles avec
Testcontainers en rôle superutilisateur, qui contourne toujours RLS) — voir ADR-015 dans
docs/ARCHITECTURE.md pour le détail : (1) l'inscription self-service violait la politique
RLS sur `users` avec le rôle applicatif restreint réel ; (2) tout endpoint inexistant
renvoyait 500 au lieu de 404, sans aucun log serveur. Les deux sont corrigés et couverts par
un test de régression qui échoue sans le correctif.

---

## Phase 2 — Pédagogie et communication

### 2.1 Bulletins scolaires
- [x] Génération automatique à partir des notes existantes (module `grade`) : moyennes par
      matière et moyenne générale, coefficients
- [x] Appréciations (par matière, professeur principal) et appréciation générale
- [x] Récapitulatif des absences/retards de la période
- [x] Export PDF
- [ ] Rang et signature électronique : **non demandés dans cette passe** — cahier §12 les
      décrit comme "si activé par l'établissement"/renvoi à la section 27 (légal), pas de
      paramétrage d'établissement construit pour en décider ; à ajouter avec le futur module
      de paramétrage établissement (cahier §6), pas avant

### 2.2 Documents (bibliothèque)
- [x] Upload/consultation de documents par matière, classe ou service
- [x] Droits de consultation par rôle
- [x] Association à un cours/devoir (référencé par les modules 2.3/2.4)
- [x] Archivage/suppression
- [ ] Stockage objet S3-compatible avec quota par tenant : **stockage local en dev/MVP**
      (pas de credentials S3 disponibles) derrière une interface `StorageGateway`, même
      pattern que `StripeGateway`/`ParentNotificationGateway` — bascule S3 sans changer les
      appelants

### 2.3 Cahier de textes et devoirs
- [ ] Contenu de séance + travail à faire, dates de publication/limite
- [ ] Pièces jointes (référencent le module 2.2)
- [ ] Consultation élèves/parents : **hors périmètre** — aucun portail élève/parent construit
      (ADR-010), consultation limitée aux rôles staff comme le reste de l'application
- [ ] Notification nouveau devoir (même pattern gateway que les absences, ADR-012)

### 2.4 Messagerie interne
- [ ] Messages individuels et de groupe entre utilisateurs staff
- [ ] Annonces de l'établissement
- [ ] Pièces jointes (référencent le module 2.2)
- [ ] Confirmation de lecture
- [ ] Historique et recherche des conversations

### 2.5 Notifications (infrastructure)
- [ ] Registre centralisé des événements de notification (nouvelle note, absence/retard déjà
      fait en 1.7, nouveau devoir, nouveau document, message reçu, annonce, alertes
      abonnement) — généralise le pattern `ParentNotificationGateway` de l'ADR-012
- [ ] Firebase Cloud Messaging réel : **non câblé** (pas de credentials FCM) — implémentation
      par défaut en log, comme pour les absences ; brancher FCM reste un remplacement
      d'implémentation, pas un changement d'appelants

### 2.6 Dashboard Super-Admin
- [ ] Nombre d'établissements actifs/en essai/suspendus/résiliés
- [ ] Revenu récurrent mensuel (MRR) et annuel (ARR)
- [ ] Taux de churn et taux de conversion essai → abonnement payant
- [ ] Écran Web réservé au rôle Super-Administrateur

**Critère de sortie de Phase 2** : mêmes garde-fous que la Phase 1 — chaque module testé
(y compris isolation cross-tenant), documenté, et son périmètre réel (vs. différé) explicite
avant de passer à la Phase 3.

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
