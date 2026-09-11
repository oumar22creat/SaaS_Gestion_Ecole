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
