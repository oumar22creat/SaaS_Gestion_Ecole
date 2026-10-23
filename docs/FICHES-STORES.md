# Fiches des stores — School Manager

Tout ce qui se colle dans Google Play Console et App Store Connect. Les champs sont donnés
dans l'ordre des formulaires, avec leur limite de caractères, pour qu'il n'y ait rien à
réécrire sur place.

Les étapes marquées 🔑 exigent une authentification sur vos comptes de développeur et vous
reviennent donc.

---

## Ce qui est prêt

| Élément | État |
|---|---|
| Bundle Android `.aab` signé | ✅ `mobile/android/app/build/outputs/bundle/release/app-release.aab`, 4,4 Mo, reconstruit le 22/10/2026 à 14 h 03 — paquet `tech.schoolmanager.app`, Firebase configuré (compte Google oumarcoulou2@gmail.com, appli « School Manager Play ») |
| Publi­cation iOS sur l'App Store | ✅ Publié le 22/10/2026 (version 1.0, build 35) — lien : https://apps.apple.com/app/id6822008457 |
| Build Google Play | ⏳ bundle prêt, à téléverser dans un test fermé sur Play Console |
| Politique de confidentialité en ligne | ✅ https://schoolmanager.tech/confidentialite.html (HTTP 200) |
| Textes de fiche | ✅ ci-dessous |
| Captures d'écran | ⏳ à prendre sur un appareil — voir §4 |

> **Attention au bundle Android.** Celui du 6 octobre qui traînait dans `build/outputs` était
> antérieur à tout le travail récent, notifications push comprises. Il a été reconstruit. Pour
> le refaire plus tard, la commande est au §5 : la chaîne exige **Node ≥ 22.22.3** et
> **JDK 21**, et les versions par défaut de la machine ne conviennent ni l'une ni l'autre.

---

## 1. Identité de l'application

| Champ | Valeur |
|---|---|
| Nom (Play, 30 car. max) | `School Manager` |
| Nom (App Store, 30 car. max) | `School Manager Tech` — 19 caractères. Le nom simple était indisponible : sur l'App Store il doit être unique sur toute la boutique |
| Nom affiché sur l'appareil | `School Manager` (`CFBundleDisplayName`) — aucune contrainte d'unicité ici. L'écart avec le nom de fiche est admis par Apple tant que les deux restent manifestement liés, et « Tech » n'est qu'un suffixe |
| Identifiant de paquet Android | `tech.schoolmanager.app` (`applicationId`). `com.schoolsaas.mobile` était déjà pris sur Google Play — l'identifiant est unique sur toute la boutique et définitif une fois l'application créée |
| Identifiant de bundle iOS | `com.schoolsaas.mobile` — **inchangé** : c'est celui de l'application publiée sur l'App Store le 22/10/2026, qu'on ne peut plus modifier sans publier une autre application |
| Version | 1.0 |
| Éditeur | logilab — Bamako, Mali |
| Langue par défaut | Français (France) |
| Autres langues | aucune pour l'instant |

### Catégorie

| Store | Catégorie | Pourquoi |
|---|---|---|
| Google Play | **Éducation** | catégorie d'application, pas de jeu |
| App Store | **Éducation**, catégorie secondaire **Productivité** | |

Ne pas choisir « Parentalité » sur Play : cette catégorie attire un examen supplémentaire
destiné aux applications de contrôle parental, qui n'est pas notre cas.

---

## 2. Textes

### 2.1 Google Play — description courte (80 caractères)

```
Suivi scolaire des élèves : notes, absences, scolarité et emploi du temps.
```
*(73 caractères)*

### 2.2 Google Play — description complète (4 000 caractères)

