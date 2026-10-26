# Compte rendu — 22 au 26 octobre 2026

Branche de travail (celle que suit le serveur de production) : `fix/parcours-mobile-et-web`.
Rien n'a été poussé sur `main`.

## 1. Résumé de ce qui a été fait

| Chantier | Résultat | État |
|---|---|---|
| Site vitrine | Refonte complète (8 pages, fond clair), badge App Store officiel, politique de confidentialité corrigée (Firebase), en-têtes de sécurité (CSP), vrais 404 et redirections 301 | En ligne |
| Failles de sécurité | Périmètre enseignant contrôlé côté serveur : élèves, discipline, détail de classe, bulletins (professeur principal), messagerie (personnel seulement, annonces à la direction, 30 destinataires maximum) | En ligne |
| Jeton push | Retiré de façon fiable à la déconnexion, même après un redémarrage ; un renouvellement tardif ne rouvre plus une session fermée | En ligne (serveur) et dans les apps |
| Interface enseignant | `GET /me/teaching-scope` (ADR-030). Sur le web, les boutons interdits sont masqués pour les enseignants (bulletins, évaluations, cahier de textes, vie scolaire, messagerie) | En ligne |
| Emploi du temps mobile | Les créneaux arrivent avec leurs libellés (classe, matière, enseignant, salle) ; l'écran n'appelle plus `/teachers`, qui était refusé aux enseignants | En ligne (serveur), iOS 1.0.1, Android v3 |
| Annuaire de la messagerie | `GET /conversations/recipients` : tout le personnel peut ouvrir une conversation en choisissant ses destinataires dans une liste avec recherche, sans e-mail ni téléphone | En ligne |
| Écrire aux familles (option a) | Seuls ADMIN, DIRECTION et TEACHER (dans son périmètre) ouvrent une conversation avec une famille ; les autres rôles reçoivent un 403 `FAMILY_MESSAGING_NOT_ALLOWED` | En ligne |
| iOS | 1.0.1 (build 43) approuvée par Apple puis publiée le 25/10 dans 174 pays | Publiée |
| Android | Test fermé « Alpha » : v1 et v2 approuvées, v3 (versionCode 3) en examen | En examen |
| Réseaux sociaux | Plan sur 4 semaines, visuels de la semaine 1, 3 publications Facebook programmées sur la page LOGILAB | En cours |

## 2. Fichiers modifiés par agent

- **agent-designer** : `docs/refonte-site/` (PROPOSITION, SPEC, pages, maquette, RELECTURE-VISUELLE).
- **agent-frontend (site)** : `landing/` (8 pages, `assets/site.v3.css`, `assets/site.v2.js`, badges, images), `robots.txt`, `sitemap.xml`.
- **agent-devops** : `deploy/nginx/conf.d/app.conf.template`, `deploy/nginx/site-headers.conf`, `deploy/Dockerfile.web`.
- **agent-backend** :
  - `teacher/TeacherScope.java`, `teacher/TeachingScopeController.java`, `common/EmailNormalizer.java` ;
  - `timetable/TimetableEntryMapper.java` ;
  - `messaging/MessagingService.java`, `messaging/MessagingController.java`, `auth/UserRepository.java` ;
  - discipline, élèves, classes, bulletins (contrôles de périmètre) ;
  - `DeviceTokenService` (retrait du jeton borné au compte) ;
  - docs `ARCHITECTURE.md` (ADR-019, ADR-030), `API_CONVENTIONS.md`.
- **agent-frontend (web)** : `core/teaching-scope.service.ts`, `messaging/messaging.page.ts`, `reportcard/report-card.page.ts`, `grade/exam-form.dialog.ts`, `homework/lesson.page.ts`, `discipline/discipline.page.ts`.
- **agent-mobile** : `push/push-notification.service.ts`, `auth/token-refresh.service.ts`, `auth/auth-token.service.ts`, `core/auth.interceptor.ts`, `timetable/timetable.page.ts`, `core/role-access.ts`, `android/app/build.gradle` (versionCode 3), `ios/.../project.pbxproj` (1.0.1).
- **agent-tests** : `TeacherScopeTest`, `MessagingRecipientsTest` (14 tests), tests web de la messagerie, `docs/refonte-site/tests/RAPPORT-TESTS.md`.
- **Supervision** : `docs/FICHES-STORES.md`, `docs/ROADMAP.md`, `docs/SESSION_LOG.md`, ce compte rendu, `marketing/`.

## 3. Résultats des tests et de la relecture

- Backend : `mvn test` → **283 tests, 0 échec, 0 erreur** (PostgreSQL 16 réel par Testcontainers), dont les tests d'isolation entre établissements de chaque nouvel endpoint.
- Web : **104/104** tests ; build de production réussi.
- Site : rapport de tests (B1 à B6, R3) puis relecture finale (A1 à A3, M1 à M7), tous corrigés avant la mise en ligne.
- Relecteur :
  - aucun BLOQUANT sur les derniers commits ;
  - deux BLOQUANTS trouvés pendant la semaine, corrigés au deuxième passage : une session fermée pouvait se rouvrir (jeton push), et une annonce pouvait être déguisée en conversation (messagerie).
- Vérifications après chaque déploiement :
  - l'application web répond 200 ;
  - les nouveaux endpoints répondent 401 sans connexion ;
  - le serveur tourne sur le commit `b87c46e`.

## 4. Problèmes rencontrés et points à valider par Oumar

**Rencontrés (résolus)**
- Paquet Android `com.schoolsaas.mobile` déjà pris sur Play → `tech.schoolmanager.app`.
- Version iOS 1.0 fermée après publication → passage à 1.0.1. Le build 40 a été retiré de l'examen et remplacé par le 43 pour y inclure l'emploi du temps.
- Trois adresses de testeurs refusées par Google, retirées de la liste.
- Le contrôle de sécurité de Claude Code bloque les déploiements en production sans accord explicite donné juste avant. C'est voulu, et chaque déploiement a été confirmé.

**À valider / à faire**
1. Tester le compte enseignant de démo sur l'iPhone (1.0.1) puis sur Android (v3) : emploi du temps, boutons masqués, messagerie.
2. Relancer les testeurs Android : il en faut 12 inscrits pendant 14 jours avant la production.
3. Instagram n'est pas relié à la page Facebook et LinkedIn n'est pas connecté : à faire pour publier sur ces réseaux.
4. Avant d'ouvrir une deuxième vraie école : migration de normalisation des e-mails avec index unique (agent-bdd).
5. Dette mineure : `CorsTest` instable, recherche des messages à plafonner à 100 caractères, réponses de recherche périmées dans la messagerie web.
6. Prochaine livraison iOS : `MARKETING_VERSION` 1.0.2.
