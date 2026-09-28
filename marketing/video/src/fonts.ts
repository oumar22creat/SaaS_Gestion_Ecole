import { loadFont as loadOutfit } from '@remotion/google-fonts/Outfit';
import { loadFont as loadInter } from '@remotion/google-fonts/Inter';

/** Outfit pour les titres et les chiffres, Inter pour le corps — comme le produit. */
export const display = loadOutfit('normal', { weights: ['500', '600', '700'], subsets: ['latin'] }).fontFamily;
export const body = loadInter('normal', { weights: ['400', '500', '600'], subsets: ['latin'] }).fontFamily;
