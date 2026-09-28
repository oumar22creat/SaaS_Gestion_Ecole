import React from 'react';
import { AbsoluteFill, interpolate, spring, useCurrentFrame, useVideoConfig } from 'remotion';
import { Stage } from '../components/Stage';
import { Kicker } from '../components/Kicker';
import { Glyph } from '../components/Glyph';
import { COLORS, SMOOTH } from '../theme';
import { display, body } from '../fonts';

const GARANTIES = [
  'Isolation au niveau de la base',
  'Accès par rôle, vérifié côté serveur',
  'Chiffrement et journalisation',
  'Portabilité garantie',
];

/**
 * L’argument le plus difficile à montrer : ce qui ne se produit pas. Deux établissements
 * côte à côte, une cloison qui se referme entre eux, et le chiffre zéro au centre.
 */
export const Securite: React.FC = () => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();

  const titleIn = spring({ frame, fps, config: SMOOTH, durationInFrames: 24 });
  const wall = spring({ frame, fps, delay: 26, config: SMOOTH, durationInFrames: 30 });
  const zeroIn = spring({ frame, fps, delay: 44, config: { damping: 13, stiffness: 140 } });

  return (
    <Stage>
      <AbsoluteFill style={{ alignItems: 'center', justifyContent: 'center', padding: 100 }}>
        <div style={{ opacity: titleIn, marginBottom: 22 }}>
          <Kicker>Sécurité</Kicker>
        </div>
        <div
          style={{
            fontFamily: display,
            fontSize: 76,
            fontWeight: 700,
            letterSpacing: -1.5,
            opacity: titleIn,
            transform: `translateY(${(1 - titleIn) * 20}px)`,
            marginBottom: 58,
          }}
        >
          Vos données restent les vôtres
        </div>

        {/* Deux établissements séparés par une cloison qui se referme. */}
        <div style={{ position: 'relative', display: 'flex', gap: 200, alignItems: 'center' }}>
          {['Établissement A', 'Établissement B'].map((nom, index) => (
            <div
              key={nom}
              style={{
                width: 420,
                padding: '38px 36px',
                borderRadius: 22,
                backgroundColor: 'rgba(255,255,255,0.055)',
                border: `1px solid ${COLORS.border}`,
                textAlign: 'center',
                opacity: spring({ frame, fps, delay: 14 + index * 8, config: SMOOTH, durationInFrames: 24 }),
              }}
            >
              <div style={{ display: 'grid', placeItems: 'center', marginBottom: 18 }}>
                <Glyph name="shield" color={COLORS.gold} size={42} />
              </div>
              <div style={{ fontFamily: display, fontSize: 38, fontWeight: 600 }}>{nom}</div>
            </div>
          ))}

          {/* La cloison : elle se dessine du centre vers les bords. */}
          <div
            style={{
              position: 'absolute',
              left: '50%',
              top: '50%',
              width: 3,
              height: interpolate(wall, [0, 1], [0, 214]),
              marginLeft: -1.5,
              marginTop: interpolate(wall, [0, 1], [0, -107]),
              borderRadius: 2,
              background: `linear-gradient(180deg, transparent, ${COLORS.gold}, transparent)`,
            }}
          />

          <div
            style={{
              position: 'absolute',
              left: '50%',
              top: '50%',
              transform: `translate(-50%, -50%) scale(${zeroIn})`,
              width: 140,
              height: 140,
              borderRadius: 999,
              backgroundColor: COLORS.ink,
              border: `2px solid ${COLORS.gold}`,
              display: 'grid',
              placeItems: 'center',
            }}
          >
            <span style={{ fontFamily: display, fontSize: 76, fontWeight: 700, color: COLORS.gold }}>0</span>
          </div>
        </div>

        <div
          style={{
            fontFamily: body,
            fontSize: 30,
            color: COLORS.mutedOnDark,
            marginTop: 42,
            opacity: zeroIn,
          }}
        >
          donnée partagée entre deux établissements
        </div>

        <div style={{ display: 'flex', gap: 18, marginTop: 52, flexWrap: 'wrap', justifyContent: 'center' }}>
          {GARANTIES.map((garantie, index) => (
            <div
              key={garantie}
              style={{
                padding: '14px 26px',
                borderRadius: 999,
                border: `1px solid ${COLORS.border}`,
                fontFamily: body,
                fontSize: 24,
                color: COLORS.textOnDark,
                opacity: spring({ frame, fps, delay: 62 + index * 6, config: SMOOTH, durationInFrames: 20 }),
              }}
            >
              {garantie}
            </div>
          ))}
        </div>
      </AbsoluteFill>
    </Stage>
  );
};
