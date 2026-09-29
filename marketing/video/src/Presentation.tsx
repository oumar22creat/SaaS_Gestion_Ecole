import React from 'react';
import { AbsoluteFill, useVideoConfig } from 'remotion';
import { TransitionSeries, linearTiming, springTiming } from '@remotion/transitions';
import { fade } from '@remotion/transitions/fade';
import { slide } from '@remotion/transitions/slide';
import { COLORS } from './theme';
import { Intro } from './scenes/Intro';
import { Probleme } from './scenes/Probleme';
import { Promesse } from './scenes/Promesse';
import { Modules } from './scenes/Modules';
import { Mobile } from './scenes/Mobile';
import { Bulletins } from './scenes/Bulletins';
import { Securite } from './scenes/Securite';
import { Tarifs } from './scenes/Tarifs';
import { Cloture } from './scenes/Cloture';
import { VoixOff } from './VoixOff';

/**
 * Durées en images (30 i/s). Elles sont calées sur le temps de lecture du texte français à
 * l’écran, pas sur un rythme abstrait : une phrase de huit mots demande environ deux secondes
 * une fois entrée.
 */
export const SCENES = [
  // Rallongée pour que la réplique d’ouverture finisse avant la transition.
  { component: Intro, duration: 160 },
  { component: Probleme, duration: 195 },
  { component: Promesse, duration: 140 },
  { component: Modules, duration: 240 },
  { component: Mobile, duration: 195 },
  { component: Bulletins, duration: 195 },
  { component: Securite, duration: 180 },
  { component: Tarifs, duration: 210 },
  // Plus longue que les autres : c’est l’image où le spectateur note le numéro.
  { component: Cloture, duration: 240 },
] as const;

/**
 * Fondu partout sauf entre le problème et la promesse, où un glissement latéral appuie le
 * renversement : l’éparpillement sort par la gauche, la plateforme entre par la droite.
 */
const TRANSITIONS = [
  { presentation: fade(), timing: linearTiming({ durationInFrames: 14 }) },
  { presentation: slide({ direction: 'from-right' }), timing: springTiming({ config: { damping: 200 }, durationInFrames: 22 }) },
  { presentation: fade(), timing: linearTiming({ durationInFrames: 16 }) },
  { presentation: fade(), timing: linearTiming({ durationInFrames: 16 }) },
  { presentation: fade(), timing: linearTiming({ durationInFrames: 16 }) },
  { presentation: fade(), timing: linearTiming({ durationInFrames: 16 }) },
  { presentation: fade(), timing: linearTiming({ durationInFrames: 16 }) },
  { presentation: fade(), timing: linearTiming({ durationInFrames: 16 }) },
] as const;

/** Somme des scènes moins le recouvrement des transitions (voir rules/transitions.md). */
export const totalDuration = (fps: number): number => {
  const scenes = SCENES.reduce((sum, scene) => sum + scene.duration, 0);
  const overlap = TRANSITIONS.reduce((sum, t) => sum + t.timing.getDurationInFrames({ fps }), 0);
  return scenes - overlap;
};

/**
 * Image de départ de chaque scène dans le film monté.
 *
 * <p>Une transition recouvre les deux plans qu'elle relie : la scène suivante commence donc
 * avant que la précédente ne finisse. C'est là-dessus que se cale la voix off, et c'est
 * pourquoi ces débuts sont calculés plutôt que notés à la main — une scène rallongée décale
 * tout ce qui suit.
 */
export const sceneStarts = (fps: number): number[] => {
  const starts: number[] = [];
  let cursor = 0;
  SCENES.forEach((scene, index) => {
    starts.push(cursor);
    const transition = TRANSITIONS[index];
    cursor += scene.duration - (transition ? transition.timing.getDurationInFrames({ fps }) : 0);
  });
  return starts;
};

export const Presentation: React.FC = () => {
  const { fps } = useVideoConfig();

  return (
    <AbsoluteFill style={{ backgroundColor: COLORS.ink }}>
      <VoixOff debuts={sceneStarts(fps)} />
      <TransitionSeries>
        {SCENES.map((scene, index) => {
          const Scene = scene.component;
          const transition = TRANSITIONS[index];
          return (
            <React.Fragment key={index}>
              <TransitionSeries.Sequence durationInFrames={scene.duration}>
                <Scene />
              </TransitionSeries.Sequence>
              {transition ? (
                <TransitionSeries.Transition
                  presentation={transition.presentation}
                  timing={transition.timing}
                />
              ) : null}
            </React.Fragment>
          );
        })}
      </TransitionSeries>
    </AbsoluteFill>
  );
};
