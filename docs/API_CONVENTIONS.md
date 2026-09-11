# API CONVENTIONS

## Base
- Préfixe : `/api/v1/...`
- Ressources au pluriel : `/api/v1/students`, `/api/v1/classes`
- Authentification : header `Authorization: Bearer <access_token>`
- Tenant résolu automatiquement via sous-domaine — jamais passé en paramètre par le client

## Format de réponse (succès)
```json
{
  "data": { ... },
  "meta": { "page": 1, "pageSize": 20, "total": 134 }
}
```

## Format d'erreur (uniforme sur toute l'API)
```json
{
  "error": {
    "code": "STUDENT_NOT_FOUND",
    "message": "Élève introuvable",
    "details": []
  }
}
```
- Codes HTTP standards : 400 (validation), 401 (non authentifié), 403 (non autorisé),
  404 (introuvable), 409 (conflit), 422 (règle métier violée), 500 (erreur serveur).
- Ne jamais renvoyer 200 avec un champ `success: false` — utiliser le vrai code HTTP.

## Pagination
- Paramètres query : `?page=1&pageSize=20`
- Toujours paginer les listes potentiellement longues (élèves, notes, messages).

## Validation
- Validation des DTO en entrée avec Bean Validation (`@NotNull`, `@Size`, etc.)
- Ne jamais faire confiance à un `school_id` ou `role` envoyé dans le body — toujours le
  déduire du token/contexte serveur.

## Endpoints d'administration SaaS (`/api/v1/admin/...`)
- Réservés au rôle Super-Administrateur
- Toujours dans un namespace séparé pour pouvoir les isoler réseau si besoin plus tard
  (ex. `/api/v1/admin/tenants`, `/api/v1/admin/metrics`)

## Documentation
- Chaque endpoint doit être documenté via OpenAPI/Swagger (annotations SpringDoc).
- La doc doit être générée automatiquement, pas maintenue à la main dans un fichier séparé.
