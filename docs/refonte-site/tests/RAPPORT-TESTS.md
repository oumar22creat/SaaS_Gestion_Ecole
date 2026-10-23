# Rapport de tests — site vitrine (étape 12, PROPOSITION.md §7)

- Date : 23/10/2026, branche `feature/refonte-site` (après le commit 1260ce3)
- Testeur : agent-tests
- Périmètre : `landing/` (7 pages + 404) servi par **la vraie image Nginx** (`deploy/Dockerfile.web`)

## Banc de test

- Image : `docker build -f deploy/Dockerfile.web -t schoolmanager-web:test .` (construction OK)
- Conteneur : `DOMAIN=example.test`, certificat auto-signé (`example.test` + `*.example.test`)
  monté sur `/etc/letsencrypt`, backend fictif `--add-host backend:127.0.0.1`,
  ports `127.0.0.1:8443→443` et `127.0.0.1:8080→80`. Seul avertissement au démarrage :
  `ssl_stapling ignored` (normal avec un certificat auto-signé).
- HTTP : `curl --resolve example.test:8443:127.0.0.1`.
- Navigateur : Chrome 155.0.8059.85 headless piloté par puppeteer-core, avec
  `--host-resolver-rules="MAP example.test:443 127.0.0.1:8443"` (les redirections absolues de
  Nginx restent donc navigables).
