/**
 * Tokens repris de docs/DESIGN.md §Palette et §Typographie — la vidéo doit ressembler au
 * produit, pas à une plaquette générique. Un prospect qui ouvre l'application après avoir vu
 * la vidéo doit reconnaître les mêmes couleurs.
 */
export const COLORS = {
  /** Vert profond de la marque (Tenant.java, migration V53). */
  primary: '#0f5c4c',
  primaryBright: '#15806a',
  /** Or secondaire : réservé aux accents, jamais à des aplats. */
  gold: '#c9a227',
  /** Ardoise sombre de la navigation, fond principal de la vidéo. */
  ink: '#0b231e',
  inkDeep: '#071711',
  surface: '#ffffff',
  paper: '#f7f7f5',
  text: '#0b231e',
  textOnDark: '#f2f6f4',
  mutedOnDark: 'rgba(242, 246, 244, 0.62)',
  muted: '#5c6b66',
  border: 'rgba(242, 246, 244, 0.14)',
} as const;

export const FPS = 30;

/** Ressort de référence : une entrée qui se pose, sans rebond. Voir rules/timing.md. */
export const SMOOTH = { damping: 200 } as const;
/** Pour les éléments d'interface qui doivent claquer un peu. */
export const SNAPPY = { damping: 26, stiffness: 190 } as const;
