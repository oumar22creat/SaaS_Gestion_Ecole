# Texte de voix off — vidéo de présentation

Calé sur le minutage réel des scènes (`src/Presentation.tsx`, 30 images/seconde, 51,4 s).

## Principe

**La voix ne lit pas ce qui est écrit à l'écran.** Le texte affiché porte déjà l'argument ;
si la voix le répète, le spectateur lit et écoute la même phrase en décalé, et n'entend plus
ni l'un ni l'autre. La voix dit donc autre chose : elle raconte, l'écran affirme.

Débit : environ 2,4 mots par seconde, posé. Le compte de mots ci-dessous laisse volontairement
du silence — dans un film de cinquante secondes, le silence est ce qui donne du poids à ce
qui est dit.

## Le texte

| Temps | Ce qui est à l'écran | Voix off |
|---|---|---|
| **0,0 – 4,0 s** | School Manager · Plateforme de gestion scolaire | *(1 s de silence, puis)* « Diriger une école, c'est tenir cent choses à la fois. » |
| **3,5 – 10,0 s** | Les notes dans un tableur · Les absences sur papier · Les frais dans un cahier → **Personne ne voit l'ensemble.** | « Chaque information vit dans son coin. Aucune ne parle à l'autre. » *(laisser la phrase à l'écran tomber dans le silence)* |
| **9,3 – 14,0 s** | Toute votre école, au même endroit. | « School Manager rassemble tout, avec les droits de chacun. » |
| **13,4 – 21,4 s** | Les six modules | « Vingt modules, de l'inscription au bulletin. Tout ce que votre école gère déjà, sans le papier. » |
| **20,9 – 27,4 s** | L'appel se fait en trente secondes. | « L'enseignant fait l'appel depuis son téléphone. Trente secondes, et c'est enregistré. » |
| **26,9 – 33,4 s** | Ils arrivent prêts, pas à refaire. | « Les bulletins sortent à votre en-tête, moyennes et rangs calculés. Plus rien à ressaisir. » |
| **32,8 – 38,8 s** | Vos données restent les vôtres · 0 | « Vos données sont isolées. Aucune école ne voit celles d'une autre. Jamais. » |
| **38,3 – 45,3 s** | Les trois plans | « Un plan selon votre effectif, à partir de cent quatre-vingt mille francs par an. » |
| **44,8 – 51,4 s** | Démarrer l'essai gratuit · +223 79 82 79 79 | « Trente jours d'essai gratuit. Écrivez-nous sur WhatsApp, le numéro est à l'écran. » |

Total : environ 105 mots.

## Le texte seul, à lire

> Diriger une école, c'est tenir cent choses à la fois.
>
> Chaque information vit dans son coin. Aucune ne parle à l'autre.
>
> School Manager rassemble tout, avec les droits de chacun.
>
> Vingt modules, de l'inscription au bulletin. Tout ce que votre école gère déjà, sans le
> papier.
>
> L'enseignant fait l'appel depuis son téléphone. Trente secondes, et c'est enregistré.
>
> Les bulletins sortent à votre en-tête, moyennes et rangs calculés. Plus rien à ressaisir.
>
> Vos données sont isolées. Aucune école ne voit celles d'une autre. Jamais.
>
> Un plan selon votre effectif, à partir de cent quatre-vingt mille francs par an.
>
> Trente jours d'essai gratuit. Écrivez-nous sur WhatsApp, le numéro est à l'écran.

## Enregistrement

- **Ton** : celui d'un directeur qui explique à un confrère, pas celui d'une publicité. Calme,
  affirmatif, sans emphase sur les chiffres.
- **Accent** : un français d'Afrique de l'Ouest est un atout sur ce marché, pas un défaut à
  corriger.
- **Matériel** : un téléphone récent dans une pièce meublée suffit. Fuyez les pièces vides et
  carrelées, qui résonnent.
- **Méthode** : enregistrez chaque paragraphe séparément, deux ou trois fois. Le montage
  choisit la meilleure prise et cale les silences.

## Si l'enregistrement ne tombe pas juste

La vidéo est du code : les durées de scènes se changent dans `src/Presentation.tsx`. Si la
voix dépasse, on allonge les scènes concernées et on refait le rendu — ce n'est pas au
comédien de parler plus vite.

## Minutage vérifié

Chaque segment a été lu par une synthèse vocale à 160 mots/minute, puis mesuré contre la
durée de sa scène. Tout passe, avec de la marge :

| Segment | Scène | Lu | Marge |
|---|---|---|---|
| Intro | 4,0 s | 3,3 s | +0,7 s |
| Problème | 6,5 s | 4,2 s | +2,3 s |
| Promesse | 4,7 s | 3,7 s | +1,0 s |
| Modules | 8,0 s | 5,9 s | +2,1 s |
| Mobile | 6,5 s | 5,7 s | +0,8 s |
| Bulletins | 6,5 s | 5,7 s | +0,8 s |
| Sécurité | 6,0 s | 4,8 s | +1,2 s |
| Tarifs | 7,0 s | 4,9 s | +2,1 s |
| Clôture | 6,7 s | 5,5 s | +1,2 s |

La clôture ne laissait aucune marge : la scène est passée de 165 à 200 images. Le numéro de
téléphone reste ainsi à l'écran plus longtemps, ce qui est de toute façon souhaitable — c'est
l'image où le spectateur le note.

## Quelle voix

Par ordre de préférence :

1. **Une vraie personne.** Un français d'Afrique de l'Ouest situe le produit sur son marché ;
   aucune synthèse vocale grand public ne le propose. Un téléphone récent suffit.
2. **ElevenLabs** (modèle multilingue) pour une synthèse : la meilleure en français à ce jour.
   **Le plan gratuit interdit l'usage commercial** — il faut l'offre payante d'entrée de gamme
   pour une vidéo de vente.
3. **Google Cloud Text-to-Speech** (voix françaises « Studio ») ou **Azure Speech**
   (Denise, Henri) : qualité proche, usage commercial inclus, facturation à l'usage — quelques
   centimes pour ce texte.

Dans tous les cas, écrivez les nombres en toutes lettres dans le texte soumis
(« cent quatre-vingt mille », pas « 180 000 ») : les synthèses les lisent souvent mal, et un
prix mal prononcé est pire que pas de prix du tout. Vérifiez la licence avant de publier : la
case « usage commercial » n'est pas cochée par défaut partout.
