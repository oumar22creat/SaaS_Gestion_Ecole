**CAHIER DES CHARGES**

**Plateforme SaaS de Gestion Scolaire Multi-Établissements**

*Solution inspirée fonctionnellement de PRONOTE — Version 2.0 (édition
SaaS)*

Ce document reprend le cahier des charges v1.0 et y intègre l'ensemble
des éléments nécessaires à la réalisation d'un véritable produit SaaS
multi-tenant : architecture, abonnements, facturation, conformité,
infrastructure et gouvernance des données.

**Sommaire**

- 1\. Objectif du projet

- 2\. Modèle SaaS et architecture multi-tenant

- 3\. Architecture générale

- 4\. Plans d'abonnement et facturation

- 5\. Gestion des utilisateurs et rôles

- 6\. Gestion de l'établissement

- 7\. Gestion des élèves et responsables

- 8\. Gestion des classes et matières

- 9\. Emploi du temps

- 10\. Absences et retards

- 11\. Notes et évaluations

- 12\. Bulletins scolaires

- 13\. Cahier de textes et devoirs

- 14\. Documents

- 15\. Communication et messagerie

- 16\. Notifications

- 17\. Vie scolaire

- 18\. Tableau de bord et statistiques

- 19\. Modules complémentaires (cantine, transport, bibliothèque, RH)

- 20\. Onboarding et provisioning des établissements

- 21\. Application mobile Ionic

- 22\. Modèle de données PostgreSQL

- 23\. API REST Spring Boot et API publique

- 24\. Sécurité et conformité RGPD

- 25\. Infrastructure, scalabilité et DevOps

- 26\. Architecture logicielle

- 27\. Aspects légaux et commerciaux

- 28\. Observabilité, support et disponibilité (SLA)

- 29\. Plan de développement

- 30\. Évolutions futures

- 31\. Livrables attendus

- 32\. Critères de réussite

**1. Objectif du projet**

Développer une plateforme SaaS complète de gestion scolaire, exploitable
par un nombre illimité d'établissements clients, permettant de
centraliser la gestion pédagogique, administrative, la vie scolaire et
la communication entre l'établissement, les enseignants, les élèves et
les parents.

Le produit s'inspire fonctionnellement des usages d'une solution de type
PRONOTE, sans reproduire son code, son identité graphique ou ses
éléments propriétaires.

**NOUVEAU — Ajout pour la version SaaS**

*Contrairement à la version 1.0 (pensée pour un seul établissement puis
évoluée vers du SaaS en phase 4), cette version considère le
multi-tenant comme une exigence fondatrice dès le MVP, car migrer une
architecture mono-tenant vers du multi-tenant a posteriori est coûteux
et risqué.*

**Objectifs principaux**

- Centraliser les données scolaires de plusieurs établissements sur une
  infrastructure mutualisée et isolée.

- Permettre à un nouvel établissement de s'inscrire et devenir
  opérationnel en autonomie (self-onboarding).

- Monétiser l'usage via des abonnements récurrents (SaaS) avec plusieurs
  niveaux de service.

- Garantir l'étanchéité totale des données entre établissements clients
  (isolation multi-tenant).

- Réduire les opérations manuelles de l'établissement.

- Permettre aux enseignants de saisir notes, absences et contenus depuis
  le Web ou le mobile.

- Permettre aux parents et élèves de suivre en temps réel leur situation
  scolaire.

- Automatiser les bulletins, notifications et statistiques.

- Offrir une supervision globale de la plateforme (back-office SaaS) à
  l'éditeur du produit.

**2. Modèle SaaS et architecture multi-tenant**

**NOUVEAU — Ajout pour la version SaaS**

Cette section est le cœur de la transformation SaaS : elle définit
comment plusieurs établissements cohabitent sur la même plateforme sans
jamais accéder aux données les uns des autres.

**2.1 Stratégie d'isolation des données**

Trois approches sont possibles ; le choix retenu doit être documenté et
validé avant le développement :

- Base partagée avec discriminant tenant_id (recommandé pour le MVP) :
  toutes les écoles partagent le même schéma PostgreSQL, chaque table
  métier porte une colonne school_id, et un filtre est appliqué
  systématiquement au niveau applicatif (idéalement via un mécanisme
  transverse type Hibernate Filter ou Row-Level Security PostgreSQL).

