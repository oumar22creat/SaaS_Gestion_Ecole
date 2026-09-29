# Déploiement en production

Procédure complète pour installer School Manager sur un serveur, la maintenir et la
restaurer. Écrite pour un VPS **Hostinger KVM 2** (2 vCPU, 8 Go, 100 Go NVMe) sous
**Ubuntu 24.04**, mais rien n'y est propre à cet hébergeur.

## Pourquoi ce dimensionnement

| Service | Mémoire allouée |
|---|---|
| PostgreSQL 16 | 2 Go |
| Backend (JVM) | 2,5 Go |
| Redis | 256 Mo |
| Nginx | 256 Mo |
| **Total conteneurs** | **~5 Go** |

Les 3 Go restants ne sont pas perdus : c'est le cache disque du système, dont PostgreSQL
dépend fortement. Une base privée de cache relit le disque à chaque requête.

Un serveur de 4 Go serait consommé en totalité au repos, sans marge pour un déploiement (où
deux versions du backend coexistent quelques secondes), ni pour une sauvegarde qui compresse
un dump. Et son unique cœur ferait attendre l'API pendant les générations de bulletins en
fin de trimestre.

---

## 1. Le nom de domaine

L'application identifie chaque établissement **par son sous-domaine**
(`TenantResolver#resolveBySubdomain`). Il faut donc deux enregistrements DNS :

| Type | Nom | Valeur |
|---|---|---|
| A | `@` | adresse IP du VPS |
| A | `*` | adresse IP du VPS |

L'enregistrement générique (`*`) est ce qui permet d'ouvrir un établissement sans toucher au
DNS : `lycee-bamako.votredomaine.com` fonctionne dès sa création dans l'application.

