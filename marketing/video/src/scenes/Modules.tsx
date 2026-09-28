import React from 'react';
import { AbsoluteFill, spring, useCurrentFrame, useVideoConfig } from 'remotion';
import { Stage } from '../components/Stage';
import { Kicker } from '../components/Kicker';
import { Glyph, GlyphName } from '../components/Glyph';
import { COLORS, SMOOTH, SNAPPY } from '../theme';
import { display, body } from '../fonts';

/** Les six familles de modules, reprises mot pour mot de la section « Les modules » du site. */
const MODULES: { icon: GlyphName; title: string; detail: string }[] = [
  { icon: 'book', title: 'Scolarité et bulletins', detail: 'Inscriptions, notes, moyennes, rangs' },
  { icon: 'calendar', title: 'Absences et emploi du temps', detail: 'Appel du jour, retards, séances' },
  { icon: 'chat', title: 'Communication aux familles', detail: 'Messages, notifications, SMS' },
  { icon: 'money', title: 'Frais de scolarité', detail: 'Échéanciers, reçus, impayés' },
  { icon: 'bus', title: 'Cantine, transport, bibliothèque', detail: 'Inscriptions et suivi par élève' },
  { icon: 'chart', title: 'Pilotage de direction', detail: 'Effectifs, présence, résultats' },
];

export const Modules: React.FC = () => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();

  const titleIn = spring({ frame, fps, config: SMOOTH, durationInFrames: 24 });

  return (
    <Stage>
      <AbsoluteFill style={{ alignItems: 'center', justifyContent: 'center', padding: 90 }}>
        <div style={{ opacity: titleIn, textAlign: 'center', marginBottom: 20 }}>
          <Kicker>Les modules</Kicker>
        </div>
        <div
          style={{
            fontFamily: display,
            fontSize: 72,
            fontWeight: 700,
            letterSpacing: -1.5,
            marginBottom: 64,
            opacity: titleIn,
            transform: `translateY(${(1 - titleIn) * 20}px)`,
          }}
        >
          Tout ce qu’un établissement gère vraiment
        </div>

        <div
          style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(3, 1fr)',
            gap: 28,
            width: 1620,
          }}
        >
          {MODULES.map((module, index) => {
            // Cascade diagonale : la lecture suit l’ordre naturel de la grille.
            const enter = spring({ frame, fps, delay: 14 + index * 7, config: SNAPPY });

            return (
              <div
                key={module.title}
                style={{
                  padding: '34px 34px 38px',
                  borderRadius: 22,
                  backgroundColor: 'rgba(255,255,255,0.055)',
                  border: `1px solid ${COLORS.border}`,
                  opacity: enter,
                  transform: `translateY(${(1 - enter) * 34}px) scale(${0.97 + enter * 0.03})`,
                }}
              >
                <div
                  style={{
                    width: 68,
                    height: 68,
                    borderRadius: 18,
                    display: 'grid',
                    placeItems: 'center',
                    backgroundColor: 'rgba(201,162,39,0.14)',
                    marginBottom: 24,
                  }}
                >
                  <Glyph name={module.icon} color={COLORS.gold} size={36} />
                </div>
                <div style={{ fontFamily: display, fontSize: 36, fontWeight: 600, lineHeight: 1.15 }}>
                  {module.title}
                </div>
                <div
                  style={{
                    fontFamily: body,
                    fontSize: 26,
                    color: COLORS.mutedOnDark,
                    marginTop: 12,
                  }}
                >
                  {module.detail}
                </div>
              </div>
            );
          })}
        </div>
      </AbsoluteFill>
    </Stage>
  );
};