- Schéma par tenant (schema-per-tenant) : un schéma PostgreSQL dédié par
  établissement — meilleure isolation, plus complexe à maintenir et à
  faire évoluer (migrations à répliquer sur chaque schéma).

- Base de données par tenant : isolation maximale, coût d'infrastructure
  et de maintenance le plus élevé — réservé aux gros clients ou aux
  exigences réglementaires fortes.

*⚠ Recommandation : démarrer avec une base partagée + tenant_id, en
activant PostgreSQL Row-Level Security (RLS) pour une isolation garantie
même en cas d'oubli de filtre côté code. Prévoir une option d'isolation
renforcée (schéma ou base dédiée) pour les clients Enterprise.*

**2.2 Propagation du contexte tenant**

- Le tenant_id (identifiant établissement) doit être résolu dès
  l'authentification et injecté dans le JWT.

- Un intercepteur/filtre Spring Boot doit positionner le contexte tenant
  sur chaque requête (ThreadLocal ou équivalent) avant d'atteindre la
  couche service.

- Toute requête JPA doit être automatiquement filtrée par tenant_id
  (Hibernate Filter global) — aucune requête métier ne doit oublier ce
  filtre.

- Les identifiants exposés dans les URLs (ex. /api/students/42) doivent
  être vérifiés côté serveur comme appartenant au tenant courant, même
  si l'ID est correct pour un autre tenant.

**2.3 Résolution du tenant côté client**

- Sous-domaine dédié par établissement (ex.
  ecole-victor-hugo.monapp.com) résolu via DNS wildcard.

- Alternative : sélection de l'établissement à la connexion pour les
  utilisateurs multi-comptes (ex. parent avec enfants dans deux écoles
  différentes).

- Domaine personnalisé (custom domain) en option payante pour les
  clients Premium (ex. ent.mon-ecole.com pointant vers la plateforme).

**2.4 Personnalisation par établissement (branding)**

- Logo, couleurs primaires/secondaires, nom affiché dans l'interface et
  les e-mails.

