# Politique de confidentialité (`/confidentialite.html`, restylée)

Socle : [../SPEC.md](../SPEC.md).

**L'URL ne change pas** : `https://schoolmanager.tech/confidentialite.html` est citée dans les
fiches App Store et Google Play. Pas de redirection vers une URL sans extension.

**Objectif** : même texte juridique, nouvelle présentation (fond clair, en-tête et pied de page
communs, sommaire). **Le contenu juridique n'est pas modifié** par cette refonte : mot pour mot
identique à `landing/confidentialite.html` en ligne après le correctif 6c6963b
(branche `fix/parcours-mobile-et-web`), date « En vigueur au 23 octobre 2026 ». Ce correctif
ajoute la permission de notification, la ligne « Jeton de notification » des données collectées
et le sous-traitant « Google (Firebase Cloud Messaging) ».

## Balises

| Balise | Valeur |
|---|---|
| `title` | Politique de confidentialité \| School Manager (inchangé) |
| `meta description` | Quelles données School Manager traite, pourquoi, combien de temps, et comment exercer vos droits. (inchangé) |
| `canonical` / `og:url` | `https://schoolmanager.tech/confidentialite.html` |
| `og:title` | Politique de confidentialité de School Manager |
| `og:description` | Quelles données sont traitées, pourquoi, combien de temps, et comment exercer vos droits. |
| WhatsApp de la page | W8 |
| `aria-current` | aucun lien de navigation principale ; dans le pied de page, `aria-current="page"` sur « Politique de confidentialité » |

## Gabarit

0. En-tête commun (remplace l'en-tête réduit actuel) · 1. Corps juridique `.legal` · 2. Pied de
page commun. Fond papier sur toute la page.

## 1. Corps `.legal` (SPEC §6.22)

Ordre :

1. `h1` « Politique de confidentialité » + date `.maj` « En vigueur au 23 octobre 2026. »
2. Paragraphe d'introduction (inchangé).
3. Encadré « L'école reste maîtresse de ses données… » en `.notice` (filet vert ; plus d'or).
4. **Sommaire ajouté** (`.toc`, `<nav aria-label="Sommaire">`), une pastille par section :

| Libellé de la pastille | Ancre (`id` posé sur le `h2`) |
|---|---|
| 1. Qui est responsable | `#responsable` |
| 2. Ce qui est collecté | `#donnees-collectees` |
| 3. Pourquoi | `#finalites` |
| 4. Mineurs | `#mineurs` |
| 5. Qui peut voir quoi | `#acces` |
| 6. Sous-traitants | `#sous-traitants` |
| 7. Combien de temps | `#durees` |
| 8. Sécurité | `#securite` |
| 9. Vos droits | `#droits` |
| 10. Modifications | `#modifications` |
| 11. Contact | `#contact` |

5. Sections 1 à 11, textes, tableaux et listes **identiques** à l'existant.
6. Le lien final « ← Retour à l'accueil » est supprimé (l'en-tête et le pied de page communs
   le remplacent).

Mise en forme :

- Les 4 tableaux passent dans `.table-scroll` (`tabindex="0"`, `role="region"`,
  `aria-label` = « Tableau : » + titre de la section, par exemple « Tableau : ce qui est
  collecté »). Fini le `white-space: nowrap` actuel.
- En-têtes de tableau fond `--paper-2`, texte `--ink` ; lignes paires fond `--surface`.
- Liens `--green` soulignés (plus d'or sur fond clair).
- Le bloc `<style>` inline actuel est **supprimé** (CSP) : tout passe dans `site.v2.css`
  (classe `.legal`).
- Plus aucun chargement de Google Fonts ni de Phosphor.

| Largeur | Disposition |
|---|---|
| 320 / 375 | Colonne pleine largeur, `padding-inline: 16px`. Tableaux défilants dans leur conteneur (colonnes `min-width: 9rem`). Sommaire en pastilles sur plusieurs lignes. |
| 768 | Colonne `46rem` centrée, tableaux sans défilement pour la plupart. |
| 1280 | Idem, colonne centrée. |
