# Notifications push

Procédure pour récupérer, chez Firebase et chez Apple, les quatre éléments dont le code a
besoin pour envoyer des notifications push vers les applications mobiles — puis pour
diagnostiquer un envoi qui n'arrive pas.

**La chaîne est en service.** Vérifiée de bout en bout le 14 octobre 2026 sur un iPhone
physique, build TestFlight n° 32 : saisie d'une absence → backend → Firebase → APNs de
production → notification affichée. `FcmNotificationGateway` remplace
`LoggingNotificationGateway` dès que `app.fcm.enabled` vaut vrai (voir
`docs/ARCHITECTURE.md` ADR-020).

Les erreurs rencontrées pour y parvenir sont consignées au §7 : elles se ressemblent toutes,
et se distinguent mal sans méthode.

---

## Les quatre éléments à obtenir

| # | Élément | Qui le délivre | Où il finit |
|---|---|---|---|
| 1 | `google-services.json` | Firebase | `mobile/android/app/` |
| 2 | `GoogleService-Info.plist` | Firebase | `mobile/ios/App/App/` |
| 3 | Clé d'authentification APNs (`.p8`) | Apple | téléversée **chez Firebase**, jamais dans le dépôt |
| 4 | Clé de compte de service (`.json`) | Firebase | secret sur le VPS, jamais dans le dépôt |

Les deux premiers s'ajoutent au dépôt : ils ne contiennent aucun secret, seulement des
identifiants publics de projet. Les deux derniers sont des secrets.

### Identifiant de l'application

Les deux plateformes n'ont **pas** le même identifiant, à saisir à l'identique dans les
consoles :

| Plateforme | Identifiant | Où il est défini |
|---|---|---|
| Android | `tech.schoolmanager.app` | `applicationId` de `mobile/android/app/build.gradle` |
| iOS | `com.schoolsaas.mobile` | `PRODUCT_BUNDLE_IDENTIFIER` du projet Xcode, `appId` de `mobile/capacitor.config.ts` |

`com.schoolsaas.mobile` était déjà pris sur Google Play ; l'application iOS, déjà publiée,
garde le sien. Détails dans `docs/FICHES-STORES.md` §5.

---

## 1. Créer le projet Firebase

Firebase Cloud Messaging (FCM) achemine les notifications vers Android **et** vers iOS. Même
pour iOS, c'est Firebase qui parle à Apple — d'où la clé APNs qu'on lui confie à l'étape 5.

