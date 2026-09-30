import { createContext, useContext } from 'react';
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

/**
 * Marges du cadre TikTok.
 *
 * <p>L'interface de TikTok recouvre le bas de l'écran (légende, nom du compte, musique) et la
 * colonne de droite (boutons j'aime, commentaires, partage). Les marges du film long ne
 * suffisent pas : un numéro de téléphone posé dessous passerait sous la légende, et un texte
 * qui court jusqu'au bord droit finirait derrière les boutons.
 */
export const TIKTOK_HAUT = 260;
export const TIKTOK_BAS = 440;
export const TIKTOK_DROITE = 150;

/** Vrai à l'intérieur d'une capsule : le décor applique alors les marges ci-dessus. */
export const CadreTikTok = createContext(false);
export const useTikTok = (): boolean => useContext(CadreTikTok);
