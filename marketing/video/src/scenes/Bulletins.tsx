import React from 'react';
import { AbsoluteFill, interpolate, spring, useCurrentFrame, useVideoConfig } from 'remotion';
import { Stage } from '../components/Stage';
import { Kicker } from '../components/Kicker';
import { COLORS, SMOOTH, SNAPPY } from '../theme';
import { display, body } from '../fonts';
import { usePortrait } from '../format';

const LIGNES = [
  { matiere: 'Mathématiques', note: '15,5', appreciation: 'Très bon trimestre' },
  { matiere: 'Français', note: '13,0', appreciation: 'Des progrès à l’écrit' },
  { matiere: 'Histoire-Géographie', note: '14,5', appreciation: 'Participation régulière' },
  { matiere: 'Sciences', note: '16,0', appreciation: 'Excellent niveau' },
  { matiere: 'Anglais', note: '12,5', appreciation: 'Peut mieux faire à l’oral' },
];

/**
 * Le bulletin se compose ligne à ligne sous les yeux du spectateur : c’est le seul moyen de
 * montrer qu’il sort prêt, sans ressaisie. Un PDF affiché d’un coup ne dirait rien.
 */
export const Bulletins: React.FC = () => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();
  const portrait = usePortrait();

  const textIn = spring({ frame, fps, delay: 6, config: SMOOTH, durationInFrames: 26 });
  const pageIn = spring({ frame, fps, delay: 10, config: { damping: 24, stiffness: 110 } });
  const stamp = spring({ frame, fps, delay: 108, config: { damping: 11, stiffness: 150 } });

  return (
    <Stage>
      <AbsoluteFill
        style={{
          // En 9:16 le texte passe au-dessus : on annonce, puis on montre. L’ordre inverse
          // du 16:9, où l'œil balaie de gauche à droite.
          flexDirection: portrait ? 'column-reverse' : 'row',
          alignItems: 'center',
          justifyContent: 'center',
          gap: portrait ? 46 : 110,
          padding: portrait ? '0 60px' : 110,
        }}
      >
        {/* Page de bulletin */}
        <div
          style={{
            width: portrait ? 800 : 700,
            height: portrait ? 940 : 880,
            borderRadius: 10,
            backgroundColor: COLORS.surface,
            boxShadow: '0 40px 90px rgba(0,0,0,0.42)',
            padding: portrait ? 46 : 52,
            opacity: pageIn,
            transform: `translateY(${(1 - pageIn) * 40}px) rotate(${interpolate(pageIn, [0, 1], [-3, -1.2])}deg)`,
          }}
        >
          {/* En-tête branché sur l’établissement — logo, nom, mentions. */}
          <div
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: 20,
              paddingBottom: 26,
              borderBottom: `2px solid ${COLORS.primary}`,
            }}
          >
            <div style={{ width: 62, height: 62, borderRadius: 14, backgroundColor: COLORS.primary }} />
            <div>
              <div style={{ fontFamily: display, fontSize: 30, fontWeight: 700, color: COLORS.text }}>
                Lycée Moderne de Bamako
              </div>
              <div style={{ fontFamily: body, fontSize: 19, color: COLORS.muted, marginTop: 4 }}>
                Bulletin du 1<sup>er</sup> trimestre — 6e A
              </div>
            </div>
          </div>

          {LIGNES.map((ligne, index) => {
            const enter = spring({ frame, fps, delay: 34 + index * 11, config: SNAPPY });
            return (
              <div
                key={ligne.matiere}
                style={{
                  display: 'flex',
                  alignItems: 'baseline',
                  justifyContent: 'space-between',
                  padding: '22px 0',
                  borderBottom: '1px solid #e8e8e4',
                  opacity: enter,
                  transform: `translateX(${(1 - enter) * 22}px)`,
                }}
              >
                <div>
                  <div style={{ fontFamily: body, fontSize: 25, fontWeight: 600, color: COLORS.text }}>
                    {ligne.matiere}
                  </div>
                  <div style={{ fontFamily: body, fontSize: 19, color: COLORS.muted, marginTop: 4 }}>
                    {ligne.appreciation}
                  </div>
                </div>
                <div style={{ fontFamily: display, fontSize: 32, fontWeight: 700, color: COLORS.primary }}>
                  {ligne.note}
                </div>
              </div>
            );
          })}

          {/* Moyenne et rang : ce que les parents lisent en premier. */}
          <div
            style={{
              marginTop: 28,
              padding: '24px 28px',
              borderRadius: 14,
              backgroundColor: 'rgba(15,92,76,0.08)',
              display: 'flex',
              justifyContent: 'space-between',
              alignItems: 'center',
              opacity: spring({ frame, fps, delay: 92, config: SMOOTH, durationInFrames: 22 }),
            }}
          >
            <span style={{ fontFamily: body, fontSize: 24, color: COLORS.text, fontWeight: 600 }}>
              Moyenne générale
            </span>
            <span style={{ fontFamily: display, fontSize: 40, fontWeight: 700, color: COLORS.primary }}>
              14,30 <span style={{ fontSize: 22, color: COLORS.muted }}>· 4e / 32</span>
            </span>
          </div>
        </div>

        <div
          style={{
            width: portrait ? 960 : 720,
            textAlign: portrait ? 'center' : 'left',
            opacity: textIn,
            transform: portrait
              ? `translateY(${(1 - textIn) * -24}px)`
              : `translateX(${(1 - textIn) * 30}px)`,
          }}
        >
          <div style={{ marginBottom: 24 }}>
            <Kicker>Bulletins</Kicker>
          </div>
          <div
            style={{
              fontFamily: display,
              fontSize: portrait ? 68 : 84,
              fontWeight: 700,
              letterSpacing: -2,
              lineHeight: 1.06,
            }}
          >
            Ils arrivent prêts, pas à refaire
            <span style={{ color: COLORS.gold }}>.</span>
          </div>
          <div
            style={{
              fontFamily: body,
              fontSize: portrait ? 27 : 32,
              lineHeight: 1.5,
              color: COLORS.mutedOnDark,
              marginTop: portrait ? 22 : 34,
            }}
          >
            Export PDF à l’en-tête de votre établissement : logo, mentions légales, moyennes et
            rangs calculés. Aucune ressaisie entre le carnet de notes et le bulletin.
          </div>

          <div
            style={{
              display: 'inline-flex',
              alignItems: 'center',
              gap: 14,
              marginTop: portrait ? 28 : 40,
              padding: '16px 28px',
              borderRadius: 999,
              backgroundColor: 'rgba(201,162,39,0.16)',
              border: `1px solid ${COLORS.gold}`,
              opacity: stamp,
              transform: `scale(${0.9 + stamp * 0.1})`,
            }}
          >
            <span style={{ fontFamily: body, fontSize: 27, fontWeight: 600, color: COLORS.gold }}>
              Trois semaines de travail en moins
            </span>
          </div>
        </div>
      </AbsoluteFill>
    </Stage>
  );
};