- Outils (installés hors dépôt, dans le scratchpad) : axe-core 4.x injecté via CDP,
  Lighthouse (CLI), html-validate 9 (la v11 ne s'installe pas : bug npm `edgesOut`), Pillow
  pour réduire les captures (palette 256 couleurs).
- Conteneur et image supprimés à la fin des tests.

## Synthèse

| # | Domaine | Résultat |
|---|---|---|
| 1 | Routes et redirections | **PASS** (2 écarts mineurs : `/404` et `/index` en 200) |
| 2 | Liens internes, ancres, liens externes | **PASS** (App Store : à valider, voir R1) |
| 3 | Responsive 320 / 360 / 375 / 768 / 1280 | **PASS** (aucun défilement de page) ; défaut connu de 2 px confirmé (B2) |
| 4 | Accessibilité | **PASS** avec 2 défauts mineurs (B1, B5) |
| 5 | Performance (Lighthouse mobile, Slow 4G) | **PASS** — 100 / 100 / 100 / 100 sur les 3 pages |
| 6 | En-têtes (CSP, cache, gzip) | **PASS** — aucune violation CSP |
| 7 | SEO | **PASS** |
| 8 | Contenu (garde-fou) | **PASS** |

Aucun bug bloquant ni majeur. 6 bugs mineurs, 3 remarques à valider.

---

## 1. Routes — PASS

Mesuré avec `curl -sk --resolve … -w '%{http_code} %{redirect_url}'` :

| Requête | Attendu | Obtenu |
|---|---|---|
| `/`, `/fonctionnalites`, `/parents`, `/tarifs`, `/securite`, `/mentions-legales` | 200 | 200 `text/html` (x6) |
| `/confidentialite.html` | 200 | 200 |
| `/index.html` | 301 → `/` | 301 → `https://example.test/` |
| `/fonctionnalites.html`, `/parents.html`, `/tarifs.html`, `/securite.html`, `/mentions-legales.html` | 301 → URL propre | 301 → `/fonctionnalites`, `/parents`, `/tarifs`, `/securite`, `/mentions-legales` |
| `/tarifs.html?a=1` | chaîne de requête conservée | 301 → `/tarifs?a=1` |
| `/tarifs/` | 301 → `/tarifs` | 301 → `/tarifs` |
| `/inexistant`, `/assets/inexistant.png`, `/assets`, `/.git/config` | 404 page perso | 404, corps **identique octet pour octet** à `landing/404.html` (md5 `9d5ae8ed…`), titre « Page introuvable \| School Manager » |
| `/404.html` | 404 | 404 (`internal`) |
| `www.example.test/` et `/tarifs?x=1` | 301 → domaine nu | 301 → `https://example.test/` et `https://example.test/tarifs?x=1` |
| `http://example.test/tarifs` | 301 → HTTPS | 301 → `https://example.test/tarifs` |
| `/register` | redirection vers l'app | 302 → `https://app.example.test/register` |
| `/login` | redirection vers l'app | 302 → `https://app.example.test/login` |

Écarts (mineurs, voir B4) :

| Requête | Obtenu | Commentaire |
|---|---|---|
| `/404` | **200**, contenu de la page 404 | « soft 404 » ; la page porte `noindex`, impact limité |
| `/index` | **200**, page d'accueil | contenu dupliqué ; le `canonical` vers `/` limite l'impact |
| `/confidentialite` | 200, page de confidentialité | dupliqué de `/confidentialite.html` ; `canonical` correct vers `.html` — acceptable |
| `/assets/CREDITS.md` | **200 `application/octet-stream`** | point connu confirmé (B3) |

## 2. Liens — PASS

Script d'extraction de tous les `href`, `src` et `srcset` des 8 fichiers, chaque chemin interne
demandé au conteneur, chaque ancre vérifiée contre les `id` de la page cible :

- **598 références** examinées, **49 chemins internes distincts** : tous en 200 (ou 302 pour
  `/login`, `/register`). **0 lien cassé, 0 ancre cassée, 0 lien relatif.**
- WhatsApp : 9 liens `wa.me` distincts, tous décodables, texte prérempli adapté à la page, ex. :
  - accueil : « Bonjour, je souhaite une démonstration de School Manager pour mon école. (Site : accueil) »
  - tarifs : « Bonjour, je souhaite un devis pour le plan Premium de School Manager. (Site : tarifs) »
  - partage parent (`wa.me/?text=`, sans numéro) : « Bonjour, je suis parent d'élève. […] Notre école pourrait l'utiliser : https://schoolmanager.tech/ »
  - HEAD `https://wa.me/22379827979` → 302 vers `api.whatsapp.com/send/?phone=22379827979…` : OK.
- `mailto:oumarcoulou44@gmail.com` : bien formé, présent sur les 8 pages.
- App Store `https://apps.apple.com/app/id6822008457` : bien formé, présent sur les 8 pages.
  HEAD → **404**. Une requête à l'API publique iTunes montre pourquoi :
  `lookup?id=6822008457&country=fr` → 0 résultat ; `country=ml` → 1 résultat
  (« School Manager Tech », Oumar Mory COULIBALY). L'application n'est publiée que sur le
  store malien : voir R1.
- Autres externes (1 HEAD chacun) : hostinger.com 200, cloudflare.com 200, 3 fichiers
  Wikimedia Commons 200.
- Badge Google Play : `<img alt="Bientôt disponible sur Google Play">` hors de tout `<a>` sur
  accueil, fonctionnalités et parents : **non cliquable, conforme**.

## 3. Responsive — PASS

Chrome headless, 8 pages × 5 largeurs (40 rendus), images différées forcées par défilement.
Critère : `document.documentElement.scrollWidth > clientWidth`.

- **Aucun défilement horizontal de page** sur les 40 rendus (scrollWidth = largeur partout).
- Aucun élément ne dépasse de la fenêtre hors d'un conteneur à défilement.
- Seuls dépassements : les tableaux de `/confidentialite.html`, tous dans un
  `.table-scroll` (`overflow-x: auto`, `tabindex="0"`, `role="region"`) — défilement voulu pour
  « ce qui est collecté » (440 px) et « sous-traitants » (432 px).
- **Point connu confirmé (B2)** : à 320 px, « pourquoi, et sur quelle base » et « combien de
  temps » ont `scrollWidth 288` pour `clientWidth 286` : une barre de défilement pour 2 px.
  Disparaît dès 360 px.

Captures (pleine page, palette 256 couleurs, 3,7 Mo au total) dans `captures/` :
`{accueil,fonctionnalites,parents,tarifs,securite,mentions-legales,confidentialite,404}-{375,1280}.png`.

Remarque : la première série de captures montrait la galerie « Dans l'application » (accueil,
375 px) en rectangles noirs. Vérification faite, les images se chargent bien
(`complete: true`, `naturalWidth > 0`) : c'était un artefact de la capture pleine page
(images différées hors écran horizontalement). Captures refaites en forçant le décodage.

