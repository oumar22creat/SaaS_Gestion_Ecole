import React from 'react';
import { AbsoluteFill, interpolate, spring, useCurrentFrame, useVideoConfig } from 'remotion';
import { Stage } from '../components/Stage';
import { Kicker } from '../components/Kicker';
import { COLORS, SMOOTH } from '../theme';
import { display, body } from '../fonts';

/**
 * Grille reprise de la migration V61 et de la section Tarifs du site : les deux doivent dire
 * la même chose, sinon l’établissement doute du reste.
 */
const PLANS = [
  { nom: 'Essentiel', effectif: "Jusqu’à 150 élèves", annuel: 180000, mensuel: '15 000 FCFA / mois', highlight: false },
  { nom: 'Standard', effectif: "Jusqu’à 800 élèves", annuel: 750000, mensuel: '62 500 FCFA / mois', highlight: true },
  { nom: 'Premium', effectif: 'Effectif illimité', annuel: 1800000, mensuel: 'Tarif dégressif', highlight: false, des: true },
];

/** Espace fine insécable à la française, sans dépendre de Intl au rendu. */
const francs = (valeur: number) => valeur.toLocaleString('fr-FR').replace(/ | /g, ' ');

export const Tarifs: React.FC = () => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();

  const titleIn = spring({ frame, fps, config: SMOOTH, durationInFrames: 24 });

  return (
    <Stage>
      <AbsoluteFill style={{ alignItems: 'center', justifyContent: 'center', padding: 90 }}>
        <div style={{ opacity: titleIn, marginBottom: 22 }}>
          <Kicker>Abonnement</Kicker>
        </div>
        <div
          style={{
            fontFamily: display,
            fontSize: 76,
            fontWeight: 700,
            letterSpacing: -1.5,
            marginBottom: 60,
            opacity: titleIn,
            transform: `translateY(${(1 - titleIn) * 20}px)`,
          }}
        >
          Un plan par taille d’établissement
        </div>

        <div style={{ display: 'flex', gap: 30, alignItems: 'stretch' }}>
          {PLANS.map((plan, index) => {
            const enter = spring({ frame, fps, delay: 16 + index * 10, config: { damping: 22, stiffness: 150 } });
            // Le montant se compose comme un compteur : le prix devient l’événement de la scène.
            const montant = Math.round(
              interpolate(enter, [0.15, 1], [0, plan.annuel], {
                extrapolateLeft: 'clamp',
                extrapolateRight: 'clamp',
              }) / 1000,
            ) * 1000;

            return (
              <div
                key={plan.nom}
                style={{
                  width: 470,
                  padding: '46px 42px',
                  borderRadius: 26,
                  backgroundColor: plan.highlight ? 'rgba(21,128,106,0.26)' : 'rgba(255,255,255,0.05)',
                  border: `1px solid ${plan.highlight ? COLORS.gold : COLORS.border}`,
                  opacity: enter,
                  transform: `translateY(${(1 - enter) * 40}px)`,
                }}
              >
                <div style={{ fontFamily: display, fontSize: 46, fontWeight: 700 }}>{plan.nom}</div>
                <div style={{ fontFamily: body, fontSize: 26, color: COLORS.mutedOnDark, marginTop: 8 }}>
                  {plan.effectif}
                </div>

                <div style={{ marginTop: 40, display: 'flex', alignItems: 'baseline', gap: 10 }}>
                  {plan.des ? (
                    <span style={{ fontFamily: body, fontSize: 26, color: COLORS.mutedOnDark }}>dès</span>
                  ) : null}
                  <span
                    style={{
                      fontFamily: display,
                      fontSize: 62,
                      fontWeight: 700,
                      color: plan.highlight ? COLORS.gold : COLORS.textOnDark,
                      fontVariantNumeric: 'tabular-nums',
                    }}
                  >
                    {francs(montant)}
                  </span>
                </div>
                <div style={{ fontFamily: body, fontSize: 25, color: COLORS.mutedOnDark, marginTop: 6 }}>
                  FCFA par an
                </div>

                <div
                  style={{
                    marginTop: 30,
                    paddingTop: 22,
                    borderTop: `1px solid ${COLORS.border}`,
                    fontFamily: body,
                    fontSize: 24,
                    color: COLORS.mutedOnDark,
                  }}
                >
                  {plan.mensuel}
                </div>
              </div>
            );
          })}
        </div>

        <div
          style={{
            fontFamily: body,
            fontSize: 28,
            color: COLORS.mutedOnDark,
            marginTop: 52,
            opacity: spring({ frame, fps, delay: 66, config: SMOOTH, durationInFrames: 22 }),
          }}
        >
          30 jours d’essai complet, sans engagement.
        </div>
      </AbsoluteFill>
    </Stage>
  );
};
