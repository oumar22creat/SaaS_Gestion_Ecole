# Page Parents (`/parents`, `landing/parents.html`)

Socle : [../SPEC.md](../SPEC.md).

**Objectif** : un parent arrivé depuis un post Facebook ou TikTok comprend en une lecture que
l'application est **gratuite pour lui**, qu'elle **ne marche qu'avec les identifiants remis par
l'école**, et où la télécharger. S'il n'a pas d'école inscrite, il transmet la page à son
directeur. C'est la page cible des publications « parents » du plan réseaux sociaux.

Ton : vouvoiement, mots des parents (« votre enfant », « le reçu », « l'école »), phrases courtes.

## Balises

| Balise | Valeur |
|---|---|
| `title` | Application parents : absences, notes, scolarité \| School Manager |
| `meta description` | Prévenu dès qu'une absence est saisie, notes et moyenne, ce qui est payé et ce qui reste dû. Gratuite pour les parents, avec le compte remis par l'école. |
| `canonical` / `og:url` | `https://schoolmanager.tech/parents` |
| `og:title` | La vie scolaire de votre enfant, sur votre téléphone |
| `og:description` | Application gratuite pour les parents. Votre école vous remet vos identifiants. |
| WhatsApp de la page | W6 (en-tête mobile, pied de page) ; W9 pour le partage vers le directeur |
| `aria-current="page"` | lien « Parents » |

## Gabarit

0. En-tête · 1. Introduction · 2. Avant de télécharger · 3. Ce que l'application vous montre ·
4. Les données de votre enfant · 5. Questions fréquentes · 6. Appel final « votre école » ·
7. Pied de page. Fonds : 1 papier · 2 papier-2 · 3 papier · 4 papier-2 · 5 papier · 6 nuit.

---

## 1. Introduction (`.page-intro`, avec visuel)

| Élément | Texte |
|---|---|
| Surtitre | Pour les parents d'élèves |
| `h1` | La vie scolaire de votre enfant, sur votre téléphone. |
| Chapô | Absences, notes, scolarité et emploi du temps : vous voyez ce que l'école enregistre, sans vous déplacer. L'application est gratuite pour les parents. |
| Badges | Rangée stores (SPEC §6.15) : App Store + Google Play bientôt + note « Sur iPhone dès maintenant. Sur Android, l'application est en test : bientôt sur Google Play. » |
| Encadré `.notice` | **Votre école doit utiliser School Manager.** C'est elle qui vous remet vos identifiants. Sans eux, la connexion est impossible. |

Visuel : photo `/assets/photos/eleves-lecture-*`, ratio 4/5 (≥ 900 px) ou 16/10 (dessous),
**`loading` normal** (au-dessus de la ligne de flottaison sur ordinateur), pas de
`fetchpriority`. `alt` : « Deux écolières en uniforme lisent ensemble un livre devant leur
école. » Légende : « Photo d'illustration : Ghana (Wikimedia Commons, CC0). »
Futur (SPEC §9) : deux écrans parents superposés.

| Largeur | Disposition |
|---|---|
| 320 / 375 | Surtitre, `h1` (30 px), chapô, badges l'un sous l'autre à 320 / côte à côte à 375, note, encadré, puis photo 16/10. |
| 768 | Idem, badges côte à côte, photo 16/10 pleine largeur. |
| 1280 | Texte à gauche `1.15fr`, photo à droite `.85fr`. |

## 2. Avant de télécharger (`aria-labelledby="titre-acces"`, fond papier-2)

| Élément | Texte |
|---|---|
| Surtitre | Avant de télécharger |
| `h2` | Trois étapes pour vous connecter. |

Étapes `.steps` :

| N° | `h3` | Texte |
|---|---|---|
| 1 | Votre école utilise School Manager | C'est l'école qui s'abonne. Vous n'avez rien à payer. |
| 2 | L'école vous remet vos identifiants | Trois éléments : le sous-domaine de l'école, votre adresse e-mail et votre mot de passe. |
| 3 | Vous vous connectez dans l'application | Téléchargez l'application, saisissez vos trois identifiants et retrouvez le dossier de votre enfant. |

Capture à côté des étapes : `/assets/captures/connexion-*`, `.shot` 240 px, ratio 9/16
complet, `loading="lazy"`. `alt` : « Capture de l'application : écran de connexion avec les
champs Sous-domaine, E-mail et Mot de passe, et le bouton Se connecter. » Légende : « L'écran
de connexion de l'application. »

Encadré `.notice` sous les étapes : « **Vous n'avez pas reçu vos identifiants ?** Demandez-les
au secrétariat de votre école : c'est elle qui crée le compte de chaque famille. LOGILAB ne peut
pas créer de compte parent à la place de l'école. »

| Largeur | Disposition |
|---|---|
| 320 / 375 / 768 | Étapes empilées (768 : empilées aussi, car à côté de la capture il ne reste pas la place de 3 colonnes), capture centrée dessous, encadré. |
| 1280 | Étapes empilées à gauche (`1.3fr`), capture à droite (`.7fr`), encadré pleine largeur dessous. |

Les étapes de cette page utilisent la variante `.steps-vertical` (pastille numérotée à gauche
du texte, une colonne) à toutes les largeurs.

## 3. Ce que l'application vous montre (`aria-labelledby="titre-montre"`)

| Élément | Texte |
|---|---|
| Surtitre | Ce que vous voyez |
| `h2` | Ce que l'application vous montre. |

Six cartes `.feature` :

| Icône | `h3` | Texte |
|---|---|---|
| `i-bell` | Absences et retards | Vous êtes prévenu dès que l'école saisit une absence, avec la date et le motif. Plus besoin d'attendre le bulletin pour l'apprendre. |
| `i-book` | Notes et moyenne | Les notes de devoirs et de compositions, matière par matière, avec la moyenne et le rang dans la classe. |
| `i-receipt` | Scolarité | L'échéancier, ce qui a été réglé, ce qui reste dû, et le reçu de chaque paiement. |
| `i-calendar` | Emploi du temps | La semaine de votre enfant, cours par cours. |
| `i-child` | Plusieurs enfants | Un seul compte suffit : vous passez de l'un à l'autre d'un geste. |
| `i-clock` | Notifications réglables | Vous choisissez ce que vous recevez, ligne par ligne. |

Paragraphe sous les cartes (`--ink-2`) : « Les élèves ont aussi leur compte, avec leurs notes,
leurs absences et leur emploi du temps. L'école crée leur accès. »

Responsive : 320 / 375 : 1 colonne. 768 : 2 colonnes. 1280 : 3 colonnes.
Futur (SPEC §9) : rail de 3 écrans parents sous les cartes.

## 4. Les données de votre enfant (fond papier-2, conteneur `--narrow`)

| Élément | Texte |
|---|---|
| Surtitre | Confidentialité |
| `h2` | Les données de votre enfant restent à l'école. |
| Paragraphe 1 | L'école de votre enfant dispose de son propre espace. Aucune autre école ne peut voir ses données. À l'intérieur de l'école, chacun voit uniquement ce qui le concerne : un enseignant voit ses classes, la comptabilité les règlements. Vous, vous voyez le dossier de vos enfants. |
| Paragraphe 2 | Aucune donnée n'est vendue, louée, ni utilisée à des fins publicitaires. |
| Liens `.more` | Comment les données sont protégées → `/securite` · Lire la politique de confidentialité → `/confidentialite.html` |

## 5. Questions fréquentes (`id="questions"`, conteneur `--narrow`)

| Élément | Texte |
|---|---|
| Surtitre | Questions fréquentes |
| `h2` | Les questions des parents. |

| Question | Réponse |
|---|---|
| L'application est-elle payante pour les parents ? | Non. C'est l'école qui s'abonne. L'application est gratuite à télécharger et à utiliser pour les parents. |
| Où trouver mes identifiants ? | Auprès de votre école. C'est elle qui crée le compte de chaque famille et vous remet le sous-domaine de l'école, votre adresse e-mail et votre mot de passe. |
| J'ai oublié mon mot de passe. | Adressez-vous au secrétariat de votre école : il peut vous en attribuer un nouveau. |
| L'application est-elle disponible sur Android ? | Pas encore pour tout le monde. Elle est en test sur Google Play et sera annoncée ici dès sa sortie publique. Elle est déjà disponible sur iPhone, dans l'App Store. |
| J'ai plusieurs enfants. Faut-il plusieurs comptes ? | Non, si vos enfants sont dans la même école : un seul compte suffit. S'ils sont dans des écoles différentes, chaque école vous remet ses propres identifiants. |
| Puis-je écrire à l'école ou payer les frais depuis l'application ? | Non. L'application sert à suivre la scolarité de votre enfant. Pour écrire à l'école ou régler les frais, adressez-vous directement à l'établissement. |
| Faut-il une bonne connexion internet ? | Non. L'application ne télécharge que ce qu'elle affiche : quand vous ouvrez les notes, elle charge les notes, rien de plus. |
| Mon école n'utilise pas School Manager. Que faire ? | Parlez-en à votre directeur. Vous pouvez lui envoyer la présentation de School Manager sur WhatsApp, en un clic. (le mot « WhatsApp » n'est pas un lien ; lien séparé à la fin de la réponse : « Envoyer à mon directeur » → W9, texte masqué « (WhatsApp, nouvel onglet) ») |

Sous la FAQ : « Une autre question ? » + lien icône WhatsApp « Écrivez-nous sur WhatsApp » → W6.

## 6. Appel final « votre école » (`.closer`)

| Élément | Texte |
|---|---|
| `h2` | Votre école n'utilise pas encore School Manager ? |
| Paragraphe | Parlez-en à votre directeur. Il peut essayer la plateforme gratuitement pendant 30 jours. |
| `.btn-gold` | icône WhatsApp + « Envoyer School Manager à mon directeur » → W9 |
| `.btn-outline-light` | Je dirige une école : voir l'offre → `/` |

## Notes d'implémentation

- Aucun bouton « Essai gratuit » dans le contenu de cette page (celui de l'en-tête suffit) : le
  parent n'est pas l'acheteur.
- Le lien « Se connecter » de l'en-tête mène à la plateforme web (personnel de l'école). Pas de
  changement de libellé, mais la FAQ et les étapes disent clairement que les parents se
  connectent dans l'application.
- W9 ouvre WhatsApp sans destinataire : le parent choisit son contact.