## 4. Accessibilité — PASS (2 défauts mineurs)

### axe-core (WCAG 2.0/2.1/2.2 A et AA + best-practice), 8 pages à 375 et 1280 px

| Page | Violations | À vérifier (incomplete) |
|---|---|---|
| accueil | 0 | color-contrast (9-11 nœuds) |
| fonctionnalités | **1 : `landmark-unique` (moderate)**, `#roles` | color-contrast (3-5) |
| parents | 0 | color-contrast (3) |
| tarifs | **1 : `landmark-unique` (moderate)**, `#comparer` | color-contrast (3) |
| sécurité | 0 | color-contrast (3) |
| mentions légales | 0 | — |
| confidentialité | 0 | color-contrast (6 à 375) |
| 404 | 0 | — |

Les « incomplete » de contraste sont des textes sur dégradé ou sur image qu'axe ne sait pas
évaluer : contrôle manuel ci-dessous. html-validate confirme la même anomalie
(`unique-landmark`, fonctionnalites.html:298/303, tarifs.html:158/162) — **B1**.

### Contrôles manuels

- `lang="fr"` sur les 8 pages : OK.
- **Un seul `h1`** par page : OK (8/8). Ordre des titres sans saut de niveau (h1 → h2 → h3)
  sur les 8 pages.
- Images : 0 `<img>` sans `alt` ; images décoratives en `alt=""` / `aria-hidden`.
- Repères : `header` 1, `main` 1, `footer` 1, `nav` étiquetées (« Navigation principale »,
  « Menu », « Sur cette page ») sur chaque page.
- Lien d'évitement : premier élément focalisable, visible au focus, cible `#contenu` existante ;
  Entrée → `#contenu`, le Tab suivant tombe sur le premier lien du contenu : OK.
- Contrastes calculés (formule WCAG 2.x, couleurs de `site.v1.css`) :

| Texte / fond | Ratio | |
|---|---|---|
| `--ink` sur `--paper` | 14,72 | AA |
| `--ink-2` sur `--paper` / `--paper-2` / `--surface` / `--green-pale` | 6,62 / 6,11 / 7,28 / 6,14 | AA |
| `--green` sur `--paper` / `--surface` / `--green-pale` | 7,19 / 7,91 / 6,67 | AA |
| blanc sur `--green` (bouton) | 7,91 | AA |
| `--on-night` / `--on-night-2` sur `--night` | 15,00 / 9,82 | AA |
| `--on-night-2` sur `--night-2` | 7,95 | AA |
| `--gold` sur `--night` / `--night-2` | 6,82 / 5,52 | AA |
| `--gold-ink` sur `--gold` (bouton or) | 7,58 | AA |
| `--gold-dark` sur `--paper` / `--surface` | 5,51 / 6,05 | AA |

  `--gold` sur fond clair donnerait 2,20, mais il n'est utilisé en texte que sur fond nuit
  (`.eyebrow-dark`, `.hero h1 .gold`, compteurs `.steps`) : vérifié dans le CSS.
- Focus clavier (375 px, accueil et tarifs) : Tab 1 lien d'évitement, 2 marque, 3 « Essai
  gratuit », 4 bouton Menu ; Entrée ouvre le menu ; Tab parcourt Fonctionnalités, Tarifs,
  Parents, Sécurité, Se connecter, WhatsApp, E-mail. Chaque élément a `:focus-visible` et un
  contour **solide de 3 px** (or sur fond nuit, vert ou nuit sur fond clair). Échap ferme le
  menu. Focus jamais entièrement masqué par l'en-tête collant (`scroll-padding-top`), mesuré sur
  25 Tab × 4 pages après la fin du défilement fluide.
- **B5** : si l'on sort du menu mobile ouvert par Tab, le panneau reste ouvert et recouvre en
  partie l'élément focalisé suivant (sur /tarifs, « Démarrer l'essai gratuit », « Choisir
  Standard » et « Demander un devis » ont leur coin supérieur gauche sous le panneau,
  `elementFromPoint` → `details.nav-mobile`).

