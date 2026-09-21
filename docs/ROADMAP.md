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
- [x] Gestion des comptes du personnel par l'Administrateur (`POST /api/v1/users` +
      `PUT /users/{id}/status|role|password`, écran `/accounts` : création, désactivation,
      changement de rôle, réinitialisation de mot de passe) — **comble un trou bloquant découvert le 2026-09-20** : seul le compte
      Administrateur créé à l'inscription pouvait exister, donc aucun enseignant ne pouvait se
      connecter et l'application mobile restait inutilisable. Rôles Élève/Parent volontairement
      non attribuables (ADR-010, aucun portail construit)
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

**Écart initial, comblé le 2026-09-17** : le mockup "Feuille d'appel" (docs/MOCKUPS.md §1)
prévoyait Mobile/Ionic — livré ici en Web à la place à l'origine, car l'app Mobile n'avait
**aucune authentification** construite (seul le branding l'était, voir Phase 0). L'auth
Mobile (connexion avec sous-domaine/e-mail/mot de passe, garde de route, intercepteur HTTP,
déconnexion, écran d'accueil minimal) est désormais construite, en miroir du Web — voir
SESSION_LOG.md 2026-09-17. Les écrans métier eux-mêmes (feuille d'appel, notes, etc.) sur
Mobile restent à construire un par un, module par module, dans de prochaines tâches.
**Portail parent/élève livré le 2026-09-21** : l'ADR-010 est levée sur son volet bloquant.
`students.user_id` et `parents.user_id` (migration V54) rattachent un compte à une fiche ;
`POST /students/{id}/account` et `POST /parents/{id}/account` ouvrent l'accès ;
`GET /portal/children` et `/portal/students/{id}/grades|attendance` servent les familles.
Écrans mobiles : liste des enfants et dossier (notes + absences). Le mockup
"Élève — emploi du temps" reste **non couvert** : l'emploi du temps d'un élève demanderait un
endpoint portail supplémentaire, non construit.

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
- [x] Contenu de séance + travail à faire, dates de publication/limite
- [x] Pièces jointes (référencent le module 2.2)
- [x] Consultation élèves/parents : **hors périmètre** — aucun portail élève/parent construit
      (ADR-010), consultation limitée aux rôles staff comme le reste de l'application
- [x] Notification nouveau devoir (même pattern gateway que les absences, ADR-012)

### 2.4 Messagerie interne
- [x] Messages individuels et de groupe entre utilisateurs staff
- [x] Annonces de l'établissement
- [x] Pièces jointes (référencent le module 2.2)
- [x] Confirmation de lecture
- [x] Historique et recherche des conversations

### 2.5 Notifications (infrastructure)
- [x] Registre centralisé des événements de notification (`NotificationType`,
      `NotificationDispatcher`) — généralise le pattern `ParentNotificationGateway`/
      `HomeworkNotificationGateway`/`MessageNotificationGateway` (ADR-012/018/019), qui sont
      supprimées. Préférences par utilisateur (`notification_preferences`, opt-out par type)
      exposées via `GET/PUT /api/v1/notification-preferences`. `NEW_GRADE`/`NEW_DOCUMENT`/
      `SUBSCRIPTION_ALERT` : le type existe et le registre est prêt, mais aucun appelant ne
      les déclenche encore (non demandé dans cette tâche, voir ADR-020)
- [x] Firebase Cloud Messaging réel : **non câblé** (pas de credentials FCM) — implémentation
      par défaut en log (`LoggingNotificationGateway`), comme pour les absences ; brancher FCM
      reste un remplacement d'implémentation, pas un changement d'appelants

### 2.6 Dashboard Super-Admin
- [x] Nombre d'établissements actifs/en essai/suspendus/résiliés
- [x] Revenu récurrent mensuel (MRR) et annuel (ARR)
- [x] Taux de churn et taux de conversion essai → abonnement payant — **taux cumulés depuis
      l'origine**, pas des cohortes par période (aucun historique d'événements d'abonnement,
      voir ADR-021)
- [x] Écran Web réservé au rôle Super-Administrateur — `/admin/login` + `/admin`
      (garde `superAdminGuard`), consomme `GET /api/v1/admin/dashboard/summary`

**État au 2026-09-17** : Phase 2 backend inchangée ; **frontend Web opérationnel** pour
bulletins, documents, cahier de textes, messagerie, notifications, dashboard Super-Admin.
Mobile : feuille d'appel, notes, emploi du temps (DESIGN.md §5, rôles staff — pas de
portail élève/parent, ADR-010).

**Critère de sortie de Phase 2** : mêmes garde-fous que la Phase 1 — chaque module testé
(y compris isolation cross-tenant), documenté, et son périmètre réel (vs. différé) explicite
avant de passer à la Phase 3.

## Phase 3 — Administration avancée et modules complémentaires

### 3.1 Vie scolaire (cahier §17)
- [x] Incidents (élève(s) concerné(s), classe, gravité, description, déclarant)
- [x] Sanctions et punitions rattachées à un incident (type, quantité/durée) — "Exclusions"
      (bullet séparé du cahier) = `SanctionType.EXPULSION`, pas une entité à part (ADR-022)
- [x] Convocations (élève/parent, motif, date, statut)
- [x] Observations (positives/négatives) et historique disciplinaire par élève
- [x] Statistiques de vie scolaire (incidents par classe/période, répartition par type/sévérité)
- [ ] Notification automatique aux parents lors d'une sanction/convocation : **hors périmètre**
      (aucun compte parent, ADR-010 ; registre de notifications ADR-020 non branché ici, pas
      demandé explicitement pour ce module contrairement aux absences en 1.7)

### 3.2 Statistiques avancées (cahier §18)
- [x] Évolution des résultats dans le temps (moyennes par classe/matière, regroupées par mois
      calendaire — pas de notion de trimestre/période paramétrable, voir ADR-023)
- [x] Rapports exportables : export CSV de l'évolution des résultats — **seul format construit
      pour cette passe** (pas de PDF pour cet indicateur, cahier §18 ne précise pas de format)
- [x] Usage global plateforme pour le Super-Admin — **notifications envoyées seulement**
      (`notification_log`, ADR-023) ; stockage utilisé et utilisateurs actifs cross-tenant
      **bloqués** : nécessitent une décision d'architecture qui touche l'invariante RLS du
      projet (voir docs/ARCHITECTURE.md "Points ouverts"), à trancher avec l'utilisateur

### 3.3 Comptabilité et frais scolaires (cahier §19.4 + §4.3)
- [x] Grille tarifaire des frais de scolarité par classe (pas de "niveau" séparé, voir ADR-024)
- [x] Échéancier de paiement pour les familles (une ligne de grille = une échéance datée)
- [x] Génération de factures par élève (masse, idempotent) — pas de PDF de reçu pour cette
      passe (voir "Hors périmètre" ci-dessous)
- [x] Rapprochement avec les moyens de paiement — saisie manuelle du personnel (méthode :
      espèces/virement/mobile money/autre), pas de rapprochement automatique bancaire
- [x] Reporting financier consolidé pour Direction/Comptable (total dû/payé/impayé par classe/période)
- [ ] Paiement mobile money local (cahier §4.3) — **questionné à l'utilisateur, réponse :
      décision reportée**. `FeePaymentMethod.MOBILE_MONEY` existe pour tracer un paiement reçu
      hors-ligne, mais aucun fournisseur/gateway n'est intégré (voir ADR-024)
- [ ] Génération de reçus/factures PDF téléchargeables : **hors périmètre de cette passe** —
      seules les données structurées existent (montant dû/payé/statut) ; l'export PDF suivrait
      le même pattern que `ReportCardPdfExporter` (ADR-016) si demandé séparément

### 3.4 Cantine (cahier §19.1)
- [x] Gestion des menus et régimes alimentaires particuliers (variante du menu du jour, pas
      de profil allergène par élève — donnée de santé, hors périmètre)
- [x] Réservation des repas — **questionné à l'utilisateur, réponse : saisie côté staff pour
      cette passe** (pas de portail parent, ADR-010/ADR-025), `SECRETARY`/`ADMIN`/`DIRECTION`
      saisissent pour le compte des familles
- [x] Facturation liée à la consommation réelle (comptage des réservations sur une période ×
      prix par repas donné à la génération, pas de tarif persisté ni d'abonnement forfaitaire)
- [x] Suivi des impayés (`GET /canteen/invoices/unpaid`, vue à plat tous élèves)

### 3.5 Bibliothèque (cahier §19.3)
- [x] Catalogue des ouvrages (codes-barres obligatoire, ISBN optionnel)
- [x] Emprunts et retours avec relances automatiques (`LibraryOverdueReminderJob`, quotidien,
      via le registre centralisé de notifications — nouveau `NotificationType.LIBRARY_OVERDUE`)
- [x] Réservations et liste d'attente (FIFO, libérée automatiquement au retour d'un ouvrage)

### 3.6 Transport scolaire (cahier §19.2)
- [x] Gestion des lignes et arrêts de bus (arrêts ordonnés par ligne)
- [x] Affectation des élèves aux circuits (une seule affectation active par élève)
- [x] Facturation du service — forfait périodique saisi à la génération (pas "à la
      consommation réelle" comme la cantine, le cahier ne le demande pas ici, voir ADR-027)
- [ ] Suivi de présence à bord via QR/NFC : **hors périmètre ici** — cahier §19.2 le renvoie
      lui-même à la section 30/Phase 4 ("QR code pour la présence")

### 3.7 Domaine personnalisé et branding avancé — plan Premium (cahier §2.3/§2.4/§4.1)
- [x] Domaine personnalisé (custom domain) pointant vers la plateforme — résolution par nom
      d'hôte (`TenantResolver`), écriture réservée au plan Premium (`PlanFeature.CUSTOM_DOMAIN`) ;
      provisioning DNS/certificat réel hors périmètre applicatif (voir ADR-028)
- [x] Modèle de bulletin personnalisable (en-tête, mentions légales de l'établissement) —
      consommé par `ReportCardPdfExporter`, comportement par défaut inchangé sans configuration
- [x] Templates de notification/e-mail personnalisables par établissement — titre/corps
      surchargeables par `NotificationType` (`NotificationTemplate`, placeholder `{message}`),
      appliqués par `NotificationDispatcher` avant l'envoi, sans changement de signature pour
      les modules appelants
- [x] Feature flags par tenant selon le plan souscrit (cantine/transport/bibliothèque en
      option Standard, inclus Premium — grille §4.1) — **simplification assumée** : "en
      option" traité comme équivalent à "inclus dès que le plan est souscrit", pas d'achat à
      la carte ni de dérogation individuelle par tenant (voir ADR-028)

**Critère de sortie de Phase 3** : mêmes garde-fous que les Phases 1 et 2 — chaque module
testé (isolation cross-tenant comprise), documenté, périmètre réel (vs. différé) explicite.

**État frontend au 2026-09-17** : écrans Web staff pour 3.1–3.7 (vie scolaire, stats,
frais, cantine, bibliothèque, transport, branding/domaine) + Mobile (appel, notes, EDT).
Hors périmètre inchangé : QR/NFC, reçus PDF, mobile money gateway, portail parent.

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
