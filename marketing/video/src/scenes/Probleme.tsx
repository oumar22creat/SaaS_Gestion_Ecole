import React from 'react';
import { AbsoluteFill, interpolate, spring, useCurrentFrame, useVideoConfig } from 'remotion';
import { Stage } from '../components/Stage';
import { COLORS, SMOOTH } from '../theme';
import { display, body } from '../fonts';
import { usePortrait } from '../format';

/** Les trois supports que l’application remplace, tels que les décrit le site vitrine. */
const SILOS = [
  { label: 'Les notes', support: 'dans un tableur', x: -430, y: -230, rotate: -7 },
  { label: 'Les absences', support: 'sur papier', x: 0, y: 0, rotate: 3 },
  { label: 'Les frais', support: 'dans un cahier', x: 430, y: 230, rotate: 8 },
];

/**
 * La scène du problème. Les trois cartes entrent séparément puis s’éloignent encore : c’est
 * l’éparpillement qui doit se voir, pas les objets eux-mêmes.
 */
export const Probleme: React.FC = () => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();
  const portrait = usePortrait();

  const punch = spring({ frame, fps, delay: 112, config: SMOOTH, durationInFrames: 26 });
  // Les cartes disparaissent AVANT que la phrase n’arrive : superposées, on ne lit ni l’une
  // ni les autres. Le vide d’une demi-seconde entre les deux est ce qui donne du poids à la
  // phrase.
  const dim = interpolate(frame, [92, 110], [1, 0], {
    extrapolateLeft: 'clamp',
    extrapolateRight: 'clamp',
  });

  return (
    <Stage>
      <AbsoluteFill style={{ alignItems: 'center', justifyContent: 'center' }}>
        <div
          style={{
            position: 'relative',
            width: portrait ? 900 : 1500,
            height: portrait ? 700 : 330,
            opacity: dim,
          }}
        >
          {SILOS.map((silo, index) => {
            const enter = spring({
              frame,
              fps,
              delay: 6 + index * 12,
              config: { damping: 18, stiffness: 140 },
            });
            // Dérive continue vers l’extérieur : les silos s’écartent avec le temps. En 9:16
            // l’écartement se fait en hauteur, seul axe qui reste disponible.
            const axis = portrait ? silo.y : silo.x;
            const spread = interpolate(frame, [30, 110], [0, 46 * Math.sign(axis || 1)], {
              extrapolateLeft: 'clamp',
              extrapolateRight: 'clamp',
            });

            return (
              <div
                key={silo.label}
                style={{
                  position: 'absolute',
                  left: '50%',
                  width: portrait ? 620 : 380,
                  marginLeft: portrait ? -310 : -190,
                  top: portrait ? '50%' : 0,
                  marginTop: portrait ? -95 : 0,
                  padding: '38px 36px',
                  borderRadius: 22,
                  backgroundColor: 'rgba(255,255,255,0.05)',
                  border: `1px solid ${COLORS.border}`,
                  opacity: enter,
                  transform: [
                    `translateX(${portrait ? 0 : silo.x + spread}px)`,
                    `translateY(${(portrait ? silo.y + spread : 0) + (1 - enter) * 40}px)`,
                    `rotate(${silo.rotate * enter}deg)`,
                  ].join(' '),
                }}
              >
                <div style={{ fontFamily: display, fontSize: portrait ? 52 : 46, fontWeight: 600 }}>{silo.label}</div>
                <div style={{ fontFamily: body, fontSize: 30, color: COLORS.mutedOnDark, marginTop: 10 }}>
                  {silo.support}
                </div>
              </div>
            );
          })}
        </div>

        <div
          style={{
            position: 'absolute',
            fontFamily: display,
            fontSize: portrait ? 70 : 82,
            fontWeight: 600,
            letterSpacing: -1,
            textAlign: 'center',
            opacity: punch,
            transform: `translateY(${(1 - punch) * 26}px)`,
          }}
        >
          Personne ne voit l’ensemble
          <span style={{ color: COLORS.gold }}>.</span>
        </div>
      </AbsoluteFill>
    </Stage>
  );
};
