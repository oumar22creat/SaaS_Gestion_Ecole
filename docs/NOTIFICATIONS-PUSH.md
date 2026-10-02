# Notifications push — obtenir les clés

Procédure pour récupérer, chez Firebase et chez Apple, les quatre éléments dont le code aura
besoin pour envoyer des notifications push vers les applications mobiles.

**Ce document ne couvre que la collecte des clés.** Le code qui les consomme n'est pas encore
écrit : `LoggingNotificationGateway` journalise l'intention d'envoyer sans rien envoyer (voir
`docs/ARCHITECTURE.md` ADR-020 et ses « Points ouverts »). La liste de ce qui reste à faire
est en fin de page.

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

Les deux plateformes partagent le même identifiant, à saisir à l'identique dans les consoles
(`mobile/capacitor.config.ts`, `PRODUCT_BUNDLE_IDENTIFIER` du projet Xcode) :

```
com.schoolsaas.mobile
```

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
2. **Nom du package** : `com.schoolsaas.mobile` — doit correspondre exactement, sinon les
   jetons sont refusés à l'exécution.
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
2. **ID du bundle** : `com.schoolsaas.mobile`, identique à Android.
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
6. **Continue**, puis **Register**.
7. **Download.**

> **Ce téléchargement n'est possible qu'une seule fois.** Le fichier, nommé
> `AuthKey_XXXXXXXXXX.p8`, ne peut plus être récupéré ensuite — seulement révoqué et recréé.
> Le ranger immédiatement dans un gestionnaire de mots de passe.

Une même clé APNs vaut pour le développement et pour la production, et pour toutes les
applications du compte. Il n'y en a qu'une à créer.

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
2. Section **Configuration de l'application iOS** → **Clé d'authentification APNs** →
   **Importer**.
3. Déposer le fichier `.p8`, puis saisir le **Key ID** et le **Team ID** relevés en 4.2.

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
chown schoolmanager:schoolmanager /home/schoolmanager/school-manager/deploy/secrets/fcm.json
chmod 600 /home/schoolmanager/school-manager/deploy/secrets/fcm.json
```

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

## Récapitulatif

- [ ] Projet Firebase créé, sans Google Analytics
- [ ] `google-services.json` dans `mobile/android/app/`
- [ ] `GoogleService-Info.plist` dans `mobile/ios/App/App/`
- [ ] Clé APNs `.p8` créée, rangée dans un gestionnaire de mots de passe
- [ ] Key ID et Team ID relevés
- [ ] App ID `com.schoolsaas.mobile` déclaré chez Apple avec la capacité Push
- [ ] Clé APNs téléversée dans Firebase → Cloud Messaging
- [ ] Clé de compte de service déposée sur le VPS, en lecture restreinte

---

## Ce qui reste à coder

Les clés ne suffisent pas : aucune des briques suivantes n'existe aujourd'hui.

| Brique | État |
|---|---|
| Table `device_tokens` + points d'API d'enregistrement | **à écrire** — rien ne sait aujourd'hui à quel appareil parler |
| Plugin `@capacitor/push-notifications` côté mobile | **à installer** — l'application ne demande jamais la permission |
| `FcmNotificationGateway` remplaçant `LoggingNotificationGateway` | **à écrire** — l'interface existe déjà (ADR-020) |
| Capacité Push dans le projet Xcode | **à activer** — aucun fichier `.entitlements` |
| Purge des jetons morts (`UNREGISTERED`) | **à écrire** |

Et trois défauts fonctionnels à corriger au passage, repérés en lisant le code existant :

1. **Les absences ne toucheraient personne.** `AttendanceService#notifyIfNeeded` appelle le
   registre avec une liste de destinataires **vide** : ce chemin a été conçu pour le SMS, qui
   vise les tuteurs par numéro de téléphone. Il faut résoudre les comptes des familles depuis
   l'élève.
2. **`NEW_GRADE` n'est jamais déclenché**, ni `NEW_DOCUMENT`, ni `SUBSCRIPTION_ALERT`. Les
   types existent dans `NotificationType`, les appels manquent — alors qu'une note saisie est
   ce qu'un parent attend le plus d'être averti.
3. **Les préférences ne sont réglables que sur le web.** L'application mobile n'a pas d'écran
   pour cela. Un parent qui ne consulte que son téléphone ne peut pas couper ce qui
   l'importune, ce qui mène au refus de la permission système — qu'on ne peut plus redemander
   ensuite.

---

## Tester

Le **simulateur iOS ne reçoit pas de notifications push réelles** : la vérification de bout en
bout exigera un iPhone physique. L'émulateur Android convient, s'il embarque les services
Google Play.

## Et le SMS ?

`HttpSmsGateway` est une implémentation **réelle**, déjà branchée sur les absences avec les
bons destinataires, et désactivée faute d'opérateur (`SMS_ENABLED=false`). Il ne lui manque
qu'une URL et des identifiants. Pour prévenir les familles, c'est le chemin le plus court — et
le commentaire de `application.yml` rappelle pourquoi : « Canal principal vers les familles au
Mali : tous les parents n'ont pas de smartphone. » Le push garde sa valeur pour les
enseignants, qui ouvrent l'application tous les jours.
