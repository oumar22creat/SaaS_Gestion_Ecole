import { useVideoConfig } from 'remotion';

/**
 * Le film existe en deux cadres : 16:9 pour une présentation à l'écran, 9:16 pour le statut
 * WhatsApp — le canal par lequel une vidéo circule réellement au Mali.
 *
 * <p>Les scènes s'adaptent au lieu d'être dupliquées : le texte, les prix et le minutage
 * n'existent qu'une fois. Deux jeux de fichiers auraient divergé dès la première correction
 * de tarif.
 */
export const usePortrait = (): boolean => {
  const { width, height } = useVideoConfig();
  return height > width;
};

/**
 * Marges hautes et basses réservées en 9:16.
 *
 * <p>Le statut WhatsApp superpose le nom de l'expéditeur en haut et la zone « Répondre » en
 * bas : tout ce qui compte doit rester entre les deux, sinon le numéro de téléphone de la
 * dernière image passe sous l'interface.
 */
export const SAFE_TOP = 230;
export const SAFE_BOTTOM = 280;