**Le DNS doit être hébergé chez Cloudflare** (gratuit, quel que soit votre registrar : il
suffit d'y déléguer les serveurs de noms). Raison : un certificat TLS couvrant `*.domaine`
ne peut pas être validé par la méthode HTTP habituelle — Let's Encrypt exige une preuve DNS,
donc un accès programmatique à la zone. Pour un autre hébergeur DNS, remplacer l'image
`certbot/dns-cloudflare` par le greffon correspondant dans `deploy/docker-compose.prod.yml`.

## 2. Préparer le serveur

```bash
# En root sur le VPS
apt update && apt upgrade -y
apt install -y git curl ufw fail2ban unattended-upgrades awscli

# Pare-feu : rien d'ouvert sauf SSH, HTTP et HTTPS. PostgreSQL et Redis ne sont
# joignables que depuis le réseau Docker interne, jamais depuis Internet.
ufw allow OpenSSH && ufw allow 80 && ufw allow 443 && ufw --force enable

# Docker
curl -fsSL https://get.docker.com | sh

# Un utilisateur non root pour l'application
adduser --disabled-password --gecos "" schoolmanager
usermod -aG docker schoolmanager
```

**Désactiver la connexion SSH par mot de passe** avant d'aller plus loin
(`PasswordAuthentication no` dans `/etc/ssh/sshd_config`, puis `systemctl restart ssh`).
Un serveur exposé avec mot de passe est attaqué en continu dès la première heure.

## 3. Installer l'application

```bash
su - schoolmanager
git clone <url-du-depot> school-manager
cd school-manager

cp deploy/.env.example deploy/.env
chmod 600 deploy/.env
```

Remplir `deploy/.env`. Générer chaque secret avec `openssl rand -base64 36` — ne pas les
inventer à la main, ne pas les réutiliser d'un environnement à l'autre.

```bash
cp deploy/certbot/cloudflare.ini.example deploy/certbot/cloudflare.ini
# y coller un jeton Cloudflare limité à « Zone:DNS:Edit » sur la zone concernée
chmod 600 deploy/certbot/cloudflare.ini
```

## 4. Premier démarrage

```bash
# Certificat générique. À faire AVANT de démarrer Nginx, qui refuse de partir sans lui.
./deploy/init-letsencrypt.sh

docker compose -f deploy/docker-compose.prod.yml --env-file deploy/.env up -d
```

Le backend applique les migrations Flyway à son démarrage. Suivre :

```bash
docker compose -f deploy/docker-compose.prod.yml logs -f backend
```

### Créer le premier Super-Administrateur

Aucun compte n'existe au départ. À insérer directement en base, une seule fois :

```bash
# Empreinte bcrypt du mot de passe choisi
docker compose -f deploy/docker-compose.prod.yml exec backend \
  java -cp app.jar -Dloader.main=org.springframework.security.crypto.bcrypt.BCrypt ...
```

Plus simple en pratique : générer l'empreinte sur votre machine, puis

```sql
INSERT INTO platform_admins (email, password_hash, first_name, last_name, created_at)
VALUES ('vous@votredomaine.com', '$2a$10$…', 'Prénom', 'Nom', now());
```

La console est ensuite accessible sur `https://<n-importe-quel-sous-domaine>/admin/login`.

## 5. Sauvegardes

**C'est la partie à ne pas remettre à plus tard.** Vous détenez les notes, les absences et
les règlements de familles entières. Une perte n'est pas rattrapable : ces informations
n'existent nulle part ailleurs.

Les sauvegardes hebdomadaires proposées par l'hébergeur sont un filet en cas de perte du
serveur entier, pas une stratégie : une semaine de saisie perdue est déjà une catastrophe
pour un établissement.

```bash
crontab -e
```

```cron
# Sauvegarde quotidienne à 2 h du matin (heure du serveur)
0 2 * * * /home/schoolmanager/school-manager/deploy/backup/backup.sh >> /home/schoolmanager/backup.log 2>&1
```

Le script fait trois choses qu'une simple copie ne fait pas :

1. **Il envoie hors du serveur** (espace S3-compatible : Backblaze B2, Scaleway, OVH…).
   Une archive restée sur le disque de la base disparaît avec lui.
2. **Il chiffre** avant l'envoi. Ces fichiers contiennent des données scolaires d'enfants
   mineurs ; ils ne doivent être lisibles que par vous.
3. **Il relit l'archive** qu'il vient d'écrire et compte les tables. Une sauvegarde corrompue
   ou chiffrée avec une mauvaise phrase se découvre ici, pas le jour où on en a besoin.

### Tester la restauration

**Avant la mise en production, puis une fois par trimestre.** Une sauvegarde jamais restaurée
n'est pas une sauvegarde.

```bash
./deploy/backup/restore.sh deploy/backup/archives/school-manager_AAAA-MM-JJ_HHMM.sql.gz.enc
```

Puis se connecter avec un compte d'établissement et vérifier qu'un bulletin s'affiche.

## 6. Mettre à jour

```bash
./deploy/deploy.sh
```

Le script sauvegarde **avant** de migrer — Flyway applique des changements de schéma
irréversibles, et c'est le dernier moment où revenir en arrière reste possible — puis
reconstruit, redémarre et vérifie que le backend répond.

## 7. Surveillance minimale

```bash
# État des conteneurs et consommation
docker compose -f deploy/docker-compose.prod.yml ps
docker stats --no-stream

# Santé applicative
curl -s https://<un-sous-domaine>/actuator/health
```

Trois signaux à surveiller :

- **Mémoire au-delà de 85 %** durablement : passer au KVM 4.
- **Disque au-delà de 80 %** : les archives locales s'accumulent, ou les documents générés.
- **Dernière sauvegarde datant de plus de 24 h** dans `backup.log` : la plus grave des trois,
  et la plus silencieuse.

## Ce que ce déploiement ne fait pas

- **Pas de haute disponibilité.** Un seul serveur : s'il tombe, le service est interrompu
  jusqu'à sa remise en route ou une restauration ailleurs. Acceptable au démarrage, à
  revoir lorsque des dizaines d'établissements en dépendront.
- **Pas de supervision automatique.** Aucune alerte ne part si le serveur s'arrête à 3 h du
  matin. Un service de surveillance externe (UptimeRobot et équivalents, gratuits) comble ce
  manque en quelques minutes ; c'est la première chose à ajouter.
- **Pas de déploiement sans interruption.** `deploy.sh` redémarre le backend : quelques
  secondes de coupure. Sans conséquence pour un usage scolaire diurne si l'on déploie le soir.

## Note de sécurité — en-tête X-Tenant-Id

`TenantResolver` accepte un en-tête `X-Tenant-Id` pour désigner l'établissement. Sur une
requête authentifiée, le JWT a déjà fixé le tenant et l'en-tête est ignoré ; mais sur un
appel anonyme, il serait cru sur parole.

`deploy/nginx/proxy-params.conf` l'efface donc à l'entrée du reverse proxy. **Ne pas retirer
cette ligne** : sans elle, un visiteur pourrait désigner l'établissement de son choix sur les
endpoints publics.
