#!/usr/bin/env bash
#
# Sauvegarde quotidienne de la base et des fichiers téléversés.
#
# Trois principes, et chacun vient d'une manière de perdre des données :
#   1. Hors du serveur. Une archive sur le même disque que la base disparaît avec lui.
#   2. Chiffrée. Elle contient les noms, les notes et les absences d'enfants mineurs, ainsi
#      que les règlements des familles. Elle ne doit être lisible que par vous.
#   3. Vérifiée. Une sauvegarde jamais restaurée n'est pas une sauvegarde : le script relit
#      l'archive qu'il vient d'écrire avant de la déclarer bonne.
#
# Installation : voir docs/DEPLOIEMENT.md, section « Sauvegardes ».

set -euo pipefail

RACINE="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
# shellcheck source=/dev/null
set -a; source "${RACINE}/.env"; set +a

COMPOSE="docker compose -f ${RACINE}/docker-compose.prod.yml --env-file ${RACINE}/.env"
DATE="$(date +%Y-%m-%d_%H%M)"
LOCAL="${RACINE}/backup/archives"
mkdir -p "${LOCAL}"

ARCHIVE="${LOCAL}/school-manager_${DATE}.sql.gz.enc"
FICHIERS="${LOCAL}/storage_${DATE}.tar.gz.enc"

journal() { echo "[$(date '+%Y-%m-%d %H:%M:%S')] $*"; }

# ------------------------------------------------------------------------------ Base
journal "Dump de ${DB_NAME}…"
# --clean --if-exists : l'archive sait recréer par-dessus une base existante, ce qui évite
# d'avoir à la détruire à la main au pire moment.
${COMPOSE} exec -T postgres pg_dump \
    --username "${DB_ADMIN_USER}" --dbname "${DB_NAME}" \
    --clean --if-exists --no-owner \
  | gzip -9 \
  | openssl enc -aes-256-cbc -pbkdf2 -salt -pass "pass:${BACKUP_PASSPHRASE}" \
  > "${ARCHIVE}"

TAILLE=$(du -h "${ARCHIVE}" | cut -f1)
journal "Base sauvegardée (${TAILLE})."

# --------------------------------------------------------------------------- Fichiers
# Logos d'établissement et documents générés : un bulletin sans le logo de son école n'est
# plus le même document.
journal "Archive des fichiers téléversés…"
${COMPOSE} exec -T backend tar -cf - -C /app storage \
  | gzip -9 \
  | openssl enc -aes-256-cbc -pbkdf2 -salt -pass "pass:${BACKUP_PASSPHRASE}" \
  > "${FICHIERS}"

# ------------------------------------------------------------------------ Vérification
# Le dump est relu et déchiffré : une archive corrompue ou chiffrée avec une mauvaise phrase
# se découvre ici, pas le jour où on en a besoin.
journal "Vérification de l'archive…"
LIGNES=$(openssl enc -d -aes-256-cbc -pbkdf2 -pass "pass:${BACKUP_PASSPHRASE}" -in "${ARCHIVE}" \
         | gunzip | grep -c "CREATE TABLE" || true)
if [ "${LIGNES}" -lt 10 ]; then
    journal "ÉCHEC : l'archive ne contient que ${LIGNES} tables. Sauvegarde non fiable."
    exit 1
fi
journal "Archive relue : ${LIGNES} tables présentes."

# ------------------------------------------------------------------------ Envoi distant
if [ -n "${BACKUP_S3_BUCKET:-}" ]; then
    journal "Envoi vers ${BACKUP_S3_BUCKET}…"
    for f in "${ARCHIVE}" "${FICHIERS}"; do
        AWS_ACCESS_KEY_ID="${BACKUP_S3_ACCESS_KEY}" \
        AWS_SECRET_ACCESS_KEY="${BACKUP_S3_SECRET_KEY}" \
        aws --endpoint-url "${BACKUP_S3_ENDPOINT}" s3 cp "$f" "s3://${BACKUP_S3_BUCKET}/$(basename "$f")"
    done
    journal "Envoi terminé."
else
    journal "ATTENTION : aucune destination distante configurée. La sauvegarde reste sur ce serveur, donc elle ne protège pas d'une perte du serveur."
fi

# -------------------------------------------------------------------------- Rétention
find "${LOCAL}" -name '*.enc' -mtime "+${BACKUP_RETENTION_DAYS:-30}" -delete
journal "Terminé."