## 5. Performance — PASS

Lighthouse CLI, mobile, `throttling-method=simulate` (réglage par défaut = Slow 4G :
RTT 150 ms, 1,6 Mb/s, CPU ×4) :

| Page | Perf. | Access. | Bonnes pratiques | SEO | FCP | LCP | CLS | TBT | Poids | Requêtes |
|---|---|---|---|---|---|---|---|---|---|---|
| `/` | 100 | 100 | 100 | 100 | 0,9 s | 1,3 s | 0 | 20 ms | 150 Kio | 10 |
| `/tarifs` | 100 | 100 | 100 | 100 | 1,1 s | 1,4 s | 0 | 30 ms | 86 Kio | 8 |
| `/parents` | 100 | 100 | 100 | 100 | 1,1 s | 1,6 s | 0 | 10 ms | 129 Kio | 12 |

(Première exécution sur `/` : erreur Lighthouse `NO_NAVSTART`, transitoire ; relancée.)

Insights non bloquants : CSS non minifiée (3 Kio à gagner) ; sur /parents, l'image LCP
(photo `eleves-lecture`) n'a pas `fetchpriority="high"` (B6).

Poids transférés mesurés dans Chrome (cache désactivé, `encodedDataLength`) :

| Page | Premier écran 375 px | Premier écran 1280 px | Page complète 1280 px |
|---|---|---|---|
| accueil | 120 Kio / 10 req. | **151 Kio** / 10 req. | 223 Kio / 16 req. |
| fonctionnalités | 97 Kio / 8 | 107 Kio / 9 | 162 Kio / 15 |
| parents | 109 Kio / 11 | 109 Kio / 11 | 109 Kio / 11 |
| tarifs | 84 Kio / 7 | 84 Kio / 7 | 84 Kio / 7 |
| sécurité | 83 Kio / 7 | 83 Kio / 7 | 83 Kio / 7 |
| mentions légales | 83 Kio / 7 | 83 Kio / 7 | 83 Kio / 7 |
| confidentialité | 85 Kio / 7 | 85 Kio / 7 | 85 Kio / 7 |
| 404 | 81 Kio / 7 | 81 Kio / 7 | 81 Kio / 7 |

Budgets SPEC : page d'accueil complète < 500 Ko : OK (223 Kio). JS < 10 Ko : OK (953 o).
Premier écran < 150 Ko : OK sur mobile (120 Kio), **dépassé de justesse sur l'accueil à
1280 px** (151 Kio ≈ 155 Ko), voir R2. Aucune ressource tierce.

## 6. En-têtes — PASS

Mesuré avec `curl -skI -H 'Accept-Encoding: gzip'` :

| Ressource | Cache-Control | CSP | HSTS | nosniff | gzip |
|---|---|---|---|---|---|
| 7 pages HTML + 404 | `no-cache` | oui | oui | oui | oui |
| `site.v1.css` | `public, max-age=31536000, immutable` | oui | oui | oui | oui |
| `site.v1.js` | `public, max-age=31536000, immutable` | oui | oui | oui | non (953 o < `gzip_min_length 1024`, normal) |
| `fonts/*.woff2` | `public, max-age=31536000, immutable` | oui | oui | oui | — (déjà compressé) |
| images avif/webp/jpg/ico | `public, max-age=604800` | oui | oui | oui | — |
| `app-store-fr.svg` | `public, max-age=604800` | oui | oui | oui | oui |
| `robots.txt`, `sitemap.xml` | `public, max-age=3600` | oui | oui | oui | non (< 1 Kio) |

CSP servie : `default-src 'self'; img-src 'self' data:; style-src 'self'; script-src 'self';
font-src 'self'; connect-src 'self'; base-uri 'self'; form-action 'none'; frame-ancestors 'none'`,
plus `Permissions-Policy`, `Cross-Origin-Opener-Policy: same-origin`, `X-Frame-Options`,
`Referrer-Policy`. Chaque en-tête présent une seule fois.

