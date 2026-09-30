#!/bin/sh
# Recharge Nginx toutes les six heures, pour qu'un certificat renouvelé par certbot soit pris
# en compte sans redémarrage manuel.
#
# Pourquoi ici et non dans `command:` de docker-compose : l'entrypoint de l'image nginx ne
# lance la substitution des variables des fichiers *.template QUE si la commande commence par
# « nginx ». Une commande `sh -c "…"` la saute en silence, et la configuration du site n'est
# alors jamais générée — seul le default.conf d'origine reste, qui sert la page d'accueil de
# Nginx. Les scripts de /docker-entrypoint.d/, eux, s'exécutent avant le démarrage.
( while :; do sleep 6h; nginx -s reload 2>/dev/null || true; done ) &
