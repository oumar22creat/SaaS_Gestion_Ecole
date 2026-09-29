#!/usr/bin/env bash
#
# Restauration d'une sauvegarde. À répéter une fois avant la mise en production, puis une
# fois par trimestre : c'est le seul moyen de savoir que les sauvegardes fonctionnent.
#
# Usage : ./restore.sh archives/school-manager_2026-09-29_0300.sql.gz.enc

set -euo pipefail

if [ $# -ne 1 ]; then
    echo "Usage : $0 <archive .sql.gz.enc>" >&2
    exit 1
fi

RACINE="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
# shellcheck source=/dev/null
set -a; source "${RACINE}/.env"; set +a
COMPOSE="docker compose -f ${RACINE}/docker-compose.prod.yml --env-file ${RACINE}/.env"

echo "Cette opération écrase la base ${DB_NAME} par le contenu de :"
echo "  $1"
echo
read -r -p "Taper le nom de la base pour confirmer : " CONFIRME
if [ "${CONFIRME}" != "${DB_NAME}" ]; then
    echo "Abandon." >&2
    exit 1
fi

# Le backend est arrêté pendant la restauration : le laisser écrire dans une base à moitié
# remplacée produirait un état incohérent, pire que la panne d'origine.
echo "Arrêt du backend…"
${COMPOSE} stop backend

echo "Restauration…"
openssl enc -d -aes-256-cbc -pbkdf2 -pass "pass:${BACKUP_PASSPHRASE}" -in "$1" \
  | gunzip \
  | ${COMPOSE} exec -T postgres psql --username "${DB_ADMIN_USER}" --dbname "${DB_NAME}"

echo "Redémarrage du backend…"
${COMPOSE} start backend

echo "Terminé. Vérifier la connexion d'un établissement avant de considérer l'incident clos."