- Modèle de bulletin personnalisable (en-tête, mentions légales de
  l'établissement).

- Paramètres pédagogiques propres (système de notation, périodes, jours
  fériés) déjà prévus en v1.0, désormais scopés par tenant.

- Templates de notification/e-mail personnalisables par établissement
  (signature, objet).

**2.5 Rôle Super-Administrateur (éditeur SaaS)**

Un rôle transverse, au-dessus de la Direction, réservé à l'éditeur de la
plateforme :

- Création, suspension et suppression des comptes établissements
  (tenants).

- Vue consolidée de tous les clients : statut d'abonnement, usage,
  dernière connexion.

- Impersonation contrôlée et journalisée (se connecter en lecture ou
  support au nom d'un établissement, avec consentement et traçabilité).

- Gestion des plans tarifaires et des fonctionnalités activées par
  tenant (feature flags).

- Accès aux métriques globales de la plateforme (MRR, churn, croissance,
  incidents).

**3. Architecture générale**

L'architecture est organisée autour d'un frontend Web Angular, d'une
application mobile Ionic/Capacitor et d'une API REST Spring Boot
connectée à PostgreSQL, le tout conçu nativement multi-tenant.

**Flux logique**

- Internet → CDN (assets statiques) → Nginx / Reverse Proxy / HTTPS

- Résolution du tenant via sous-domaine → Angular Web ou Ionic Mobile

- Angular/Ionic → API Gateway → API REST Spring Boot

- Spring Boot → Filtre de contexte tenant → PostgreSQL (RLS activé)

- Spring Boot → Redis (cache, sessions, rate limiting, files d'attente
  légères)

- Spring Boot → Firebase Cloud Messaging (notifications push)

- Spring Boot → File d'attente asynchrone (ex. RabbitMQ/Kafka) pour les
  traitements lourds : génération de bulletins en masse, envoi
  d'e-mails, exports.

**NOUVEAU — Ajout pour la version SaaS**

*Redis, initialement présenté comme optionnel en v1.0, devient une
brique standard : sessions techniques, cache des permissions par tenant,
et rate limiting distribué (indispensable dès qu'il y a plusieurs
instances applicatives).*

**4. Plans d'abonnement et facturation**

**NOUVEAU — Ajout pour la version SaaS**

**4.1 Grille tarifaire indicative**

|                                    |                    |                    |                          |
|------------------------------------|--------------------|--------------------|--------------------------|
|                                    | **Essentiel**      | **Standard**       | **Premium / Enterprise** |
| Élèves inclus                      | Jusqu'à 150        | Jusqu'à 800        | Illimité                 |
| Utilisateurs (parents/profs)       | Inclus             | Inclus             | Inclus                   |
| Application mobile                 | Oui                | Oui                | Oui                      |
| Bulletins & notes                  | Oui                | Oui                | Oui                      |
| Cantine / Transport / Bibliothèque | Non                | En option          | Inclus                   |
| Domaine personnalisé               | Non                | Non                | Oui                      |
| Statistiques avancées / BI         | Non                | Basique            | Avancé                   |
| Support                            | E-mail             | E-mail + chat      | Dédié + SLA              |
| Facturation                        | Mensuelle/annuelle | Mensuelle/annuelle | Annuelle, sur devis      |

*Cette grille est indicative : les seuils, prix et fonctionnalités par
plan sont à valider avec le porteur de projet en fonction du marché
cible.*

**4.2 Cycle de vie de l'abonnement**

- Essai gratuit (trial) de durée paramétrable (ex. 30 jours) sans carte
  bancaire ou avec pré-autorisation.

- Souscription à un plan avec choix de périodicité (mensuelle / annuelle
  avec remise).

- Changement de plan (upgrade/downgrade) avec calcul de proratisation.

- Relance automatique en cas d'échec de paiement, période de grâce, puis
  suspension progressive de l'accès (lecture seule, puis blocage).

- Résiliation avec export des données de l'établissement avant
  suppression définitive (droit à la portabilité).

- Facturation automatique et génération de factures PDF téléchargeables.

**4.3 Moyens de paiement**

- Intégration d'un fournisseur de paiement par carte (Stripe ou
  équivalent) pour les paiements internationaux/récurrents.

- Intégration de moyens de paiement locaux mobile money selon le marché
  cible (ex. Orange Money, Wave, Moov Money) pour le règlement des
  abonnements et, en Phase 3, des frais de scolarité.

- Webhooks de paiement pour synchroniser automatiquement le statut
  d'abonnement (paiement réussi, échoué, remboursé).

- Historique de facturation consultable par l'établissement (rôle
  Comptable/Direction).

**4.4 Limites et quotas par plan**

- Nombre maximal d'élèves, d'enseignants et de comptes actifs.

- Quota de stockage documentaire (Go) par établissement.

- Limites d'envoi de notifications push / e-mails par mois.

