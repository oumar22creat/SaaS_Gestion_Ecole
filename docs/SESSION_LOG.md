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

## [Exemple — à supprimer après la première vraie session]
**Tâche(s) réalisée(s) :** Mise en place du squelette Spring Boot + docker-compose PostgreSQL,
configuration Flyway.
**Décisions prises (et pourquoi) :** Mono-repo avec `/backend`, `/web`, `/mobile` séparés en
sous-dossiers, plus simple à faire naviguer par Claude Code qu'un multi-repo pour un projet
en solo.
**Problèmes rencontrés / points de vigilance :** Aucun encore. Prochaine session doit poser
la première table `tenants` et le mécanisme RLS avant tout le reste — ne pas coder de
fonctionnalité métier avant que l'isolation tenant soit validée par un test.
**Prochaine étape :** ROADMAP.md → Phase 1.1 (fondations multi-tenant).