```
School Manager relie l'école et la famille.

Conçue pour les établissements d'Afrique de l'Ouest, l'application donne aux parents, aux
élèves et aux enseignants un accès direct à la vie scolaire, depuis le téléphone.

POUR LES PARENTS

• Absences et retards. Vous êtes prévenu dès que l'absence est saisie par l'école, avec le
  motif et la date. Plus besoin d'attendre le bulletin pour l'apprendre.
• Notes et bulletins. Les notes de devoirs et de composition s'affichent matière par matière,
  avec la moyenne et le rang dans la classe.
• Scolarité. L'échéancier, ce qui a été réglé, ce qui reste dû, et le reçu de chaque paiement
  consultable à tout moment.
• Emploi du temps. La semaine de votre enfant, salle par salle.
• Plusieurs enfants. Un seul compte suffit : vous passez de l'un à l'autre d'un geste.

POUR LES ENSEIGNANTS

• Feuille d'appel. L'appel se fait en classe, en quelques secondes, et les familles concernées
  sont prévenues aussitôt.
• Saisie des notes. Les évaluations se remplissent directement depuis le téléphone.
• Emploi du temps. Vos cours de la journée, sans passer par le secrétariat.

POUR LES ÉLÈVES

• Leur propre compte, avec leurs notes, leurs absences et leur emploi du temps.

NOTIFICATIONS

Vous choisissez ce que vous recevez, ligne par ligne, depuis l'application. Couper une
notification qui vous importune vaut mieux que de toutes les désactiver.

CONÇUE POUR LE CONTEXTE MALIEN

• Entièrement en français.
• Montants en francs CFA.
• Programme de l'enseignement fondamental, de la 1ère à la 9ème année.
• Pensée pour les connexions lentes : l'application ne télécharge que ce qu'elle affiche.

UN COMPTE FOURNI PAR VOTRE ÉCOLE

School Manager ne s'utilise pas seul. Votre établissement doit être inscrit sur la plateforme
et vous remettre vos identifiants : le sous-domaine de l'école, votre adresse e-mail et votre
mot de passe. Sans ces trois éléments, la connexion est impossible.

Les écoles intéressées trouveront la plateforme d'administration sur schoolmanager.tech.

CONFIDENTIALITÉ

Les données de chaque établissement sont cloisonnées : une école ne voit jamais les données
d'une autre. À l'intérieur d'une école, chaque rôle n'ouvre que son périmètre — la
comptabilité ne voit pas la vie scolaire, un enseignant ne voit pas la trésorerie.

Notre politique de confidentialité : https://schoolmanager.tech/confidentialite.html
```
*(environ 2 150 caractères — il reste de la marge si vous souhaitez ajouter des témoignages
d'établissements une fois les premiers clients signés.)*

### 2.3 App Store — sous-titre (30 caractères)

```
L'école dans votre poche
```
*(23 caractères)*

### 2.4 App Store — texte promotionnel (170 caractères, modifiable sans nouvelle version)

```
Absences signalées en temps réel, notes, bulletins, scolarité et emploi du temps : la vie scolaire de votre enfant, consultable depuis votre téléphone.
```
*(149 caractères)*

### 2.5 App Store — description

Reprendre le texte du §2.2 tel quel. Apple accepte le même contenu et la même longueur.

### 2.6 App Store — mots-clés (100 caractères, séparés par des virgules, sans espaces)

```
école,scolaire,bulletin,notes,absence,parent,enseignant,élève,Mali,scolarité,emploi du temps
```
*(91 caractères)*

> Ne pas répéter dans les mots-clés ce qui figure déjà dans le nom ou le sous-titre : Apple
> indexe les trois ensemble, et un doublon gâche une place.

### 2.7 Notes de version (première publication)

```
Première version de School Manager.

• Absences et retards signalés en temps réel aux familles
• Notes, moyennes et bulletins
• Scolarité : échéancier, paiements et reçus
• Emploi du temps
• Feuille d'appel et saisie des notes pour les enseignants
• Notifications réglables ligne par ligne
```

---

## 3. Formulaires de conformité

### 3.1 Classification du contenu (Play) / Tranche d'âge (App Store)

Questionnaire Play, section « Référence, actualités ou éducation » :

| Question | Réponse |
|---|---|
| Violence, sexualité, grossièretés, drogues | Non à tout |
| L'application permet-elle aux utilisateurs d'interagir entre eux ? | **Non** — la messagerie n'existe que sur le web ; l'application mobile n'en contient pas (vérifié le 22/10/2026) |
| Partage de la position de l'utilisateur ? | Non |
| Achats dans l'application ? | Non — l'abonnement est facturé à l'établissement, hors application |
| Contenu généré par les utilisateurs ? | Non, pour la même raison |

Résultat obtenu le 22/10/2026 : **PEGI 3 / Tout public** sur Play, **4+** sur l'App Store.

> **Le jour où la messagerie arrive dans l'application mobile**, refaire le questionnaire Play
> (interaction entre utilisateurs : Oui) et ajouter « Messages » à la sécurité des données.
> Répondre « Oui » à l'interaction entre utilisateurs est obligatoire dès qu'une messagerie
> existe, même interne et cloisonnée. Une réponse « Non » découverte plus tard entraîne un
> retrait.

### 3.2 Sécurité des données (Play) / Étiquettes de confidentialité (App Store)

Données **collectées** et **liées à l'identité** de l'utilisateur :

| Catégorie | Donnée | Finalité | Obligatoire ? |
|---|---|---|---|
| Informations personnelles | Nom, prénom | Fonctionnement de l'application | Oui |
| Informations personnelles | Adresse e-mail | Identification du compte | Oui |
| Informations personnelles | Numéro de téléphone | Alertes d'absence par SMS | Oui |
| Identifiants | Identifiant de compte | Fonctionnement de l'application | Oui |
| Informations financières | Historique des paiements de scolarité | Fonctionnement de l'application | Oui |
| Photos | Portrait de l'élève | Carte d'identité scolaire | Non |

Données **non** collectées : position, contacts, calendrier, historique de navigation,
historique de recherche, fichiers et documents de l'appareil, santé, données publicitaires.

Réponses aux questions transversales :

| Question | Réponse |
|---|---|
| Les données sont-elles chiffrées en transit ? | **Oui** — HTTPS exclusivement |
| L'utilisateur peut-il demander la suppression de ses données ? | **Oui** — par l'établissement, procédure décrite dans la politique de confidentialité |
| Les données sont-elles partagées avec des tiers ? | **Non** |
| Les données servent-elles à la publicité ou au suivi ? | **Non** |

> Le jeton de notification push est traité par Firebase Cloud Messaging. Ni Google ni Apple ne
> le comptent comme une donnée collectée au sens du formulaire lorsqu'il ne sert qu'à l'envoi
> des notifications, ce qui est notre cas. Il n'y a donc rien à déclarer de ce côté, mais la
> question revient souvent.

### 3.3 Déclaration de chiffrement (App Store)

`ITSAppUsesNonExemptEncryption` est déjà positionné à `false` dans `Info.plist` : l'application
n'utilise que HTTPS, ce qui relève de l'exemption. Aucune démarche supplémentaire.

### 3.4 Coordonnées et liens

| Champ | Valeur |
|---|---|
| Site web | https://schoolmanager.tech |
| Politique de confidentialité | https://schoolmanager.tech/confidentialite.html |
| E-mail d'assistance | `oumarcoulou44@gmail.com` — publiquement visible sur la fiche. Une adresse dédiée (`support@schoolmanager.tech`) sera à préférer le jour où le volume d'écoles le justifie |
| Téléphone de contact | `+223 79 82 79 79` — au format international. Apple refuse le préfixe `00` |

> **Où se trouve l'URL de confidentialité chez Apple.** Sous **Confidentialité de l'app**,
> dans la colonne de gauche (« Confiance et sécurité »), et non sous « Informations sur
> l'app » — c'est la même page que le questionnaire des données collectées.

### 3.5 Compte de démonstration pour les examinateurs

Les deux stores refusent une application dont l'écran d'accueil est un formulaire de connexion
sans compte fourni. Les identifiants sont dans `identifiants-revue-stores.txt` (non versionné).

Texte à coller dans le champ « Notes pour l'examinateur » :

```
L'écran de connexion comporte TROIS champs. Sans le premier, la connexion échoue.

  Établissement : demo
  E-mail        : revue@demo.schoolmanager.tech
  Mot de passe  : (celui saisi dans le champ prévu ci-dessus)

Ce compte est administrateur. Pour voir l'application du côté famille, qui est
l'usage principal, se connecter avec parent@demo.schoolmanager.tech et le même
mot de passe.

L'établissement de démonstration contient une année scolaire complète : 9 classes,
108 élèves, notes, absences, emploi du temps et paiements de scolarité.
```

---

## 4. Captures d'écran

### 4.1 Ce qu'exigent les stores

| Store | Format | Nombre | Dimensions |
|---|---|---|---|
| Google Play | PNG ou JPEG | 2 minimum, 8 maximum | côté le plus court ≥ 320 px, le plus long ≤ 3 840 px, et **jamais plus du double du plus court** |
| App Store — iPhone 6,9" | PNG ou JPEG | 3 minimum, 10 maximum | 1320 × 2868 ou 2868 × 1320 |
| App Store — iPhone 6,5" | PNG ou JPEG | facultatif si le 6,9" est fourni | 1242 × 2688 |

> Le piège de Play : une capture d'iPhone moderne fait 1290 × 2796, soit un rapport de 2,17.
> Play la refuse, parce qu'elle dépasse le double. Il faut la recadrer ou la poser sur un fond.
> Une capture d'Android est généralement conforme telle quelle.

### 4.2 Les six écrans à photographier, dans cet ordre

L'ordre compte : les deux premières captures sont les seules que voit un visiteur qui ne fait
pas défiler.

| # | Écran | Compte | Pourquoi celui-ci |
|---|---|---|---|
| 1 | Portail famille, fiche d'un élève | `parent@` | C'est l'application en une image : notes, absences et scolarité au même endroit |
| 2 | Notification d'absence reçue | `parent@` | L'argument qui décide un parent. Capturer l'écran verrouillé au moment où elle arrive |
| 3 | Notes d'une matière | `parent@` | Montre la profondeur du suivi |
| 4 | Scolarité : échéancier et reçus | `parent@` | Le sujet le plus sensible pour une famille |
| 5 | Feuille d'appel | `enseignant@` | Montre que l'école travaille dans l'application, pas seulement qu'elle la consulte |
| 6 | Emploi du temps | `parent@` ou `enseignant@` | Écran lisible, qui fait respirer la série |

Prendre les captures sur l'iPhone, sur la build TestFlight déjà installée : ce sont de vraies
captures d'appareil, aux bonnes dimensions pour Apple, et recadrables pour Play. Pour la n° 2,
l'écran verrouillé s'obtient en verrouillant le téléphone puis en faisant saisir une absence
sur une date encore libre (voir `NOTIFICATIONS-PUSH.md` §7.1).

### 4.3 Icône et bandeau

| Élément | Store | Dimensions | État |
|---|---|---|---|
| Icône | Play | 512 × 512 PNG 32 bits | 🔑 à extraire des ressources du projet |
| Bandeau de présentation | Play | 1024 × 500 | 🔑 obligatoire, à produire |
| Icône | App Store | 1024 × 1024 PNG sans transparence | 🔑 à extraire |

---

## 5. Reconstruire le bundle Android

```bash
cd mobile
export PATH="$HOME/.nvm/versions/node/v22.23.1/bin:$PATH"
export JAVA_HOME="/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home"
npx ng build && npx cap sync android && (cd android && ./gradlew bundleRelease)
```

Les deux `export` ne sont pas décoratifs :

- La version de Node par défaut de la machine (v22.19) est **inférieure au minimum exigé par
  Angular** (v22.22.3). `ng build` s'arrête alors sans rien produire — et `cap sync`, lancé
  à la suite, recopie sans broncher les *anciens* fichiers web. Le bundle est construit, signé,
  valide, et contient une version périmée de l'application. Rien ne le signale.
- La version de Java par défaut (25) n'est pas supportée par Gradle 8.14, qui échoue sur
  `Unsupported class file major version 69`. Celle-là, au moins, est bruyante.

Vérifier après coup que le `.aab` porte bien la date du jour, et qu'il est signé :

```bash
ls -l mobile/android/app/build/outputs/bundle/release/app-release.aab
"$JAVA_HOME/bin/keytool" -printcert -jarfile \
  mobile/android/app/build/outputs/bundle/release/app-release.aab | grep Propriétaire
```

Le certificat attendu est `CN=oumar, OU=logilab, O=logilab, L=Mali, ST=Bamako, C=ML`.

### Identifiant Android : `tech.schoolmanager.app`

Le nom de paquet `com.schoolsaas.mobile` étant déjà pris sur Google Play, l'application
Android s'appelle `tech.schoolmanager.app`. iOS garde `com.schoolsaas.mobile`.

Ce qui a changé, et ce qui n'a volontairement pas changé :

| Élément | Valeur | Pourquoi |
|---|---|---|
| `applicationId` (`mobile/android/app/build.gradle`) | `tech.schoolmanager.app` | identité vue par Play et Firebase |
| `namespace` et paquet Java de `MainActivity` | `com.schoolsaas.mobile` | interne à la compilation (classe `R`) ; Android autorise l'écart, et on évite de déplacer le code |
| `appId` (`mobile/capacitor.config.ts`) | `com.schoolsaas.mobile` | commun à iOS. Capacitor ne le recopie dans les projets natifs qu'au `cap add` : `cap sync` ne touche ni l'`applicationId` ni le bundle iOS |
| Projet Xcode, `Info.plist`, `GoogleService-Info.plist` | inchangés | application iOS déjà publiée |
| Autorités des `provider` (FileProvider, Firebase) | `tech.schoolmanager.app.*` | dérivées automatiquement de `${applicationId}` |
| Canal de notification `vie-scolaire` | inchangé | ne dépend pas du paquet |

**Conséquence : le `google-services.json` actuel ne convient plus.** Il ne déclare que
`com.schoolsaas.mobile`, et le build échoue sur `No matching client found for package name
'tech.schoolmanager.app'`. À faire dans la console Firebase 🔑 :

1. [console.firebase.google.com](https://console.firebase.google.com) → projet
   **schoolmanager-60a0f** → ⚙ **Paramètres du projet** → **Vos applications** →
   **Ajouter une application** → **Android**.
2. **Nom du package** : `tech.schoolmanager.app`, à l'identique. Surnom : « School Manager
   Android ».
3. Empreinte SHA-1 : **facultative**. Les notifications push (FCM) ne l'utilisent pas ; elle
   ne servirait qu'à une connexion Google ou à App Check. Pour la renseigner quand même
   (empreintes de la clé de téléversement) :

   ```bash
   cd mobile/android
   export JAVA_HOME="/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home"
   STORE=$(grep '^storeFile=' keystore.properties | cut -d= -f2-)
   ALIAS=$(grep '^keyAlias=' keystore.properties | cut -d= -f2-)
   "$JAVA_HOME/bin/keytool" -list -v -keystore "$STORE" -alias "$ALIAS" | grep -E 'SHA1|SHA256'
   ```

   `keytool` demande le mot de passe du magasin sans l'afficher ; il n'est jamais lu depuis
   `keystore.properties`. Avec Play App Signing, l'application distribuée est signée par la
   clé de Google : ses empreintes sont dans Play Console → **Test et publication** →
   **Intégrité de l'application**, à ajouter aussi le jour où un service les exige.
4. **Télécharger `google-services.json`** et **remplacer**
   `mobile/android/app/google-services.json`. Le nouveau fichier contient les deux clients
   (`com.schoolsaas.mobile` et `tech.schoolmanager.app`) : c'est attendu, et sans effet sur iOS.
   Le committer (identifiants publics, pas de secret).
5. Ignorer les étapes Gradle proposées par Firebase : le plugin est déjà branché.
6. Reconstruire le bundle avec la commande ci-dessus.

Côté serveur, rien à changer : le backend envoie aux jetons d'appareil avec le compte de
service du projet, sans restriction de paquet. Les éventuelles installations de test sous
`com.schoolsaas.mobile` continuent de recevoir tant qu'elles restent installées (leur client
existe toujours dans le projet) ; une fois désinstallées, FCM répond `UNREGISTERED` et le
backend oublie leur jeton de lui-même.

### Numéro de version

`versionCode` vaut **2** et `versionName` **1.0**, dans `mobile/android/app/build.gradle`.
Le `versionCode` 1 est celui du bundle du 22/10/2026 ; le 2 est réservé à la livraison
suivante (retrait fiable du jeton push à la déconnexion, voir `docs/NOTIFICATIONS-PUSH.md` §8).
Play refuse un `versionCode` déjà téléversé : l'incrémenter avant chaque nouveau bundle.

### Versions iOS

- **1.0 (build 35)** : publiée le 22/10/2026.
- **1.0.1 (build 40)** : correctif du jeton push à la déconnexion, envoyée en examen le
  23/10/2026. Disponibilité étendue le même jour à 174 pays et régions (tous sauf la Chine
  continentale, qui exige une licence ICP).

Les builds iOS sont produits par GitHub Actions (`.github/workflows/ios.yml`, lancement manuel
avec « publier ») : le numéro de build y est **le numéro d'exécution du workflow**
(`CURRENT_PROJECT_VERSION=${{ github.run_number }}`), la valeur du projet n'est pas utilisée.
**Une version publiée est fermée** : App Store Connect refuse tout nouveau build portant la même
`MARKETING_VERSION`. Avant chaque livraison, incrémenter `MARKETING_VERSION` dans
`mobile/ios/App/App.xcodeproj/project.pbxproj` puis créer la version correspondante dans
App Store Connect avant d'y associer le build.

---

## 6. Ordre de dépôt sur Google Play 🔑

1. Play Console → **Créer une application** — nom, langue par défaut, « Application »,
   « Gratuite ».
2. **Configuration du tableau de bord**, dans l'ordre que la console impose :
   accès à l'application (y coller les identifiants du §3.5), publicités (**non**),
   classification du contenu (§3.1), public cible (**13-15, 16-17 et 18 ans et plus**,
   choix d'Oumar du 22/10/2026 : les collégiens ont leur propre compte, mais exclure les moins
   de 13 ans évite le programme « Familles » et son examen renforcé ; les plus jeunes passent
   par le compte de leurs parents), sécurité des données
   (§3.2), politique de confidentialité (§3.4).
3. **Fiche principale du store** — textes du §2, captures du §4.
4. **Production → Créer une version** — téléverser le `.aab`, coller les notes de version.
5. Soumettre.

Compter deux à sept jours d'examen pour une première publication. Apple est généralement plus
rapide, mais plus exigeant sur le compte de démonstration : c'est là que la plupart des
premiers refus se jouent, et c'est pourquoi le §3.5 insiste sur les trois champs.
