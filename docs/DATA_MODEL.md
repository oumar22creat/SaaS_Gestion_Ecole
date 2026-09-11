# DATA MODEL — Référence rapide

Détail complet des règles métier dans `cahier-des-charges.md` §22.
Ici : liste des entités et rappel des règles structurelles à ne jamais oublier.

## Règle universelle
Toute table ci-dessous (sauf `tenants` lui-même) contient une colonne `school_id`
(FK → `tenants.id`) + RLS activée. Ne jamais créer de table métier sans cette colonne.

## Entités — SaaS / plateforme (hors tenant)
- `tenants` (id, name, subdomain, status, created_at, plan_id...)
- `plans` (id, name, price, max_students, features...)
- `subscriptions` (tenant_id, plan_id, status, trial_ends_at, current_period_end...)
- `invoices`, `payments`

## Entités — cœur métier (scoped `school_id`)
- `users`, `roles`, `permissions`
- `academic_years`
- `students`, `parents`, `student_parents` (table de liaison)
- `teachers`, `staff`
- `classes`, `subjects`, `class_subjects`
- `rooms`, `timetables`, `courses`
- `attendance`, `attendance_reasons`
- `exams`, `grades`, `grade_items`
- `report_cards`, `report_card_comments`
- `homework`, `lessons`, `documents`
- `messages`, `notifications`
- `disciplinary_actions`, `incidents`

## Entités — modules complémentaires (Phase 3, scoped `school_id`)
- `canteen_menus`, `canteen_reservations`
- `transport_routes`, `transport_assignments`
- `library_items`, `library_loans`

## Transverse
- `audit_logs` (scoped `school_id` + `user_id`, y compris actions du Super-Admin sur un tenant)

## Ordre de création recommandé (respecter les dépendances FK)
1. `tenants`, `plans`, `subscriptions`
2. `users`, `roles`, `permissions`
3. `academic_years`, `classes`, `subjects`, `rooms`
4. `students`, `parents`, `student_parents`, `teachers`
5. `timetables`, `courses`
6. `attendance`, `exams`, `grades`
7. Reste des modules au fur et à mesure des phases
