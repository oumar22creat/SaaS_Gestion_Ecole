import React from 'react';
import { AbsoluteFill, interpolate, spring, useCurrentFrame, useVideoConfig } from 'remotion';
import { Stage } from '../components/Stage';
import { Kicker } from '../components/Kicker';
import { COLORS, SMOOTH, SNAPPY } from '../theme';
import { display, body } from '../fonts';

const ELEVES = [
  'Aminata Traoré',
  'Modibo Keïta',
  'Fatoumata Sidibé',
  'Sékou Coulibaly',
  'Awa Diarra',
  'Oumar Diallo',
];

/** Une coche qui se dessine, plutôt qu’une coche qui apparaît : le geste se voit. */
const Check: React.FC<{ progress: number }> = ({ progress }) => (
  <svg width="30" height="30" viewBox="0 0 30 30" fill="none">
    <circle cx="15" cy="15" r="14" fill={COLORS.primary} opacity={progress} />
    <path
      d="M9 15.5l4 4 8-8"
      stroke="#ffffff"
      strokeWidth="2.6"
      strokeLinecap="round"
      strokeLinejoin="round"
      strokeDasharray="18"
      strokeDashoffset={18 - 18 * progress}
    />
  </svg>
);

export const Mobile: React.FC = () => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();

  const phoneIn = spring({ frame, fps, delay: 6, config: { damping: 22, stiffness: 110 } });
  const textIn = spring({ frame, fps, delay: 16, config: SMOOTH, durationInFrames: 26 });

  // Le chronomètre monte jusqu’à 30 : c’est la promesse du site, montrée plutôt qu’écrite.
  const seconds = Math.round(
    interpolate(frame, [40, 150], [0, 30], { extrapolateLeft: 'clamp', extrapolateRight: 'clamp' }),
  );

  return (
    <Stage>
      <AbsoluteFill
        style={{
          flexDirection: 'row',
          alignItems: 'center',
          justifyContent: 'center',
          gap: 120,
          padding: 110,
        }}
      >
        <div style={{ width: 760, opacity: textIn, transform: `translateX(${(1 - textIn) * -30}px)` }}>
          <div style={{ marginBottom: 24 }}>
            <Kicker>Sur le terrain</Kicker>
          </div>
          <div
            style={{
              fontFamily: display,
              fontSize: 86,
              fontWeight: 700,
              letterSpacing: -2,
              lineHeight: 1.06,
            }}
          >
            L’appel se fait en trente secondes
            <span style={{ color: COLORS.gold }}>.</span>
          </div>
          <div
            style={{
              fontFamily: body,
              fontSize: 32,
              lineHeight: 1.5,
              color: COLORS.mutedOnDark,
              marginTop: 34,
            }}
          >
            Toute la classe validée depuis un seul écran, sur Android comme sur iOS — avec le logo
            de votre établissement.
          </div>
        </div>

        {/* Téléphone */}
        <div
          style={{
            width: 430,
            height: 860,
            borderRadius: 54,
            padding: 14,
            backgroundColor: '#050f0c',
            border: `2px solid ${COLORS.border}`,
            boxShadow: '0 40px 90px rgba(0,0,0,0.45)',
            opacity: phoneIn,
            transform: `translateY(${(1 - phoneIn) * 60}px) scale(${0.94 + phoneIn * 0.06})`,
          }}
        >
          <div
            style={{
              width: '100%',
              height: '100%',
              borderRadius: 42,
              backgroundColor: COLORS.paper,
              overflow: 'hidden',
            }}
          >
            {/* En-tête de l’écran d’appel */}
            <div style={{ backgroundColor: COLORS.ink, padding: '34px 28px 26px' }}>
              <div style={{ fontFamily: body, fontSize: 19, color: COLORS.gold, letterSpacing: 2.2 }}>
                FEUILLE D’APPEL
              </div>
              <div
                style={{
                  fontFamily: display,
                  fontSize: 38,
                  fontWeight: 600,
                  color: COLORS.textOnDark,
                  marginTop: 8,
                }}
              >
                6e A — Mathématiques
              </div>
            </div>

            <div style={{ padding: '20px 20px 0' }}>
              {ELEVES.map((nom, index) => {
                const tick = spring({ frame, fps, delay: 44 + index * 14, config: SNAPPY });
                return (
                  <div
                    key={nom}
                    style={{
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'space-between',
                      padding: '22px 20px',
                      marginBottom: 12,
                      borderRadius: 16,
                      backgroundColor: `rgba(15,92,76,${0.06 + tick * 0.08})`,
                      border: '1px solid rgba(15,92,76,0.12)',
                    }}
                  >
                    <span style={{ fontFamily: body, fontSize: 25, color: COLORS.text, fontWeight: 500 }}>
                      {nom}
                    </span>
                    <Check progress={tick} />
                  </div>
                );
              })}
            </div>

            {/* Chronomètre */}
            <div
              style={{
                margin: '18px 20px 0',
                padding: '20px',
                borderRadius: 16,
                backgroundColor: COLORS.primary,
                textAlign: 'center',
              }}
            >
              <span style={{ fontFamily: display, fontSize: 34, fontWeight: 700, color: '#ffffff' }}>
                {seconds} s
              </span>
            </div>
          </div>
        </div>
      </AbsoluteFill>
    </Stage>
  );
};
