import React from 'react';
import { AbsoluteFill, interpolate, spring, useCurrentFrame, useVideoConfig } from 'remotion';
import { Stage } from '../components/Stage';
import { Mark } from '../components/Mark';
import { COLORS, SMOOTH } from '../theme';
import { display, body } from '../fonts';
import { usePortrait } from '../format';

export const Intro: React.FC = () => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();
  const portrait = usePortrait();

  const markIn = spring({ frame, fps, config: { damping: 14, stiffness: 120 } });
  const wordIn = spring({ frame, fps, delay: 12, config: SMOOTH, durationInFrames: 24 });
  const ruleIn = spring({ frame, fps, delay: 22, config: SMOOTH, durationInFrames: 30 });
  const taglineIn = spring({ frame, fps, delay: 30, config: SMOOTH, durationInFrames: 24 });

  // Respiration lente sur toute la scène : l’image n’est jamais complètement figée.
  const drift = interpolate(frame, [0, 120], [0, -14]);

  return (
    <Stage>
      <AbsoluteFill
        style={{
          alignItems: 'center',
          justifyContent: 'center',
          transform: `translateY(${drift}px)`,
        }}
      >
        <div style={{ transform: `scale(${markIn})`, marginBottom: 46 }}>
          <Mark size={portrait ? 150 : 132} />
        </div>

        <div
          style={{
            fontFamily: display,
            fontSize: portrait ? 86 : 108,
            fontWeight: 700,
            letterSpacing: -2,
            opacity: wordIn,
            transform: `translateY(${(1 - wordIn) * 22}px)`,
          }}
        >
          School Manager
        </div>

        <div
          style={{
            width: interpolate(ruleIn, [0, 1], [0, 220]),
            height: 3,
            borderRadius: 2,
            backgroundColor: COLORS.gold,
            margin: '30px 0',
          }}
        />

        <div
          style={{
            fontFamily: body,
            fontSize: portrait ? 36 : 34,
            letterSpacing: 1,
            color: COLORS.mutedOnDark,
            opacity: taglineIn,
          }}
        >
          Plateforme de gestion scolaire
        </div>
      </AbsoluteFill>
    </Stage>
  );
};
