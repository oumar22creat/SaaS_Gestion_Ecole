# Capsules TikTok

Six vidéos de 18 secondes, 1080×1920, tirées du même projet que le film long.
`npm run capsules` les régénère toutes.

## Pourquoi elles diffèrent du film

| Film de présentation | Capsule |
|---|---|
| 54 s, tout le produit | 18 s, **une seule idée** |
| Ouvre sur le logo | Ouvre sur la douleur — le logo n'arrive qu'à la fin |
| Regardée volontairement | Interceptée au passage |
| Voix off | Muette : tout le propos est à l'écran |

Sur TikTok, les deux premières secondes décident de tout. Une marque en ouverture se lit
comme une publicité et le doigt glisse ; c'est pourquoi l'accroche est lisible dès la
première image, sans animation d'entrée qui gaspillerait la seule seconde disponible.

Les marges sont celles de TikTok (`src/format.ts`), pas celles du film : l'interface recouvre
le bas de l'écran et la colonne de droite.

## Les six capsules

| Fichier | Accroche | Montre |
|---|---|---|
| `tiktok-bulletins.mp4` | Trois semaines pour sortir les bulletins | Le bulletin qui se compose seul |
| `tiktok-appel.mp4` | Le cahier d'appel a encore disparu | L'appel en trente secondes |
| `tiktok-frais.mp4` | Qui a payé ce trimestre ? | Les impayés en un écran |
| `tiktok-securite.mp4` | À qui confiez-vous les dossiers de vos élèves ? | L'isolation entre établissements |
| `tiktok-parents.mp4` | « Montre-moi ton carnet » | Le portail famille |
| `tiktok-eleves.mp4` | Un élève connaît-il vraiment sa moyenne ? | Le suivi par l'élève |

Les quatre rubriques du portail (notes, absences, emploi du temps, frais) sont celles que
l'API sert réellement (`PortalController`). Une vidéo qui annonce une fonctionnalité absente
se paie au premier essai gratuit.

## Légendes prêtes à coller

**Bulletins**
> Trois semaines de travail pour sortir les bulletins. Chaque trimestre. On a construit
> l'outil qui les sort en une fois. 🇲🇱

**Appel**
> Le cahier d'appel égaré, les parents prévenus le vendredi. L'appel se fait maintenant en
> trente secondes, depuis le téléphone.

**Frais**
> « Qui a payé ce trimestre ? » — si la réponse est dans un cahier, cette vidéo est pour vous.

**Sécurité**
> Les notes et les absences de vos élèves n'appartiennent qu'à vous. Aucune autre école ne
> peut les voir. Jamais.

**Parents**
> Les parents ne réclament plus le carnet : notes, absences et emploi du temps sont sur leur
> téléphone.

**Élèves**
> Un élève qui découvre sa moyenne le jour du bulletin découvre toujours trop tard.

## Hashtags

```
#ecolemali #bamako #directeurdecole #enseignantmalien #educationmali #gestionscolaire
```

Éviter `#fyp` et les hashtags génériques : ils exposent à un public mondial qui ne sera jamais
client, ce qui dilue l'audience aux yeux de l'algorithme.

## Publication

- **Une par semaine**, dans l'ordre du tableau : la douleur la plus vive d'abord.
- **19 h – 22 h**, heure de Bamako.
- **Lien en bio vers WhatsApp** (`https://wa.me/22379827979`), pas vers le site : un directeur
  qui écrit entre en conversation, un directeur qui visite un site disparaît.
- **Répondre aux commentaires dans l'heure.** Sur un compte jeune, chaque échange compte
  double, et un « ça coûte combien ? » auquel on répond publiquement vaut dix vues.

## Ce qu'il faut mesurer

Pas les vues : **le nombre de messages WhatsApp reçus par vidéo publiée**. Dix mille vues sans
un seul message signalent un problème d'appel à l'action, pas de portée — il faut alors
changer la fin, pas publier davantage.

## Ajouter une voix off

Les capsules sont muettes à dessein : on regarde TikTok sans le son. Si vous voulez malgré
tout une voix, enregistrez une réplique par capsule — l'accroche suffit — et déposez les
fichiers dans `public/voix-off/` ; le montage est le même que pour le film long
(`src/VoixOff.tsx`).
