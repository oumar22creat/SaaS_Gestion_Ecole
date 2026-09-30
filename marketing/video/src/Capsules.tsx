import React from 'react';
import { AbsoluteFill, Audio, Sequence, interpolate, staticFile, useVideoConfig } from 'remotion';
import { TransitionSeries, linearTiming } from '@remotion/transitions';
import { fade } from '@remotion/transitions/fade';
import { CadreTikTok } from './format';
import { COLORS } from './theme';
import { Accroche } from './capsules/Accroche';
import { ClotureCourte } from './capsules/ClotureCourte';
import { Frais } from './capsules/scenes/Frais';
import { Portail } from './capsules/scenes/Portail';
import { Bulletins } from './scenes/Bulletins';
import { Mobile } from './scenes/Mobile';
import { Securite } from './scenes/Securite';

/**
 * Capsules courtes destinées à TikTok.
 *
 * <p>Le film long est un argumentaire complet : il se regarde volontairement. Une capsule
 * s'intercepte au passage, et n'a droit qu'à une seule idée. D'où une structure fixe en trois
 * temps — l'accroche qui arrête le pouce, la démonstration, l'appel à l'action — et une durée
 * de dix-huit secondes.
 *
 * <p>Pas de voix off : ces vidéos se regardent sans le son, tout le propos est à l'écran. Si
 * vous enregistrez six répliques, elles s'ajoutent en quelques minutes.
 */
const ACCROCHE = 90;
const DEMO = 330;
const CLOTURE = 150;
const TRANSITION = 14;

/** 90 + 330 + 150 − 2 × 14 = 542 images, soit 18,1 s. */
export const DUREE_CAPSULE = ACCROCHE + DEMO + CLOTURE - 2 * TRANSITION;

export interface Capsule {
  id: string;
  accroche: string;
  appui?: string;
  promesse: string;
  demo: React.FC;
}

export const CAPSULES: Capsule[] = [
  {
    id: 'CapsuleBulletins',
    accroche: 'Trois semaines pour sortir les bulletins',
    appui: 'Et il faut tout recommencer au trimestre suivant',
    promesse: 'Les bulletins arrivent prêts',
    demo: Bulletins,
  },
  {
    id: 'CapsuleAppel',
    accroche: 'Le cahier d’appel a encore disparu',
    appui: 'Et les parents ne sauront rien avant vendredi',
    promesse: 'L’appel en trente secondes',
    demo: Mobile,
  },
  {
    id: 'CapsuleFrais',
    accroche: 'Qui a payé ce trimestre ?',
    appui: 'La réponse est dans un cahier, quelque part',
    promesse: 'Les impayés en un écran',
    demo: Frais,
  },
  {
    id: 'CapsuleSecurite',
    accroche: 'À qui confiez-vous les dossiers de vos élèves ?',
    promesse: 'Vos données restent les vôtres',
    demo: Securite,
  },
  {
    id: 'CapsuleParents',
    accroche: '« Montre-moi ton carnet »',
    appui: 'Et le carnet est toujours resté à l’école',
    promesse: 'Les parents suivent depuis leur téléphone',
    demo: () => (
      <Portail
        titre="Les parents voient tout"
        sousTitre="Depuis leur téléphone, sans rien demander"
        entete="Mes enfants"
      />
    ),
  },
  {
    id: 'CapsuleEleves',
    accroche: 'Un élève connaît-il vraiment sa moyenne ?',
    appui: 'Avant le bulletin, il découvre tout trop tard',
    promesse: 'Chaque élève suit ses résultats',
    demo: () => (
      <Portail
        titre="L’élève suit ses résultats"
        sousTitre="Ses notes, ses absences, son emploi du temps"
        entete="Mon suivi"
      />
    ),
  },
];

/** Musique de fond, montée puis descendue : aucune voix à ménager ici. */
const Fond: React.FC = () => {
  const { durationInFrames } = useVideoConfig();
  return (
    <Audio
      src={staticFile('musique-fond.mp3')}
      // Départ à la 16e seconde du morceau : son intro monte lentement, ce qui conviendrait
      // mal à une capsule qui doit frapper tout de suite.
      trimBefore={16 * 30}
      volume={(frame) =>
        0.22 *
        interpolate(frame, [0, 20], [0, 1], { extrapolateRight: 'clamp' }) *
        interpolate(frame, [durationInFrames - 45, durationInFrames], [1, 0], {
          extrapolateLeft: 'clamp',
          extrapolateRight: 'clamp',
        })
      }
    />
  );
};

/**
 * Reçoit l'identifiant de la capsule, pas la capsule elle-même : Remotion sérialise les
 * propriétés d'une composition en JSON, et un composant React ne survit pas au passage — la
 * démonstration arrivait « undefined » au rendu.
 */
export const CapsuleVideo: React.FC<{ id: string }> = ({ id }) => {
  const capsule = CAPSULES.find((c) => c.id === id);
  if (!capsule) {
    throw new Error(`Capsule inconnue : ${id}`);
  }
  const Demo = capsule.demo;
  return (
    <CadreTikTok.Provider value={true}>
      <AbsoluteFill style={{ backgroundColor: COLORS.ink }}>
        <Sequence>
          <Fond />
        </Sequence>
        <TransitionSeries>
          <TransitionSeries.Sequence durationInFrames={ACCROCHE}>
            <Accroche texte={capsule.accroche} appui={capsule.appui} />
          </TransitionSeries.Sequence>
          <TransitionSeries.Transition
            presentation={fade()}
            timing={linearTiming({ durationInFrames: TRANSITION })}
          />
          <TransitionSeries.Sequence durationInFrames={DEMO}>
            <Demo />
          </TransitionSeries.Sequence>
          <TransitionSeries.Transition
            presentation={fade()}
            timing={linearTiming({ durationInFrames: TRANSITION })}
          />
          <TransitionSeries.Sequence durationInFrames={CLOTURE}>
            <ClotureCourte promesse={capsule.promesse} />
          </TransitionSeries.Sequence>
        </TransitionSeries>
      </AbsoluteFill>
    </CadreTikTok.Provider>
  );
};