- Blocage ou alerte automatique en cas de dépassement de quota (avec
  proposition d'upgrade).

**5. Gestion des utilisateurs et rôles**

Rôles prévus (tous scopés à un établissement, à l'exception du
Super-Administrateur) :

- Super-Administrateur (SaaS) : Gestion de la plateforme globale, des
  tenants, des plans d'abonnement et de la facturation. N'appartient à
  aucun établissement.

- Administrateur : Configuration complète de l'établissement,
  utilisateurs, permissions, paramètres, sécurité, statistiques et
  supervision.

- Direction : Pilotage de l'établissement, classes, enseignants,
  résultats, absences, bulletins et statistiques.

- Enseignant : Emploi du temps, appel, notes, cahier de textes, devoirs,
  documents et messagerie.

- Élève : Emploi du temps, notes, devoirs, cahier de textes, absences,
  documents et notifications.

- Parent / Tuteur : Suivi des enfants, notes, absences, devoirs,
  bulletins, emploi du temps, communication et paiements.

- Vie scolaire : Absences, retards, incidents, sanctions, convocations
  et suivi disciplinaire.

- Secrétaire : Inscriptions, dossiers élèves, documents administratifs
  et gestion courante.

- Comptable : Frais scolaires, paiements, factures, reçus, reporting
  financier et abonnement SaaS de l'établissement.

**NOUVEAU — Ajout pour la version SaaS**

*Ajout du rôle Super-Administrateur, indispensable pour opérer la
plateforme en mode SaaS.*

**6. Gestion de l'établissement**

- Informations de l'établissement : nom, logo, adresse, téléphone,
  e-mail, site Web.

- Années scolaires et périodes : trimestres ou semestres.

- Niveaux, classes, salles et groupes.

- Paramètres de notation, coefficients et règles de calcul.

- Configuration des horaires, jours fériés et périodes de cours.

- Paramètres d'abonnement et de facturation propres à l'établissement
  (visible par Direction/Comptable).

**7. Gestion des élèves et responsables**

- Matricule, identité, photo, date et lieu de naissance, sexe, contacts
  et adresse.

- Classe, niveau, année scolaire et statut de scolarité.

- Dossier administratif et pièces justificatives.

- Association d'un ou plusieurs parents/tuteurs.

- Définition du responsable principal et des moyens de contact.

- Historique des inscriptions et changements de classe.

- Import en masse via fichier Excel/CSV lors de l'onboarding ou en
  rentrée scolaire.

**8. Gestion des classes et matières**

- Création et modification des classes.

- Affectation des élèves et du professeur principal.

- Création des matières et codes matières.

- Coefficient, volume horaire et type d'évaluation.

- Affectation des enseignants aux classes et matières.

**9. Emploi du temps**

- Création des cours avec classe, enseignant, matière, salle et
  horaires.

- Répétition hebdomadaire.

- Modification ou annulation exceptionnelle.

- Gestion des remplacements d'enseignants.

- Vues journalière, hebdomadaire, par classe et par enseignant.

- Détection des conflits de salle, enseignant et classe.

**10. Absences et retards**

La feuille d'appel doit être optimisée pour une utilisation rapide sur
smartphone.

- Présent, absent, retard, départ anticipé et autres statuts
  configurables.

- Motif d'absence et justificatif.

- Commentaire et historique des modifications.

- Notification au parent selon les règles de l'établissement.

- Statistiques d'absences et retards par élève, classe et période.

**11. Notes et évaluations**

- Création d'évaluations par matière et classe.

- Barème, coefficient, date et type d'évaluation.

- Saisie individuelle ou import de notes.

- Calcul automatique des moyennes.

- Moyenne de classe, minimum, maximum et statistiques.

- Gestion des absences aux évaluations.

- Historisation des modifications de notes.

**12. Bulletins scolaires**

- Génération automatique des bulletins.

- Moyennes par matière et moyenne générale.

- Coefficients et calculs configurables.

- Rang si activé par l'établissement.

- Appréciations des enseignants et du professeur principal.

- Appréciation générale et décision du conseil de classe.

- Absences et retards.

- Export PDF, signature électronique (voir section 27) et archivage.

**13. Cahier de textes et devoirs**

- Saisie du contenu du cours.

- Ajout du travail à faire.

- Date de publication et date limite.

- Pièces jointes.

- Consultation par élèves et parents.

- Notifications en cas de nouveau devoir.

**14. Documents**

- Bibliothèque de documents par matière, classe ou service.

- PDF, images et documents bureautiques selon les règles définies.

- Gestion des droits de consultation.

- Archivage et suppression contrôlée.

- Possibilité d'associer un document à un cours ou devoir.

- Stockage objet (S3-compatible) avec quota par tenant selon le plan
  souscrit.

**15. Communication et messagerie**

- Messagerie interne entre utilisateurs autorisés.

- Messages individuels et de groupe.

- Annonces de l'établissement.

- Pièces jointes.

- Confirmation de lecture.

- Historique et recherche des conversations.

**16. Notifications**

- Notifications push via Firebase Cloud Messaging.

- Nouvelle note.

- Absence ou retard.

- Nouveau devoir.

- Nouveau document.

- Message reçu.

- Annonce de l'établissement.

- Alertes liées à l'abonnement (fin d'essai, échec de paiement, quota
  atteint) pour les rôles Direction/Comptable.

- Paramétrage des notifications par rôle.

**17. Vie scolaire**

- Suivi des absences et retards.

- Incidents.

- Sanctions et punitions.

- Exclusions.

- Convocations.

- Observations.

- Historique disciplinaire.

- Statistiques de vie scolaire.

**18. Tableau de bord et statistiques**

- Nombre d'élèves, enseignants et classes.

- Taux de présence.

- Nombre d'absences et retards.

- Moyennes par classe et matière.

- Évolution des résultats.

- Indicateurs de réussite.

- Rapports exportables.

**Tableau de bord Super-Admin (SaaS)**

**NOUVEAU — Ajout pour la version SaaS**

- Nombre d'établissements actifs, en essai, suspendus et résiliés.

