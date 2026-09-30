#!/usr/bin/env bash
#
# Crée un compte Super-Administrateur de la plateforme.
#
# Aucun compte n'existe après l'installation : c'est volontaire, un mot de passe par défaut
# dans un dépôt finit toujours par rester en production. Ce script demande le mot de passe
# sans l'afficher, calcule son empreinte sur place et n'écrit que celle-ci — le mot de passe
# lui-même ne touche ni le disque ni l'historique du shell.

set -euo pipefail

RACINE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=/dev/null
set -a; source "${RACINE}/.env"; set +a
COMPOSE="docker compose -f ${RACINE}/docker-compose.prod.yml --env-file ${RACINE}/.env"

command -v htpasswd >/dev/null || { echo "Installer d'abord : sudo apt-get install -y apache2-utils" >&2; exit 1; }

read -rp  "Adresse e-mail : " EMAIL
read -rp  "Prénom         : " PRENOM
read -rp  "Nom            : " NOM
read -rsp "Mot de passe   : " MDP; echo
read -rsp "Confirmer      : " MDP2; echo

[ "${MDP}" = "${MDP2}" ] || { echo "Les mots de passe diffèrent." >&2; exit 1; }
# Douze caractères au minimum : ce compte voit tous les établissements de la plateforme, il
# n'a pas le même profil de risque qu'un compte d'école.
[ "${#MDP}" -ge 12 ] || { echo "Douze caractères minimum." >&2; exit 1; }

# bcrypt, coût 10 — même algorithme et même coût que BCryptPasswordEncoder côté Spring.
HASH="$(htpasswd -nbBC 10 "" "${MDP}" | cut -d: -f2)"
unset MDP MDP2

${COMPOSE} exec -T postgres psql -v ON_ERROR_STOP=1 \
    --username "${DB_ADMIN_USER}" --dbname "${DB_NAME}" \
    -v email="'${EMAIL}'" -v hash="'${HASH}'" -v prenom="'${PRENOM}'" -v nom="'${NOM}'" <<'SQL'
INSERT INTO platform_admins (email, password_hash, first_name, last_name)
VALUES (:email, :hash, :prenom, :nom);
SQL

echo
echo "Compte créé. Connexion : https://admin.${DOMAIN}/admin/login"
echo "(n'importe quel sous-domaine convient, la console n'est pas rattachée à un établissement)"
