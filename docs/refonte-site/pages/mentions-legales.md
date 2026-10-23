# Mentions légales (`/mentions-legales`, `landing/mentions-legales.html`)

Socle : [../SPEC.md](../SPEC.md). Gabarit juridique identique à la politique de confidentialité
(`.legal`, fond papier, en-tête et pied de page communs).

**Objectif** : identifier l'éditeur, le responsable de la publication et l'hébergeur ;
créditer les photos et les marques. Uniquement des informations fournies par Oumar ou
vérifiées ; **rien d'inventé**.

## Balises

| Balise | Valeur |
|---|---|
| `title` | Mentions légales \| School Manager |
| `meta description` | Éditeur, directeur de la publication, hébergement et crédits du site schoolmanager.tech. |
| `canonical` / `og:url` | `https://schoolmanager.tech/mentions-legales` |
| `og:title` | Mentions légales de School Manager |
| `og:description` | Éditeur, hébergement et crédits du site schoolmanager.tech. |
| WhatsApp de la page | W8 |
| `aria-current` | dans le pied de page, sur « Mentions légales » |

## Contenu (texte définitif)

**`h1`** : Mentions légales
**Date `.updated`** : « En vigueur au [date de mise en ligne]. » La date réelle de mise en
production, au format « 2 novembre 2026 », est écrite par agent-frontend au moment du
déploiement. Jamais de crochets en production.

### `h2 id="editeur"` 1. Éditeur

> Le site schoolmanager.tech et la plateforme School Manager sont édités par **LOGILAB**,
> Bamako, Mali.

> **Note interne (à compléter par Oumar, ne figure pas dans la page publique)** : ajouter
> sous ce paragraphe, une ligne `<p>` par valeur, uniquement une fois la valeur connue (ne
> jamais afficher une ligne vide) : « Forme juridique : … », « RCCM : … », « NIF : … ».
> Aucun commentaire HTML de ce type ne doit être laissé dans `landing/mentions-legales.html`
> (il serait lisible dans le code source de la page).

Puis, affiché :

> Contact : oumarcoulou44@gmail.com (lien `mailto:`) · WhatsApp +223 79 82 79 79 (lien W8,
> texte masqué « (WhatsApp, nouvel onglet) »).

### `h2 id="publication"` 2. Directeur de la publication

> Oumar Mory Coulibaly.

### `h2 id="hebergement"` 3. Hébergement

> Le site et la plateforme sont hébergés sur un serveur privé virtuel loué à :
>
> **Hostinger International Ltd**
> 61 Lordou Vironos Street, 6023 Larnaca, Chypre
> www.hostinger.com (lien `https://www.hostinger.com`, nouvel onglet)
>
> Le serveur est situé dans l'Union européenne.
>
> Le nom de domaine et l'acheminement sécurisé des échanges sont assurés par :
>
> **Cloudflare, Inc.**
> 101 Townsend Street, San Francisco, CA 94107, États-Unis
> www.cloudflare.com (lien `https://www.cloudflare.com`, nouvel onglet)

(Adresses vérifiées le 23/10/2026, sources en fin de `SPEC.md`. « Union européenne » repris de la
politique de confidentialité, SPEC §11, point 3.)

### `h2 id="donnees"` 4. Données personnelles et cookies

> Le traitement des données des écoles, des élèves et des familles est décrit dans la
> politique de confidentialité (lien `/confidentialite.html`).
>
> Ce site de présentation ne contient aucun formulaire, ne dépose aucun cookie de mesure
> d'audience ni de publicité, et ne charge aucune ressource depuis un autre site. Les liens
> vers WhatsApp et l'App Store ne transmettent rien tant que vous ne cliquez pas.

### `h2 id="propriete"` 5. Propriété intellectuelle

> Les textes, captures d'écran et éléments graphiques de ce site sont la propriété de leur
> éditeur, sauf mention contraire. Leur reproduction sans autorisation est interdite.
>
> Apple, le logo Apple et App Store sont des marques d'Apple Inc. Google Play et le logo
> Google Play sont des marques de Google LLC.
>
> Les polices Inter et Outfit sont distribuées sous licence SIL Open Font License 1.1.

### `h2 id="credits"` 6. Crédits photos

> Les photos d'illustration montrent des élèves d'Afrique de l'Ouest. Elles ne représentent
> pas des écoles utilisatrices de School Manager. Toutes proviennent de Wikimedia Commons et
> sont placées dans le domaine public (licence CC0 1.0).

Liste `<ul>` :

| Libellé affiché | Lien (nouvel onglet) |
|---|---|
| « Students reading in a classroom », Bright Kwame Ayisi, Amanya (région d'Oti, Ghana) | `https://commons.wikimedia.org/wiki/File:Students_reading_in_a_classroom.jpg` |
| « Pupils studying a book », Bright Kwame Ayisi (Ghana) | `https://commons.wikimedia.org/wiki/File:Pupils_studying_a_book.jpg` |
| « Students writing notes », Onwuka Glory (Nigeria) | `https://commons.wikimedia.org/wiki/File:Students_writing_notes.jpg` |

> Les captures d'écran de l'application utilisent des données de démonstration : les noms
> d'élèves qui y figurent sont fictifs.

### `h2 id="contact"` 7. Contact

> Pour toute question sur ce site : oumarcoulou44@gmail.com (lien `mailto:`) ou WhatsApp
> +223 79 82 79 79 (lien W8).

## Responsive

Identique à la politique de confidentialité (`confidentialite.md`) : colonne `46rem` centrée,
`padding-inline: 16px` sous 600 px. Pas de tableau sur cette page (la liste des crédits est une
`<ul>`).
