#!/usr/bin/env bash
#
# Première émission du certificat générique. À lancer une seule fois, avant le premier
# démarrage de Nginx : celui-ci refuse de démarrer sans certificat, et le certificat ne peut
# pas être obtenu pendant qu'il tourne. D'où ce script séparé.
#
# Un certificat couvrant *.domaine ne peut pas être validé par la méthode HTTP : Let's Encrypt
# exige une preuve DNS. C'est pourquoi un jeton d'API est nécessaire (deploy/certbot/).

set -euo pipefail

RACINE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=/dev/null
set -a; source "${RACINE}/.env"; set +a

if [ ! -f "${RACINE}/certbot/cloudflare.ini" ]; then
    echo "Manque ${RACINE}/certbot/cloudflare.ini — copier cloudflare.ini.example et y mettre le jeton." >&2
    exit 1
fi
chmod 600 "${RACINE}/certbot/cloudflare.ini"

COMPOSE="docker compose -f ${RACINE}/docker-compose.prod.yml --env-file ${RACINE}/.env"

echo "Demande d'un certificat pour ${DOMAIN} et *.${DOMAIN}…"
${COMPOSE} run --rm --entrypoint certbot certbot \
    certonly --dns-cloudflare \
    --dns-cloudflare-credentials /etc/cloudflare.ini \
    --dns-cloudflare-propagation-seconds 60 \
    --email "${LETSENCRYPT_EMAIL}" --agree-tos --no-eff-email \
    -d "${DOMAIN}" -d "*.${DOMAIN}"

echo
echo "Certificat obtenu. Démarrer la pile :"
echo "  docker compose -f deploy/docker-compose.prod.yml --env-file deploy/.env up -d"
