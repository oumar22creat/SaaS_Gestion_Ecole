import React from 'react';
import { AbsoluteFill, spring, useCurrentFrame, useVideoConfig } from 'remotion';
import { Stage } from '../../components/Stage';
import { Glyph, GlyphName } from '../../components/Glyph';
import { COLORS, SMOOTH, SNAPPY } from '../../theme';
import { display, body } from '../../fonts';

/**
 * Le portail vu par une famille ou par un élève.
 *
 * <p>Les quatre rubriques sont celles que l'API sert réellement (PortalController : notes,
 * absences, emploi du temps, frais). Rien n'est promis ici qui n'existe dans le produit — une
 * vidéo qui annonce une fonctionnalité absente se paie au premier essai gratuit.
 */
const RUBRIQUES: { icon: GlyphName; titre: string; detail: string }[] = [
  { icon: 'book', titre: 'Les notes', detail: 'Par matière, dès la saisie' },
  { icon: 'calendar', titre: 'Les absences', detail: 'Signalées le jour même' },
  { icon: 'clock', titre: "L’emploi du temps", detail: 'La semaine en cours' },
  { icon: 'money', titre: 'Les frais', detail: 'Ce qui est réglé, ce qui reste' },
];

export const Portail: React.FC<{ titre: string; sousTitre: string; entete: string }> = ({
  titre,
  sousTitre,
  entete,
}) => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();

  const titreIn = spring({ frame, fps, config: SMOOTH, durationInFrames: 22 });
  const telIn = spring({ frame, fps, delay: 8, config: { damping: 22, stiffness: 110 } });

  return (
    <Stage>
      <AbsoluteFill style={{ alignItems: 'center', justifyContent: 'center', padding: '0 50px' }}>
        <div
          style={{
            fontFamily: display,
            fontSize: 62,
            fontWeight: 700,
            letterSpacing: -1.5,
            textAlign: 'center',
            opacity: titreIn,
          }}
        >
          {titre}
        </div>
        <div
          style={{
            fontFamily: body,
            fontSize: 30,
            color: COLORS.mutedOnDark,
            textAlign: 'center',
            marginTop: 12,
            marginBottom: 36,
            opacity: titreIn,
          }}
        >
          {sousTitre}
        </div>

        {/* Téléphone : c'est l'appareil qu'ont les familles, pas un ordinateur. */}
        <div
          style={{
            width: 460,
            borderRadius: 48,
            padding: 13,
            backgroundColor: '#050f0c',
            border: `2px solid ${COLORS.border}`,
            boxShadow: '0 30px 70px rgba(0,0,0,0.45)',
            opacity: telIn,
            transform: `translateY(${(1 - telIn) * 40}px)`,
          }}
        >
          <div style={{ borderRadius: 38, backgroundColor: COLORS.paper, overflow: 'hidden' }}>
            <div style={{ backgroundColor: COLORS.ink, padding: '26px 24px 22px' }}>
              <div style={{ fontFamily: body, fontSize: 18, color: COLORS.gold, letterSpacing: 2 }}>
                ESPACE FAMILLE
              </div>
              <div
                style={{
                  fontFamily: display,
                  fontSize: 34,
                  fontWeight: 600,
                  color: COLORS.textOnDark,
                  marginTop: 6,
                }}
              >
                {entete}
              </div>
            </div>

            <div style={{ padding: '18px 18px 24px' }}>
              {RUBRIQUES.map((rubrique, index) => {
                const enter = spring({ frame, fps, delay: 30 + index * 10, config: SNAPPY });
                return (
                  <div
                    key={rubrique.titre}
                    style={{
                      display: 'flex',
                      alignItems: 'center',
                      gap: 18,
                      padding: '20px 18px',
                      marginBottom: 10,
                      borderRadius: 16,
                      backgroundColor: 'rgba(15,92,76,0.08)',
                      border: '1px solid rgba(15,92,76,0.14)',
                      opacity: enter,
                      transform: `translateX(${(1 - enter) * 22}px)`,
                    }}
                  >
                    <Glyph name={rubrique.icon} color={COLORS.primary} size={30} />
                    <span>
                      <span
                        style={{
                          display: 'block',
                          fontFamily: body,
                          fontSize: 26,
                          fontWeight: 600,
                          color: COLORS.text,
                        }}
                      >
                        {rubrique.titre}
                      </span>
                      <span style={{ fontFamily: body, fontSize: 21, color: COLORS.muted }}>
                        {rubrique.detail}
                      </span>
                    </span>
                  </div>
                );
              })}
            </div>
          </div>
        </div>
      </AbsoluteFill>
    </Stage>
  );
};
