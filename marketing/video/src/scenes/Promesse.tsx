import React from 'react';
import { AbsoluteFill, interpolate, spring, useCurrentFrame, useVideoConfig } from 'remotion';
import { Stage } from '../components/Stage';
import { Kicker } from '../components/Kicker';
import { COLORS, SMOOTH } from '../theme';
import { display } from '../fonts';
import { usePortrait } from '../format';

/**
 * Le renversement : les trois silos éclatés se referment en une seule surface. Le mouvement
 * dit la promesse avant que le texte ne l’énonce.
 */
export const Promesse: React.FC = () => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();
  const portrait = usePortrait();

  const converge = spring({ frame, fps, config: SMOOTH, durationInFrames: 34 });
  const reveal = spring({ frame, fps, delay: 18, config: SMOOTH, durationInFrames: 24 });
  const kickerIn = spring({ frame, fps, delay: 24, config: SMOOTH, durationInFrames: 22 });
  const lineOne = spring({ frame, fps, delay: 32, config: SMOOTH, durationInFrames: 26 });
  const lineTwo = spring({ frame, fps, delay: 42, config: SMOOTH, durationInFrames: 26 });

  return (
    <Stage>
      <AbsoluteFill style={{ alignItems: 'center', justifyContent: 'center' }}>
        {/* Les trois fragments viennent se superposer en une carte unique. */}
        <div
          style={{
            position: 'relative',
            width: portrait ? 760 : 520,
            height: portrait ? 440 : 300,
            marginBottom: portrait ? 78 : 58,
          }}
        >
          {[-1, 0, 1].map((slot) => (
            <div
              key={slot}
              style={{
                position: 'absolute',
                inset: 0,
                borderRadius: 26,
                border: `1px solid ${COLORS.border}`,
                backgroundColor: slot === 0 ? 'rgba(21,128,106,0.30)' : 'rgba(255,255,255,0.05)',
                transform: [
                  `translateX(${interpolate(converge, [0, 1], [slot * (portrait ? 250 : 430), 0])}px)`,
                  `rotate(${interpolate(converge, [0, 1], [slot * 7, 0])}deg)`,
                  `scale(${interpolate(converge, [0, 1], [0.86, 1])})`,
                ].join(' '),
                opacity: slot === 0 ? 1 : interpolate(converge, [0.55, 1], [1, 0], { extrapolateLeft: 'clamp' }),
              }}
            />
          ))}

          {/* Esquisse d'application dans la carte unifiée : sans elle, la surface verte se lit
              comme un rectangle vide plutôt que comme un produit. */}
          <div style={{ position: 'absolute', inset: 0, padding: 26, opacity: reveal }}>
            <div
              style={{
                height: 30,
                width: 150,
                borderRadius: 8,
                backgroundColor: 'rgba(242,246,244,0.28)',
                marginBottom: 20,
              }}
            />
            <div style={{ display: 'flex', gap: 12, marginBottom: 18 }}>
              {[0, 1, 2].map((tile) => (
                <div
                  key={tile}
                  style={{
                    flex: 1,
                    height: 76,
                    borderRadius: 12,
                    backgroundColor: tile === 1 ? 'rgba(201,162,39,0.34)' : 'rgba(242,246,244,0.16)',
                  }}
                />
              ))}
            </div>
            {[0, 1, 2, 3].map((row) => (
              <div
                key={row}
                style={{
                  height: 20,
                  marginBottom: 10,
                  borderRadius: 6,
                  width: `${100 - row * 9}%`,
                  backgroundColor: 'rgba(242,246,244,0.12)',
                }}
              />
            ))}
          </div>
        </div>

        <div style={{ opacity: kickerIn, marginBottom: 26 }}>
          <Kicker>Une seule plateforme</Kicker>
        </div>

        <div
          style={{
            fontFamily: display,
            fontSize: portrait ? 82 : 94,
            fontWeight: 700,
            letterSpacing: -2,
            lineHeight: 1.08,
            textAlign: 'center',
          }}
        >
          <div style={{ opacity: lineOne, transform: `translateY(${(1 - lineOne) * 24}px)` }}>
            Toute votre école,
          </div>
          <div style={{ opacity: lineTwo, transform: `translateY(${(1 - lineTwo) * 24}px)` }}>
            au même endroit<span style={{ color: COLORS.gold }}>.</span>
          </div>
        </div>
      </AbsoluteFill>
    </Stage>
  );
};