- Revenu récurrent mensuel (MRR) et annuel (ARR).

- Taux de churn (résiliation) et taux de conversion essai → abonnement
  payant.

- Usage global (stockage, notifications envoyées, utilisateurs actifs)
  pour anticiper le scaling.

- Alertes d'incidents techniques et de santé de la plateforme.

**19. Modules complémentaires (cantine, transport, bibliothèque, RH)**

**NOUVEAU — Ajout pour la version SaaS**

Ces modules, évoqués en Phase 3 dans le plan initial, sont détaillés ici
pour être chiffrés dès la conception du modèle de données.

**19.1 Cantine**

- Gestion des menus et régimes alimentaires particuliers.

- Réservation des repas par les parents.

- Facturation liée à la consommation réelle.

- Suivi des impayés.

**19.2 Transport scolaire**

- Gestion des lignes et arrêts de bus.

- Affectation des élèves aux circuits.

- Suivi de présence à bord (optionnel, via QR code/NFC — voir section
  30).

- Facturation du service.

**19.3 Bibliothèque**

- Catalogue des ouvrages avec codes-barres/ISBN.

- Emprunts et retours avec relances automatiques.

- Réservations et liste d'attente.

**19.4 Comptabilité et frais scolaires**

- Grille tarifaire des frais de scolarité par niveau/classe.

- Échéancier de paiement pour les familles.

- Génération de reçus et factures.

- Rapprochement avec les moyens de paiement intégrés (section 4.3).

- Reporting financier consolidé pour la Direction/Comptable.

**19.5 Ressources humaines (personnel)**

- Dossiers du personnel enseignant et non-enseignant.

- Suivi des contrats et absences du personnel.

- Distinct de la paie, qui peut être intégrée via connecteur avec un
  logiciel de paie tiers plutôt que redéveloppée.

**20. Onboarding et provisioning des établissements**

**NOUVEAU — Ajout pour la version SaaS**

**20.1 Inscription en self-service**

- Formulaire d'inscription public (nom de l'établissement, pays, taille
  estimée, coordonnées).

- Création automatique du tenant, du sous-domaine et du compte
  Administrateur initial.

- Choix du plan (avec essai gratuit par défaut) et activation immédiate.

- Vérification e-mail et validation anti-abus (captcha, limitation de
  créations).

**20.2 Assistant de configuration initiale (wizard)**

- Étape 1 : informations établissement, logo, année scolaire en cours.

- Étape 2 : import des niveaux, classes et matières (modèles pré-remplis
  proposés).

- Étape 3 : import des élèves et parents via fichier Excel/CSV avec
  validation des erreurs ligne par ligne.

- Étape 4 : import ou création des comptes enseignants avec affectation
  aux classes/matières.

- Étape 5 : configuration de l'emploi du temps (import ou saisie
  manuelle).

- Étape 6 : récapitulatif et activation — l'établissement devient
  pleinement opérationnel.

**20.3 Migration depuis un autre système**

- Modèles d'import génériques (Excel/CSV) documentés pour faciliter la
  reprise de données depuis un système existant (y compris export
  PRONOTE si le client en dispose).

- Service d'accompagnement à la migration pour les clients Premium.

**21. Application mobile Ionic**

L'application mobile doit proposer une expérience adaptée au rôle
connecté et à l'établissement (branding dynamique chargé au démarrage
selon le tenant).

- Parent : enfants, notes, absences, emploi du temps, devoirs,
  bulletins, notifications, messagerie et paiement des frais/cantine.

- Élève : emploi du temps, notes, devoirs, cahier de textes, documents
  et notifications.

- Enseignant : emploi du temps, appel, notes, cahier de textes, devoirs
  et messagerie.

- Authentification sécurisée et gestion du token (avec refresh
  silencieux).

- Notifications push.

- Stockage local limité pour améliorer l'expérience hors connexion.

- Sélecteur d'établissement pour les comptes rattachés à plusieurs
  tenants (ex. parent avec enfants dans deux écoles).

**22. Modèle de données PostgreSQL**

Principales entités prévues — toutes les tables métier portent désormais
une colonne school_id (tenant) avec contrainte de clé étrangère et
Row-Level Security activée :

- tenants (school_id), subscriptions, plans, invoices, payments

- users, roles, permissions

- schools, academic_years

