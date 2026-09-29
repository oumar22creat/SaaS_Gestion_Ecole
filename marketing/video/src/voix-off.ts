/**
 * Bande voix off, scène par scène.
 *
 * <p>Tant que `fichier` vaut `null`, la vidéo se rend sans son — c'est l'état actuel, en
 * attente de l'enregistrement. Il suffit de déposer les fichiers dans `public/voix-off/` et
 * d'inscrire leur nom ici pour que les deux formats les embarquent.
 *
 * <p>Une piste par scène plutôt qu'un fichier unique : chaque réplique se pose exactement sur
 * son image de départ, et une prise refaite ne décale pas tout ce qui suit. C'est aussi ce
 * qui permet d'allonger une scène dont la réplique déborde sans toucher aux autres.
 */
export interface VoixOffPiste {
  /** Scène concernée, dans l'ordre de `SCENES` (src/Presentation.tsx). */
  scene: number;
  /** Nom du fichier dans `public/voix-off/`, ou null tant qu'il n'est pas enregistré. */
  fichier: string | null;
  /**
   * Décalage en images par rapport au début de la scène. Sert à laisser respirer une image
   * avant que la voix n'entre — l'ouverture, notamment, où le logo doit se poser seul.
   */
  retard: number;
  /** Texte attendu, pour relire le montage sans rouvrir le script. */
  texte: string;
}

export const VOIX_OFF: VoixOffPiste[] = [
  { scene: 0, fichier: null, retard: 30, texte: "Diriger une école, c’est tenir cent choses à la fois." },
  { scene: 1, fichier: null, retard: 12, texte: "Chaque information vit dans son coin. Aucune ne parle à l’autre." },
  { scene: 2, fichier: null, retard: 10, texte: "School Manager rassemble tout, avec les droits de chacun." },
  { scene: 3, fichier: null, retard: 12, texte: "Vingt modules, de l’inscription au bulletin. Tout ce que votre école gère déjà, sans le papier." },
  { scene: 4, fichier: null, retard: 12, texte: "L’enseignant fait l’appel depuis son téléphone. Trente secondes, et c’est enregistré." },
  { scene: 5, fichier: null, retard: 12, texte: "Les bulletins sortent à votre en-tête, moyennes et rangs calculés. Plus rien à ressaisir." },
  { scene: 6, fichier: null, retard: 12, texte: "Vos données sont isolées. Aucune école ne voit celles d’une autre. Jamais." },
  { scene: 7, fichier: null, retard: 12, texte: "Un plan selon votre effectif, à partir de cent quatre-vingt mille francs par an." },
  { scene: 8, fichier: null, retard: 10, texte: "Trente jours d’essai gratuit. Écrivez-nous sur WhatsApp, le numéro est à l’écran." },
];

/** Vrai dès qu'au moins une piste est enregistrée : évite de monter une bande vide. */
export const aUneVoixOff = (): boolean => VOIX_OFF.some((piste) => piste.fichier !== null);
