import React from 'react';
import { AbsoluteFill, interpolate, spring, useCurrentFrame, useVideoConfig } from 'remotion';
import { Stage } from '../components/Stage';
import { COLORS, SMOOTH } from '../theme';
import { display } from '../fonts';

/**
 * Première image d'une capsule : la phrase qui doit arrêter le pouce.
 *
 * <p>Pas de logo, pas de marque. Sur TikTok, les deux premières secondes décident de tout, et
 * une marque en ouverture se lit comme une publicité — le doigt glisse. On ouvre donc sur la
 * douleur ; la marque arrive à la fin, quand la personne a une raison de la retenir.
 *
 * <p>Le texte est déjà lisible à l'image 1 : une animation d'entrée, même courte, gaspille la
 * seule seconde dont on dispose.
 */
export const Accroche: React.FC<{ texte: string; appui?: string }> = ({ texte, appui }) => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();

  const appuiIn = spring({ frame, fps, delay: 22, config: SMOOTH, durationInFrames: 20 });
  // Très léger rapprochement : l'image bouge sans distraire de la lecture.
  const zoom = interpolate(frame, [0, 90], [1, 1.04]);

  return (
    <Stage>
      <AbsoluteFill
        style={{
          alignItems: 'center',
          justifyContent: 'center',
          padding: '0 60px',
          transform: `scale(${zoom})`,
        }}
      >
        <div
          style={{
            fontFamily: display,
            fontSize: 96,
            fontWeight: 700,
            letterSpacing: -2,
            lineHeight: 1.05,
            textAlign: 'center',
          }}
        >
          {texte}
          {/* Le point d'accent ne s'ajoute qu'à une phrase non ponctuée : une question se
              terminait sinon par « ?. ». */}
          {/[?!.]$/.test(texte) ? null : <span style={{ color: COLORS.gold }}>.</span>}
        </div>

        {appui ? (
          <div
            style={{
              fontFamily: display,
              fontSize: 44,
              fontWeight: 600,
              color: COLORS.gold,
              marginTop: 40,
              textAlign: 'center',
              opacity: appuiIn,
              transform: `translateY(${(1 - appuiIn) * 18}px)`,
            }}
          >
            {appui}
          </div>
        ) : null}
      </AbsoluteFill>
    </Stage>
  );
};
