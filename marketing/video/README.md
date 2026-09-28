# Vidéo de présentation — School Manager

Vidéo motion design de présentation du produit, rendue avec [Remotion](https://remotion.dev)
(React → MP4). Tout est du code : aucun fichier de montage binaire, aucune dépendance à un
logiciel d'édition, et une modification de texte ou de prix se fait dans un fichier `.tsx`.

## Rendu

```bash
npm install
npm run render     # → out/school-manager.mp4
```

Node 22 minimum. Le premier rendu télécharge un Chrome Headless (~150 Mo), mis en cache
ensuite.

## Modifier

```bash
npm run dev        # Remotion Studio : aperçu image par image dans le navigateur
```

| Fichier | Contenu |
|---|---|
| `src/Presentation.tsx` | Le montage : ordre des scènes, durées, transitions |
| `src/theme.ts` | Couleurs et ressorts d'animation, repris de `docs/DESIGN.md` |
| `src/scenes/*.tsx` | Une scène par fichier |

## Où sont les chiffres

Les montants affichés dans `src/scenes/Tarifs.tsx` sont ceux de la migration
`V61__align_plan_pricing_with_public_offer.sql` et de la section Tarifs du site vitrine :
180 000 / 750 000 / dès 1 800 000 FCFA par an. **Les trois doivent être changés ensemble** —
un prospect qui voit trois prix différents ne fait confiance à aucun.

Le numéro WhatsApp de `src/scenes/Cloture.tsx` est celui de
`web/src/app/core/support.util.ts`.

## Ce que la vidéo n'a pas

- **Pas de bande son.** Ajoutez une musique libre de droits au montage final si vous en
  voulez une ; le rythme des scènes est tenu sans elle.
- **Pas de version verticale** (9:16 pour WhatsApp et les réseaux). Les scènes en deux
  colonnes (mobile, bulletins) demandent une mise en page distincte, pas un recadrage.
