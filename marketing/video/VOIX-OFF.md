# Texte de voix off — vidéo de présentation

Calé sur le minutage réel des scènes (`src/Presentation.tsx`, 30 images/seconde, 50,3 s).

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
| **44,8 – 50,3 s** | Démarrer l'essai gratuit · +223 79 82 79 79 | « Trente jours d'essai gratuit. Écrivez-nous sur WhatsApp, le numéro est à l'écran. » |

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
