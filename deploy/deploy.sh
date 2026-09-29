#!/usr/bin/env bash
#
# Mise à jour du serveur avec la version courante du dépôt.
#
# Une sauvegarde est prise AVANT toute migration : Flyway applique des changements de schéma
# irréversibles, et c'est le seul moment où revenir en arrière est encore possible.

set -euo pipefail

RACINE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
COMPOSE="docker compose -f ${RACINE}/docker-compose.prod.yml --env-file ${RACINE}/.env"

echo "1/5  Sauvegarde préalable"
"${RACINE}/backup/backup.sh"

echo "2/5  Récupération du code"
git -C "${RACINE}/.." pull --ff-only

echo "3/5  Construction des images"
${COMPOSE} build backend web

echo "4/5  Redémarrage"
# Le backend d'abord : il applique les migrations Flyway au démarrage, et la nouvelle version
# du front peut appeler des endpoints qui n'existent pas encore dans l'ancienne.
${COMPOSE} up -d backend
${COMPOSE} up -d web

echo "5/5  Vérification"
for i in $(seq 1 30); do
    if ${COMPOSE} exec -T backend wget -qO- http://127.0.0.1:8080/actuator/health | grep -q '"status":"UP"'; then
        echo "Backend opérationnel."
        exit 0
    fi
    sleep 5
done

echo "Le backend ne répond pas après 150 s. Journaux :" >&2
${COMPOSE} logs --tail=60 backend >&2
exit 1