- students, parents, student_parents

- teachers, staff

- classes, subjects, class_subjects

- rooms, timetables, courses

- attendance, attendance_reasons

- exams, grades, grade_items

- report_cards, report_card_comments

- homework, lessons, documents

- messages, notifications

- disciplinary_actions, incidents

- canteen_menus, canteen_reservations, transport_routes,
  transport_assignments, library_items, library_loans

- audit_logs (avec traçabilité par tenant et par utilisateur, y compris
  actions du Super-Administrateur)

**NOUVEAU — Ajout pour la version SaaS**

*Ajout des tables liées aux abonnements (tenants, subscriptions, plans,
invoices, payments) et aux modules complémentaires, ainsi que du
principe systématique school_id + RLS.*

**23. API REST Spring Boot et API publique**

**23.1 API interne (Web + Mobile)**

- /api/auth

- /api/users

- /api/students

- /api/parents

- /api/teachers

- /api/classes

- /api/subjects

- /api/timetables

- /api/attendance

- /api/grades

- /api/exams

- /api/report-cards

- /api/homework

- /api/documents

- /api/messages

- /api/notifications

- /api/statistics

- /api/canteen, /api/transport, /api/library

- /api/billing (abonnement, factures)

Les endpoints doivent utiliser des DTO, une validation des entrées, une
gestion centralisée des exceptions, un filtrage systématique par tenant
et une documentation OpenAPI/Swagger.

**23.2 API d'administration SaaS (réservée Super-Admin)**

**NOUVEAU — Ajout pour la version SaaS**