**Violations CSP : 0** sur les 40 rendus (écouteur `securitypolicyviolation` posé avant le
chargement + console). Aucune requête échouée. Seule erreur console : « Failed to load
resource: 404 » sur la page 404 elle-même, attendue.

## 7. SEO — PASS

- `title` et `meta description` **uniques** sur les 8 pages.
- `canonical` absolu sur les 7 pages, en URL propre (`/tarifs`…), `confidentialite.html`
  conservé ; la 404 a `noindex` et pas de canonical : correct.
- Open Graph complet (`og:type`, `og:title`, `og:description`, `og:url` = canonical,
  `og:image`, dimensions, `og:image:alt`) + `twitter:card=summary_large_image` sur les 7 pages.
  `og:image` → `/assets/og/school-manager-1200x630.jpg` : 200, JPEG 1200 × 630.
- JSON-LD (accueil) : JSON valide, `@graph` = `Organization` (LOGILAB, Bamako, ML, e-mail,
  téléphone WhatsApp) + `SoftwareApplication` (`EducationalApplication`, `operatingSystem:
  "Web, iOS"`, `AggregateOffer` XOF `lowPrice 180000`, `offerCount 3`). **Aucun
  `aggregateRating` ni `review`** : conforme.
- `robots.txt` : `Allow: /` + `Sitemap: https://schoolmanager.tech/sitemap.xml`.
- `sitemap.xml` : 7 URL, toutes en 200 sur le banc, identiques aux canonicals, sans `.html`
  sauf `confidentialite.html` ; la 404 n'y figure pas.
- Lighthouse SEO : 100 sur les 3 pages mesurées.

Remarques (R3) : titre de /fonctionnalites à 74 caractères ; descriptions de fonctionnalités,
tarifs et sécurité à 163-164 caractères (Google tronque souvent vers 155-160).

## 8. Contenu (garde-fou) — PASS

Recherche sur le texte visible des 8 pages :

- « Android et iOS » (et variantes) : **0**. Android n'apparaît qu'en « en test : bientôt sur
  Google Play » (accueil, parents, fonctionnalités) et dans la FAQ « disponible sur Android ? ».
