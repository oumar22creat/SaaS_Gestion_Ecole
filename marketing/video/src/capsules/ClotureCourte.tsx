import React from 'react';
import { AbsoluteFill, interpolate, spring, useCurrentFrame, useVideoConfig } from 'remotion';
import { Stage } from '../components/Stage';
import { Mark } from '../components/Mark';
import { COLORS, SMOOTH } from '../theme';
import { display, body } from '../fonts';

/**
 * Dernière image d'une capsule : la marque, puis le seul geste attendu.
 *
 * <p>Un numéro WhatsApp plutôt qu'une adresse de site : un directeur qui écrit entre en
 * conversation, un directeur qui visite un site disparaît. C'est aussi le canal par lequel
 * l'abonnement se règle réellement.
 */
export const ClotureCourte: React.FC<{ promesse: string }> = ({ promesse }) => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();

  const markIn = spring({ frame, fps, config: { damping: 16, stiffness: 130 } });
  const texteIn = spring({ frame, fps, delay: 10, config: SMOOTH, durationInFrames: 22 });
  const contactIn = spring({ frame, fps, delay: 24, config: SMOOTH, durationInFrames: 22 });

  return (
    <Stage>
      <AbsoluteFill style={{ alignItems: 'center', justifyContent: 'center', padding: '0 60px' }}>
        <div style={{ transform: `scale(${markIn})`, marginBottom: 30 }}>
          <Mark size={104} />
        </div>

        <div
          style={{
            fontFamily: display,
            fontSize: 62,
            fontWeight: 700,
            letterSpacing: -1,
            textAlign: 'center',
            opacity: texteIn,
            transform: `translateY(${(1 - texteIn) * 20}px)`,
          }}
        >
          {promesse}
        </div>

        <div
          style={{
            fontFamily: body,
            fontSize: 30,
            color: COLORS.mutedOnDark,
            marginTop: 20,
            opacity: texteIn,
          }}
        >
          School Manager · 30 jours d’essai
        </div>

        <div
          style={{
            display: 'flex',
            alignItems: 'center',
            gap: 16,
            marginTop: 46,
            padding: '22px 44px',
            borderRadius: 999,
            backgroundColor: COLORS.gold,
            color: COLORS.inkDeep,
            opacity: contactIn,
            transform: `scale(${0.92 + contactIn * 0.08})`,
          }}
        >
          <svg width="34" height="34" viewBox="0 0 32 32" fill={COLORS.inkDeep}>
            <path d="M16 3C9.4 3 4 8.4 4 15c0 2.1.6 4.1 1.6 5.9L4 29l8.3-1.5c1.7.9 3.6 1.4 5.7 1.4 6.6 0 12-5.4 12-12S22.6 3 16 3zm0 21.8c-1.8 0-3.5-.5-5-1.4l-.4-.2-4.4.8.8-4.3-.2-.4c-1-1.6-1.5-3.4-1.5-5.3 0-5.4 4.4-9.8 9.8-9.8s9.8 4.4 9.8 9.8-4.5 10.8-8.9 10.8z" />
            <path d="M21.4 18.1c-.3-.2-1.8-.9-2-1-.3-.1-.5-.2-.7.2s-.8 1-.9 1.2c-.2.2-.3.2-.6.1-.3-.2-1.3-.5-2.4-1.5-.9-.8-1.5-1.8-1.7-2.1-.2-.3 0-.5.1-.6l.5-.5c.1-.2.2-.3.3-.5 0-.2 0-.4 0-.5 0-.2-.7-1.6-.9-2.2-.2-.6-.5-.5-.7-.5h-.6c-.2 0-.5.1-.8.4-.3.3-1 1-1 2.5s1.1 2.9 1.2 3.1c.2.2 2.1 3.2 5.1 4.5.7.3 1.3.5 1.7.6.7.2 1.4.2 1.9.1.6-.1 1.8-.7 2-1.5.2-.7.2-1.4.2-1.5-.1-.2-.3-.2-.6-.3z" />
          </svg>
          <span style={{ fontFamily: display, fontSize: 40, fontWeight: 700 }}>
            +223 79 82 79 79
          </span>
        </div>

        <AbsoluteFill
          style={{
            pointerEvents: 'none',
            backgroundColor: COLORS.inkDeep,
            opacity: interpolate(frame, [110, 135], [0, 1], {
              extrapolateLeft: 'clamp',
              extrapolateRight: 'clamp',
            }),
          }}
        />
      </AbsoluteFill>
    </Stage>
  );
};