- /api/admin/tenants (création, suspension, suppression
  d'établissements)

- /api/admin/plans (gestion des plans et fonctionnalités)

- /api/admin/metrics (MRR, churn, usage global)

**23.3 API publique et webhooks (évolution)**

**NOUVEAU — Ajout pour la version SaaS**

- API publique documentée pour intégrations tierces (site web de
  l'établissement, ENT académique, logiciels de paie).

- Webhooks sortants (nouvel élève inscrit, bulletin publié, paiement
  reçu) pour connecter des outils externes.

- Clés API par établissement avec scopes et quotas.

**24. Sécurité et conformité RGPD**

**24.1 Sécurité applicative (base v1.0)**

- HTTPS/TLS obligatoire en production.

- JWT avec access token et refresh token.

- Hachage sécurisé des mots de passe avec BCrypt ou Argon2.

- RBAC et contrôle des permissions côté serveur.

- Validation stricte des entrées.

- Protection contre les injections SQL via JPA/Hibernate et requêtes
  paramétrées.

- CORS configuré explicitement.

- Rate limiting sur les endpoints sensibles.

- Journalisation des événements de sécurité.

- Audit des opérations sensibles.

- Sauvegardes PostgreSQL régulières et tests de restauration.

- Aucun secret ou mot de passe dans le code source.

**24.2 Isolation multi-tenant (spécifique SaaS)**

**NOUVEAU — Ajout pour la version SaaS**

- PostgreSQL Row-Level Security activée sur toutes les tables métier, en
  complément du filtre applicatif.

- Tests automatisés dédiés à la fuite de données inter-tenant (un
  utilisateur de l'établissement A ne doit jamais pouvoir accéder à une
  ressource de l'établissement B, même via manipulation d'identifiants).

- Chiffrement au repos des données sensibles (disques chiffrés, colonnes
  sensibles chiffrées si nécessaire).

- Chiffrement des sauvegardes et séparation des sauvegardes par tenant
  pour faciliter une restauration ciblée.

**24.3 Conformité RGPD et protection des mineurs**

**NOUVEAU — Ajout pour la version SaaS**

- Les données concernant des élèves mineurs sont des données sensibles :
  minimisation de la collecte, durée de conservation définie et
  documentée.

- Recueil du consentement parental lorsqu'exigé par la réglementation
  locale (ex. traitement de photo, partage à des tiers).

- Droit d'accès, de rectification et d'effacement (« droit à l'oubli »)
  pour les parents/élèves majeurs, avec procédure documentée.

- Droit à la portabilité : export structuré des données d'un
  établissement lors d'une résiliation.

- Registre des traitements et, si applicable, désignation d'un délégué à
  la protection des données (DPO).

- Contrat de sous-traitance (DPA — Data Processing Agreement) entre
  l'éditeur SaaS et chaque établissement client, précisant les
  responsabilités respectives.

- Politique de purge automatique des comptes et données au-delà de la
  durée de conservation définie.

- Localisation des données précisée contractuellement (pays
  d'hébergement), point souvent exigé par les établissements publics.

**25. Infrastructure, scalabilité et DevOps**

**25.1 Socle (base v1.0)**

- Docker pour la conteneurisation de chaque service.

- Nginx en reverse proxy / terminaison TLS.

- GitLab CI/CD pour l'intégration et le déploiement continus.

**25.2 Scalabilité (spécifique SaaS)**

**NOUVEAU — Ajout pour la version SaaS**

- Orchestration via Kubernetes (ou service managé équivalent) dès que le
  nombre d'établissements le justifie, pour permettre l'auto-scaling
  horizontal du backend.

- Séparation des workers asynchrones (génération de bulletins en masse,
  envoi d'e-mails/notifications) du service API principal, pour éviter
  qu'un traitement lourd ne dégrade le temps de réponse global.

- CDN pour la distribution des assets statiques (Web et ressources
  documentaires publiques).

- Base de données : réplication en lecture (read replicas) si la charge
  de lecture (dashboards, statistiques) devient significative.

- Stratégie de sauvegarde automatisée, testée régulièrement, avec
  objectifs de RPO (perte de données maximale tolérée) et RTO (délai de
  restauration) définis par plan tarifaire.

**25.3 Environnements et déploiement**

- Environnements distincts : développement, recette/staging, production.

- Migrations de base de données versionnées (ex. Flyway ou Liquibase)
  appliquées automatiquement en CI/CD.

- Déploiements sans interruption de service (rolling deployment /
  blue-green) pour un service SaaS multi-clients.

- Feature flags pour activer progressivement de nouvelles
  fonctionnalités par tenant ou par plan.

**26. Architecture logicielle**

Le backend Spring Boot sera organisé par domaines fonctionnels :

- auth

- tenant / billing

- user

- student

- parent

- teacher

- class

- subject

- timetable

- attendance

- grade

- reportcard

- homework

- document

- messaging

- notification

- discipline

- statistics

- canteen

- transport

- library

Chaque module pourra être structuré autour de controller, service,
repository, entity, DTO, mapper et gestion des exceptions. Le module
tenant/billing centralise la résolution du contexte établissement et
l'intégration avec le fournisseur de paiement.

**27. Aspects légaux et commerciaux**

**NOUVEAU — Ajout pour la version SaaS**

- Conditions Générales d'Utilisation (CGU) et Conditions Générales de
  Vente (CGV) régissant la relation avec les établissements clients.

- Politique de confidentialité publique, détaillant les traitements de
  données personnelles.

- Contrat-cadre / DPA pour les clients Enterprise, avec engagement de
  niveau de service (SLA).

- Modalités de résiliation et de récupération des données en fin de
  contrat.

- Signature électronique pour les documents contractuels (bulletins,
  conventions) — voir aussi section 30.

- Mentions légales et conformité à la réglementation locale sur le
  commerce électronique et les paiements.

**28. Observabilité, support et disponibilité (SLA)**

**NOUVEAU — Ajout pour la version SaaS**

**28.1 Observabilité**

- Monitoring applicatif et infrastructure (ex. Prometheus/Grafana ou
  solution managée équivalente).

- Centralisation des logs (ex. stack ELK ou équivalent) avec
  conservation adaptée à l'audit de sécurité.

- Suivi des erreurs applicatives en temps réel (ex. Sentry) avec
  alerting.

- Page de statut publique communiquant la disponibilité de la plateforme
  et les incidents en cours.

**28.2 Support client**

- Canal de support différencié par plan (e-mail, chat, support dédié —
  voir grille tarifaire section 4.1).

- Base de connaissances / FAQ en self-service.

- Système de tickets pour le suivi des demandes.

**28.3 Engagements de service (SLA)**

- Taux de disponibilité cible (ex. 99,5 % hors maintenance planifiée) à
  formaliser contractuellement pour les plans payants.

- Fenêtre de maintenance planifiée communiquée à l'avance.

- Plan de reprise d'activité (PRA) documenté et testé périodiquement.

**29. Plan de développement**

*⚠ Le multi-tenant, la gestion des abonnements et l'onboarding sont
désormais intégrés dès la Phase 1 : les ajouter plus tard obligerait à
retraiter tout le modèle de données et la couche d'accès existants.*

**Phase 1 — MVP SaaS**

- Architecture multi-tenant (school_id + RLS) et résolution du tenant.

- Authentification, utilisateurs et rôles (dont Super-Administrateur).

- Onboarding self-service et assistant de configuration initiale.

- Plans d'abonnement de base, essai gratuit et paiement par carte.

- Élèves et parents, enseignants, classes et matières.

- Emploi du temps.

- Absences.

- Notes.

- Dashboard établissement.

**Phase 2 — Pédagogie et communication**

- Bulletins.

- Cahier de textes.

- Devoirs.

- Documents.

- Notifications.

- Messagerie.

- Dashboard Super-Admin (métriques SaaS de base : MRR, nombre de
  tenants, churn).

**Phase 3 — Administration avancée et modules complémentaires**

- Discipline (vie scolaire).

- Statistiques avancées.

- Comptabilité et frais scolaires.

- Paiement des frais scolaires (moyens de paiement locaux).

- Cantine.

- Bibliothèque.

- Transport scolaire.

- Domaine personnalisé et branding avancé (plan Premium).

**Phase 4 — Innovation, scalabilité et écosystème**

- QR Code pour présence.

- Signature électronique.

- BI / Analytics avancé.

- Fonctionnalités IA (aide à la rédaction d'appréciations, détection
  d'élèves à risque, avec contrôle humain).

- API publique et webhooks pour intégrations tierces.

- Passage à une architecture orchestrée (Kubernetes) si la croissance du
  nombre de tenants le justifie.

- Abonnements SaaS avancés (facturation à l'usage, marketplace de
  modules).

**30. Évolutions futures**

- Expansion multi-pays avec adaptation aux réglementations locales
  (paiement, protection des données).

- Intégration de moyens de paiement locaux additionnels selon les
  marchés visés.

- QR Code ou NFC pour certains processus de présence (élèves,
  transport).

- Tableaux de bord décisionnels avancés.

- Exports comptables et administratifs.

- Application progressive hors ligne pour les zones à connectivité
  limitée.

- Moteurs de recommandation et assistants IA pour les équipes
  pédagogiques, avec contrôle humain.

- Marketplace de modules tiers développés par des partenaires.

**31. Livrables attendus**

- Application Web Angular.

- Application mobile Ionic/Capacitor Android et iOS.

- API Spring Boot documentée (interne, administration SaaS et publique).

- Base PostgreSQL multi-tenant et scripts de migration.

- Back-office Super-Admin (gestion des tenants, plans et facturation).

- Intégration avec au moins un fournisseur de paiement international et
  un moyen de paiement local.

- Documentation technique.

- Documentation utilisateur (par rôle).

- Documentation API OpenAPI/Swagger.

- Tests unitaires, intégration, tests de sécurité et tests d'isolation
  multi-tenant.

- Pipeline GitLab CI/CD.

- Dockerfiles et configuration de déploiement (Kubernetes si retenu).

- Procédure de sauvegarde et restauration, testée.

- CGU, CGV et politique de confidentialité.

**32. Critères de réussite**

- Les rôles et permissions sont correctement appliqués, y compris le
  rôle Super-Administrateur.

- Aucune fuite de données n'est possible entre deux établissements
  (isolation multi-tenant vérifiée par tests).

- Un nouvel établissement peut s'inscrire et devenir opérationnel en
  autonomie via l'onboarding self-service.

- Le cycle de vie de l'abonnement (essai, paiement, échec, résiliation)
  fonctionne de bout en bout.

- Les données scolaires sont cohérentes entre Web et mobile.

- Les notes, moyennes et bulletins sont calculés selon les règles
  configurées par établissement.

- Les absences et retards sont historisés.

- Les notifications critiques sont délivrées de manière fiable.

- Les données sont protégées contre les accès non autorisés et conformes
  aux exigences RGPD.

- Les sauvegardes et restaurations sont testées, y compris pour un
  tenant isolé.

- L'application est exploitable sur ordinateur, tablette et smartphone.

- La plateforme supporte la montée en charge (ajout de nouveaux
  établissements) sans dégradation perceptible.

**Fin du cahier des charges — Version 2.0 (édition SaaS)**