1. Ouvrir [console.firebase.google.com](https://console.firebase.google.com) avec un compte
   Google.
2. **Créer un projet**, le nommer par exemple `school-manager`.
3. Google Analytics est proposé : **le refuser**. Il n'apporte rien ici et ajoute une collecte
   de données sur des élèves mineurs, qu'il faudrait alors documenter auprès des familles.

Le plan gratuit (Spark) suffit : FCM n'est pas facturé, quel que soit le volume.

---

## 2. Déclarer l'application Android

1. Sur la page d'accueil du projet, bouton **Android**.
2. **Nom du package** : `tech.schoolmanager.app` — doit correspondre exactement à
   l'`applicationId`, sinon le build échoue (`No matching client found`).
3. Surnom et empreinte SHA-1 : facultatifs. Le SHA-1 ne sert qu'à la connexion Google, que
   l'application n'utilise pas.
4. **Télécharger `google-services.json`** et le déposer dans :

```
mobile/android/app/google-services.json
```

5. Firebase propose ensuite d'ajouter des lignes de build Gradle : **passer cette étape**, le
   code s'en chargera.

---

## 3. Déclarer l'application iOS

1. Page d'accueil du projet → **Ajouter une application** → **iOS**.
2. **ID du bundle** : `com.schoolsaas.mobile` (différent d'Android, voir plus haut).
3. **Télécharger `GoogleService-Info.plist`** et le déposer dans :

```
mobile/ios/App/App/GoogleService-Info.plist
```

> Déposer le fichier dans le dossier ne suffit pas : sous Xcode il devra aussi être **ajouté à
> la cible** `App`, sans quoi il n'entre pas dans le binaire. C'est une étape de code.

---

## 4. Créer la clé APNs chez Apple

C'est la partie qui exige un **compte Apple Developer payant** (99 $/an) — nécessaire de toute
façon pour publier sur l'App Store.

### 4.1 Créer la clé

1. Ouvrir [developer.apple.com/account](https://developer.apple.com/account).
2. **Certificates, Identifiers & Profiles** → **Keys** (menu de gauche).
3. Bouton **+**.
4. Nommer la clé, par exemple `School Manager — APNs`.
5. Cocher **Apple Push Notifications service (APNs)**.
6. **Cliquer sur le bouton `Configure` de cette ligne**, et choisir **Sandbox & Production**.
7. **Continue**, puis **Register**.
8. **Download.**

> **L'étape 6 n'est pas facultative.** Sans elle, Apple crée une clé *Sandbox only*, qui
> fonctionne depuis Xcode sur un appareil branché et échoue pour toute application distribuée
> par TestFlight ou par l'App Store. Rien ne le signale : Firebase répond
> `THIRD_PARTY_AUTH_ERROR — Invalid APNs credential`, erreur identique à celle d'une clé
> absente. Le diagnostic est au §7.2.

> **Ce téléchargement n'est possible qu'une seule fois.** Le fichier, nommé
> `AuthKey_XXXXXXXXXX.p8`, ne peut plus être récupéré ensuite — seulement révoqué et recréé.
> Le ranger immédiatement dans un gestionnaire de mots de passe.

Une clé créée en **Sandbox & Production** vaut pour les deux environnements et pour toutes
les applications du compte. Il n'y en a qu'une à créer — mais elle se téléverse **deux fois**
chez Firebase, voir §5.

### 4.2 Relever les deux identifiants qui l'accompagnent

| Identifiant | Où le lire |
|---|---|
| **Key ID** (10 caractères) | la partie variable du nom de fichier : `AuthKey_`**`3N9MXTE249`**`.p8` |
| **Team ID** (10 caractères) | page **Membership details** du compte développeur |

### 4.3 Déclarer l'identifiant d'application chez Apple

Toujours dans **Certificates, Identifiers & Profiles** :

1. **Identifiers** → **+** → **App IDs** → **App**.
2. **Bundle ID** : `com.schoolsaas.mobile`, en mode **Explicit**.
3. Dans la liste des capacités, cocher **Push Notifications**.
4. **Continue**, puis **Register**.

Sans cette déclaration, Apple refuse les jetons même si la clé est valide.

---

## 5. Téléverser la clé APNs chez Firebase

C'est ce qui relie les deux mondes : Firebase se servira de cette clé pour parler à Apple en
votre nom.

1. Console Firebase → **⚙ Paramètres du projet** → onglet **Cloud Messaging**.
2. Section **Configuration de l'application Apple** → **Clé d'authentification APNs**.

Firebase y présente **deux emplacements distincts**, et c'est le piège principal de toute
cette procédure :

| Emplacement | Qui l'utilise |
|---|---|
| Clé APNs **de développement** | les builds lancées depuis Xcode sur un appareil branché |
| Clé APNs **de production** | **TestFlight et l'App Store** |

3. Importer le **même fichier `.p8`** dans **les deux**, avec le même **Key ID** et le même
   **Team ID** relevés en 4.2.

Notre application porte `aps-environment = production` dans
`mobile/ios/App/App/App.entitlements`, ce qui est le réglage correct pour une app distribuée.
Firebase cherche donc la clé de **production** : l'emplacement de développement seul rempli,
aucune notification n'arrive, et l'erreur renvoyée est la même que si aucune clé n'existait.

---

## 6. Créer la clé de compte de service (serveur)

Le backend doit s'authentifier auprès de FCM pour demander un envoi.

1. Console Firebase → **⚙ Paramètres du projet** → onglet **Comptes de service**.
2. **Générer une nouvelle clé privée** → confirmer.
3. Un fichier JSON se télécharge. **C'est un secret** : il ouvre l'envoi de notifications à
   tous les utilisateurs de l'application.

### Déposer la clé sur le serveur

Elle ne va ni dans le dépôt, ni dans `deploy/.env` — c'est un fichier, pas une valeur :

```bash
scp -i ~/.ssh/schoolmanager_vps fcm-service-account.json \
    root@VOTRE_IP:/home/schoolmanager/school-manager/deploy/secrets/fcm.json
```

Puis, sur le serveur, en restreindre la lecture :

```bash
cd /home/schoolmanager/school-manager/deploy
# Le groupe 101 est celui de l'utilisateur « app » DANS le conteneur, pas sur l'hôte.
# C'est lui qui doit pouvoir lire : le conteneur ne tourne pas en root, et un fichier
# en 600 appartenant à schoolmanager fait échouer le démarrage du backend sur un
# « Permission denied » qui ne mentionne jamais les droits du montage.
chown -R schoolmanager:101 secrets
chmod 750 secrets
chmod 640 secrets/fcm.json
```

> Sur l'hôte, le gid 101 est celui de `syslog`, qui n'a aucun membre : personne d'autre
> que le conteneur ne gagne l'accès. Vérifiez-le quand même avec `getent group 101` avant
> d'appliquer, les numéros de groupe système varient d'une distribution à l'autre.

> **Ces deux réglages n'existent pas encore** et ne seront lus par rien tant que la passerelle
> FCM n'est pas écrite. Ils sont donnés ici pour que la clé soit déjà en place, et parce que
> le nom retenu suit la convention des autres réglages de `deploy/.env` :
>
> ```
> FCM_ENABLED=true
> FCM_CREDENTIALS_PATH=/run/secrets/fcm.json
> ```
>
> `FCM_ENABLED=false` conservera le comportement actuel — notifications journalisées, jamais
> envoyées. Même interrupteur que `SMS_ENABLED`, et pour la même raison : rien ne part tant
> que l'exploitant ne l'a pas décidé.

---

## 7. Diagnostiquer un envoi qui n'arrive pas

### 7.1 Lire la chaîne dans l'ordre

Une notification qui n'arrive pas peut s'être arrêtée à quatre endroits. Les distinguer prend
deux requêtes, et évite de chercher une clé invalide quand le message n'est tout simplement
jamais parti.

```bash
# 1. L'événement a-t-il été dispatché, et vers combien de comptes ?
docker exec school-manager-postgres-1 psql -U school_saas_admin -d school_saas \
  -c "SELECT * FROM notification_log ORDER BY id DESC LIMIT 5;"

# 2. Un appareil est-il enregistré pour ce compte ?
docker exec school-manager-postgres-1 psql -U school_saas_admin -d school_saas \
  -c "SELECT u.email, d.platform, length(d.token) FROM device_tokens d
      JOIN users u ON u.id = d.user_id;"

# 3. FCM a-t-il refusé l'envoi ?
docker logs --since 5m school-manager-backend-1 2>&1 | grep -i push
```

| Observation | Interprétation |
|---|---|
| aucune ligne dans `notification_log` | `dispatch` n'a pas été appelé, ou tous les destinataires se sont désabonnés |
| ligne avec `recipient_count = 0` | l'élève n'a aucun tuteur titulaire d'un compte |
| ligne avec un destinataire, aucun appareil en table | la famille n'a pas installé l'application |
| `WARN … Envoi push refusé` | la chaîne va jusqu'à Firebase : voir §7.2 |
| aucun des trois | l'envoi a réussi ; chercher du côté de l'affichage dans l'app |

> **Le piège de la feuille d'appel.** `submitRollCall` ne notifie qu'à la **création** d'un
> enregistrement. Ressaisir une absence sur une date déjà saisie passe par `applyChange`, qui
> met à jour sans rien envoyer et renvoie pourtant un `201`. Pour tester, choisir une date
> libre — et le vérifier en base plutôt que de s'en remettre à l'interface :
>
> ```bash
> docker exec school-manager-postgres-1 psql -U school_saas_admin -d school_saas \
>   -At -c "SELECT date FROM attendance_records WHERE student_id = <id> ORDER BY date;"
> ```
>
> Après coup, `created_at` sur la ligne créée doit porter l'heure de l'essai. S'il porte une
> date antérieure, l'enregistrement existait déjà et aucune notification n'est partie.

### 7.2 `THIRD_PARTY_AUTH_ERROR — Invalid APNs credential`

Firebase a accepté le jeton mais Apple a refusé la clé. Trois causes, de la plus fréquente à
la plus rare :

1. **La clé n'est pas dans l'emplacement de production** chez Firebase (§5). C'est la cause la
   plus courante, car l'emplacement de développement est celui qu'on remplit spontanément.
2. **La clé est *Sandbox only*** : le bouton `Configure` a été laissé sur son réglage par
   défaut à la création (§4.1). Une clé *Sandbox only* est irrécupérable — il faut en créer
   une nouvelle.
3. **Le Team ID saisi chez Firebase est erroné.** Il se tape à la main, Firebase ne le déduit
   pas du fichier.

Pour trancher entre la 2 et les autres, interroger Apple directement avec un faux jeton
d'appareil. La réponse ne dépend alors que de la clé :

| Réponse d'Apple | Signification |
|---|---|
| `400 BadDeviceToken` | la clé est **acceptée** pour cet environnement — le faux jeton est la seule chose refusée |
| `403 BadEnvironmentKeyInToken` | la clé est **refusée** pour cet environnement |

```bash
python3 - <<'SCRIPT'
import base64, json, time, subprocess, os
from cryptography.hazmat.primitives import hashes, serialization
from cryptography.hazmat.primitives.asymmetric import ec, utils

KEY_ID, TEAM_ID, BUNDLE = "XXXXXXXXXX", "253HSP874T", "com.schoolsaas.mobile"
CLE = os.path.expanduser(f"~/Downloads/AuthKey_{KEY_ID}.p8")
b64 = lambda d: base64.urlsafe_b64encode(d).rstrip(b"=").decode()
entete = b64(json.dumps({"alg": "ES256", "kid": KEY_ID}).encode())
charge = b64(json.dumps({"iss": TEAM_ID, "iat": int(time.time())}).encode())
cle = serialization.load_pem_private_key(open(CLE, "rb").read(), password=None)
r, s = utils.decode_dss_signature(
    cle.sign(f"{entete}.{charge}".encode(), ec.ECDSA(hashes.SHA256())))
jwt = f"{entete}.{charge}.{b64(r.to_bytes(32, 'big') + s.to_bytes(32, 'big'))}"

for nom, hote in [("production", "api.push.apple.com"),
                  ("bac a sable", "api.sandbox.push.apple.com")]:
    res = subprocess.run([
        "curl", "-s", "-o", "/tmp/apns.json", "-w", "%{http_code}", "--http2",
        "-H", f"authorization: bearer {jwt}", "-H", f"apns-topic: {BUNDLE}",
        "-H", "apns-push-type: alert", "-d", '{"aps":{"alert":"test"}}',
        f"https://{hote}/3/device/{'00' * 32}"], capture_output=True, text=True)
    motif = json.load(open("/tmp/apns.json")).get("reason", "—")
    print(f"  {nom:12} HTTP {res.stdout:4} {motif}")
SCRIPT
```

Une clé valable pour les deux environnements répond `BadDeviceToken` aux deux lignes. Si la
clé passe en production mais que Firebase continue de la refuser, la cause est 1 ou 3 : il n'y
a plus rien à chercher du côté d'Apple, tout se joue dans le formulaire de la console.

---

## 8. Cycle de vie du jeton d'appareil

Code : `mobile/src/app/push/push-notification.service.ts`, côté serveur
`backend/src/main/java/com/schoolsaas/notification/DeviceTokenService.java`.

| Moment | Ce que fait l'application |
|---|---|
| **Connexion** | demande la permission (seul moment où elle l'est), `getToken()`, `POST /api/v1/device-tokens`, abonnement à `tokenReceived` |
| **Lancement avec une session conservée** | si la permission est **déjà** accordée : même chemin, sans jamais la redemander (`reprendreAuDemarrage`, déclenché depuis `app.config.ts` sans bloquer le démarrage). Refusée ou jamais tranchée : rien |
| **Renouvellement par Firebase** (`tokenReceived`) | redéclaré au serveur, **seulement si une session est ouverte** |
| **Déconnexion** | attend un enregistrement encore en cours, prend le jeton en mémoire ou le redemande à `getToken()`, envoie `DELETE /api/v1/device-tokens/{token}` **avant** l'effacement de la session (le JWT est nécessaire) — le tout plafonné à 3 s ; puis `deleteToken()` chez Firebase, plafonné à 2 s. Hors ligne, la déconnexion aboutit donc en 5 s au plus ; le bouton est désactivé pendant ce temps |
| **Session perdue** (le serveur refuse le renouvellement : 400/401/403) | plus de requête authentifiée possible : `deleteToken()` seul (intercepteur HTTP), une seule fois même si plusieurs requêtes échouent ensemble. Une coupure réseau pendant le renouvellement ne ferme **pas** la session |

Deux gardes empêchent une réponse tardive de défaire la déconnexion :

- un renouvellement du jeton d'accès qui répond après l'effacement de la session n'est pas
  stocké (`AuthTokenService.generation()`, contrôlée par `TokenRefreshService`) — sans cela,
  la personne suivante rouvrait l'application sur le compte précédent ;
- aucune déclaration (`POST`) ne part hors session, et un retrait abandonné au délai ne part
  pas si un autre compte s'est connecté entre-temps.

Côté serveur :

- Un jeton déjà connu dans l'établissement est **réattribué** au compte qui l'enregistre
  (unicité `(school_id, token)`) : pas de doublon entre deux comptes d'une même école.
- Le `DELETE` ne retire que la ligne **du compte authentifié**, dans **son** établissement
  (filtre Hibernate + RLS). Un collègue, ou un ancien propriétaire après réattribution, ne
  peut pas priver un compte de ses notifications.

Pourquoi `deleteToken()` en plus du `DELETE` : c'est le filet de sécurité. Si le retrait n'a
pas atteint le serveur (réseau coupé), la ligne qui y reste désigne un jeton que Firebase ne
connaît plus — il répond `UNREGISTERED` au premier envoi et la passerelle l'oublie (§7). La
connexion suivante obtient un jeton neuf par `getToken()`. C'est aussi ce qui couvre le cas
d'un même téléphone utilisé dans **deux établissements** : la ligne de la première école,
invisible depuis la seconde, ne peut pas y être réattribuée.

> **Historique.** Jusqu'à la version Android 1 / iOS build 35, le jeton n'était connu qu'en
> mémoire, rempli par la seule page de connexion. La session survivant au redémarrage, une
> déconnexion faite après réouverture de l'application n'envoyait aucun `DELETE` : sur un
> téléphone partagé, le compte précédent continuait de recevoir ses notifications. Corrigé à
> partir de la version Android 2 / iOS build 36.

## Récapitulatif

- [ ] Projet Firebase créé, sans Google Analytics
- [ ] `google-services.json` dans `mobile/android/app/`
- [ ] `GoogleService-Info.plist` dans `mobile/ios/App/App/`
- [ ] Clé APNs `.p8` créée **en Sandbox & Production**, rangée dans un gestionnaire de mots de passe
- [ ] Key ID et Team ID relevés
- [ ] App ID `com.schoolsaas.mobile` déclaré chez Apple avec la capacité Push
- [ ] Clé APNs téléversée dans Firebase, dans l'emplacement **de développement**
- [ ] Clé APNs téléversée dans Firebase, dans l'emplacement **de production** — le même fichier
- [ ] Clé de compte de service déposée sur le VPS, en lecture restreinte
- [ ] Notification reçue sur un appareil physique, build TestFlight

---

## Ce qui reste à coder

| Brique | État |
|---|---|
| Table `device_tokens` + points d'API d'enregistrement | ✅ en service |
| `@capacitor-firebase/messaging` côté mobile | ✅ en service — rend un vrai jeton FCM, là où `@capacitor/push-notifications` ne rend que le jeton APNs brut |
| Renvoi du jeton APNs dans `AppDelegate` | ✅ en service — sans lui, Capacitor n'est jamais prévenu et rien ne remonte, **sans la moindre erreur** |
| `FcmNotificationGateway` remplaçant `LoggingNotificationGateway` | ✅ en service |
| `App.entitlements` avec `aps-environment = production` | ✅ en service |
| Purge des jetons morts (`UNREGISTERED`) | ✅ en service |
| Résolution des comptes familles (`FamilyRecipients`) | ✅ en service |
| Écran de préférences dans l'application mobile | ✅ en service |

Restent deux types déclarés dans `NotificationType` que personne ne déclenche :

| Type | État |
|---|---|
| `NEW_DOCUMENT` | **à brancher** — aucun appel au registre |
| `SUBSCRIPTION_ALERT` | **à brancher** — aucun appel au registre |

Tant qu'ils ne sont pas branchés, un document déposé et un abonnement qui expire ne
préviennent personne : le type existe, l'appel manque.

---

## Tester

Le **simulateur iOS ne reçoit pas de notifications push réelles** : la vérification de bout en
bout exige un iPhone physique, et une build distribuée par TestFlight — une build lancée
depuis Xcode utilise le bac à sable, donc l'autre emplacement de clé chez Firebase (§5).
L'émulateur Android convient, s'il embarque les services Google Play.

La marche à suivre pour provoquer un envoi, et lire où il s'est arrêté s'il n'arrive pas, est
au §7.

## Et le SMS ?

`HttpSmsGateway` est une implémentation **réelle**, déjà branchée sur les absences avec les
bons destinataires, et désactivée faute d'opérateur (`SMS_ENABLED=false`). Il ne lui manque
qu'une URL et des identifiants. Pour prévenir les familles, c'est le chemin le plus court — et
le commentaire de `application.yml` rappelle pourquoi : « Canal principal vers les familles au
Mali : tous les parents n'ont pas de smartphone. » Le push garde sa valeur pour les
enseignants, qui ouvrent l'application tous les jours.
