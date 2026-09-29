import React from 'react';
import { Audio, interpolate, staticFile, useVideoConfig } from 'remotion';
import { VOIX_OFF } from './voix-off';

/**
 * Niveaux de la musique de fond.
 *
 * <p>Deux valeurs, pas une : une musique posée à volume constant sous une voix oblige à
 * choisir entre l'entendre et comprendre le texte. Elle respire dans les silences et
 * s'efface pendant la parole — c'est ce qui permet de la garder présente sans gêner.
 */
const VOLUME_SEUL = 0.18;
const VOLUME_SOUS_VOIX = 0.06;
/** Montée et descente du niveau autour de chaque réplique. Trop court s'entend comme un à-coup. */
const RAMPE = 9;
const ENTREE = 45;
const SORTIE = 75;

/**
 * Musique de fond, découpée dans le morceau fourni (118 s pour un film de 54 s).
 *
 * <p>On part du début du morceau : son intro monte progressivement, ce qui accompagne l'arc
 * du film — ouverture calme, plein régime sur les tarifs et la clôture. Le morceau a sa
 * propre fin bien au-delà de notre durée, d'où le fondu de sortie ajouté ici.
 */
export const Musique: React.FC<{ debuts: number[] }> = ({ debuts }) => {
  const { fps, durationInFrames } = useVideoConfig();

  // Fenêtres de parole, calculées depuis les pistes réellement montées.
  const paroles = VOIX_OFF.filter((p) => p.fichier !== null).map((p) => {
    const depart = debuts[p.scene] + p.retard;
    return [depart, depart + Math.round(p.duree * fps)] as const;
  });

  const volume = (frame: number): number => {
    // 1 pendant la parole, 0 dans les silences, avec une rampe de part et d'autre.
    const parle = paroles.reduce((max, [a, b]) => {
      const montee = interpolate(frame, [a - RAMPE, a], [0, 1], {
        extrapolateLeft: 'clamp',
        extrapolateRight: 'clamp',
      });
      const descente = interpolate(frame, [b, b + RAMPE], [1, 0], {
        extrapolateLeft: 'clamp',
        extrapolateRight: 'clamp',
      });
      return Math.max(max, Math.min(montee, descente));
    }, 0);

    const niveau = interpolate(parle, [0, 1], [VOLUME_SEUL, VOLUME_SOUS_VOIX]);
    const entree = interpolate(frame, [0, ENTREE], [0, 1], { extrapolateRight: 'clamp' });
    const sortie = interpolate(frame, [durationInFrames - SORTIE, durationInFrames], [1, 0], {
      extrapolateLeft: 'clamp',
      extrapolateRight: 'clamp',
    });
    return niveau * entree * sortie;
  };

  return <Audio src={staticFile('musique-fond.mp3')} volume={volume} />;
};