- Essai : jamais promis sur tous les modules ; /tarifs : « L'essai fonctionne avec le plan
  Essentiel » (deux fois, dont la FAQ « L'essai donne-t-il accès à tous les modules ? »).
- Témoignages, avis, notes, étoiles : **0**.
- Chiffre d'utilisateurs, d'écoles ou de clients : **0**.
- Moyen de paiement (Orange Money, Moov, Wave, virement, espèces, carte, Stripe) : **0** ;
  seulement « aucun paiement demandé pour l'essai ».
- Messagerie : décrite comme « messagerie interne sur la plateforme web » / « entre membres
  du personnel », et /fonctionnalites précise « La messagerie et les annonces sont sur la
  plateforme web uniquement. L'application mobile ne permet pas d'écrire à l'école » :
  conforme.
- Badge Google Play non cliquable : conforme (voir §2).

## 9. Validation HTML (html-validate 9, règles `standard`)

Aucune erreur de structure. Relevés : `doctype-style` (minuscules, purement stylistique, à
ignorer), `prefer-native-element` sur les `div role="region"` des tableaux défilants (choix
volontaire, à ignorer), `unique-landmark` (= B1), `long-title` (= R3).

---

## Bugs

| # | Gravité | Agent | Fichier | Description | Reproduction |
|---|---|---|---|---|---|
| B1 | Mineur | agent-frontend | `landing/fonctionnalites.html:297/303`, `landing/tarifs.html:158/162` | Deux repères `region` portent le même nom : la `<section aria-labelledby="titre-roles">` et son `.table-scroll role="region" aria-labelledby="titre-roles"` (idem `titre-comparer` sur /tarifs). Un lecteur d'écran annonce deux régions identiques. Correctif suggéré : `aria-label="Tableau : …"` propre au conteneur, comme sur confidentialite.html. | axe `landmark-unique` ; html-validate `unique-landmark` |
| B2 | Mineur (connu) | agent-frontend | `landing/confidentialite.html` (tableaux « pourquoi, et sur quelle base » et « combien de temps »), `landing/assets/site.v1.css:993-998` | À 320 px, le tableau mesure 288 px pour un conteneur de 286 px : barre de défilement horizontale pour 2 px. | 320 px, `.table-scroll` : `scrollWidth 288 / clientWidth 286` |
| B3 | Mineur (connu) | agent-devops (ou agent-frontend si déplacé hors de `landing/`) | `landing/assets/CREDITS.md`, `deploy/Dockerfile.web` (`COPY landing/`) | Fichier interne servi publiquement en `application/octet-stream` (téléchargement forcé), `Cache-Control: no-cache`. L'exclure de l'image ou lui répondre 404. | `curl -skI https://example.test/assets/CREDITS.md` → 200 |
| B4 | Mineur | agent-devops | `deploy/nginx/conf.d/app.conf.template` (`location /`, `try_files $uri $uri.html`) | `/404` répond **200** avec la page 404 (soft 404) et `/index` répond 200 avec l'accueil (doublon). Suggestion : `location = /404 { return 404; }` et `location = /index { return 301 /; }` (ou les traiter dans la regex des `.html`). | `curl -sk -o /dev/null -w '%{http_code}' https://example.test/404` → 200 |
| B5 | Mineur | agent-frontend | `landing/assets/site.v1.js` | Le menu mobile `<details>` reste ouvert quand le focus clavier en sort par Tab ; le panneau recouvre en partie l'élément focalisé suivant (risque WCAG 2.4.11 si on revient en arrière par Maj+Tab). Suggestion : fermer le menu sur `focusout` quand le nouveau focus est hors du menu. Tout changement impose `site.v2.js` et la mise à jour des 8 pages. | 375 px, /tarifs : Tab ×4, Entrée, Tab ×8 → « Démarrer l'essai gratuit » en partie sous le panneau |
| B6 | Mineur (optimisation) | agent-frontend | `landing/parents.html:118` | L'image LCP de /parents (photo `eleves-lecture`) n'a pas `fetchpriority="high"`. Lighthouse : `lcp-discovery-insight` en échec (score Performance déjà à 100). | Lighthouse mobile sur /parents |

## Remarques et points à valider

| # | Pour | Sujet |
|---|---|---|
| R1 | Oumar | **App Store** : `apps.apple.com/app/id6822008457` répond 404 depuis le banc ; l'API iTunes montre l'app sur le store **malien uniquement** (`country=ml` : 1 résultat, `country=fr` : 0). Un visiteur avec un compte Apple d'un autre pays (France, Côte d'Ivoire…) verra « indisponible ». Soit étendre la disponibilité dans App Store Connect, soit l'assumer (cible Mali). Le lien lui-même est correct. |
| R2 | agent-frontend / Oumar | Premier écran de l'accueil à 1280 px : 151 Kio pour un budget de 150 Ko (120 Kio à 375 px, mobile conforme). Dépassement marginal, sans effet mesuré (Lighthouse 100). |
| R3 | agent-frontend | Titre de /fonctionnalites à 74 caractères ; 3 descriptions à 163-164 caractères (troncature possible dans Google). |
| R4 | agent-relecteur | Les trois cartes « Le constat » de l'accueil (« « Trois semaines pour sortir les bulletins. » »…) sont des phrases entre guillemets sans auteur. Ce ne sont pas des témoignages, mais la typographie peut le suggérer : à confirmer à l'étape 13. |

## Comptage

- Contrôles automatisés : 34 requêtes de routes, 598 références de liens (49 chemins
  distincts), 7 liens externes (1 HEAD chacun) + 2 appels à l'API iTunes, 40 rendus responsive,
  16 analyses axe, 16 captures, 3 audits Lighthouse, 21 contrôles d'en-têtes, 8 pages SEO,
  8 pages garde-fou contenu, 8 pages html-validate, 2 parcours clavier complets + 4 parcours de
  25 Tab.
- **8 domaines testés : 8 PASS, 0 FAIL.**
- **Bugs : 0 bloquant, 0 majeur, 6 mineurs** (B1 à B6) ; 4 remarques (R1 à R4).
