import React from 'react';
import { AbsoluteFill, interpolate, spring, useCurrentFrame, useVideoConfig } from 'remotion';
import { Stage } from '../components/Stage';
import { Mark } from '../components/Mark';
import { COLORS, SMOOTH } from '../theme';
import { display, body } from '../fonts';

const CHIFFRES = [
  { valeur: '20', libelle: 'modules livrés' },
  { valeur: '8', libelle: 'rôles distincts' },
  { valeur: '30 j', libelle: "d’essai complet" },
];

/**
 * Clôture : ce qu’on retient et ce qu’on fait ensuite. Le numéro WhatsApp est le seul moyen
 * de souscrire aujourd’hui — le règlement se fait en espèces, il doit donc être lisible et
 * rester à l’écran assez longtemps pour être noté.
 */
export const Cloture: React.FC = () => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();

  const markIn = spring({ frame, fps, config: { damping: 16, stiffness: 130 } });
  const titleIn = spring({ frame, fps, delay: 12, config: SMOOTH, durationInFrames: 26 });
  const ctaIn = spring({ frame, fps, delay: 38, config: { damping: 18, stiffness: 160 } });
  const contactIn = spring({ frame, fps, delay: 50, config: SMOOTH, durationInFrames: 24 });

  return (
    <Stage>
      <AbsoluteFill style={{ alignItems: 'center', justifyContent: 'center', padding: '0 110px' }}>
        <div style={{ transform: `scale(${markIn})`, marginBottom: 36 }}>
          <Mark size={104} />
        </div>

        <div
          style={{
            fontFamily: display,
            fontSize: 92,
            fontWeight: 700,
            letterSpacing: -2,
            textAlign: 'center',
            opacity: titleIn,
            transform: `translateY(${(1 - titleIn) * 24}px)`,
          }}
        >
          Toute votre école, dans une seule plateforme
          <span style={{ color: COLORS.gold }}>.</span>
        </div>

        {/* Les chiffres du site, en bandeau : ce qui reste en mémoire après la vidéo. */}
        <div style={{ display: 'flex', gap: 72, marginTop: 52 }}>
          {CHIFFRES.map((chiffre, index) => (
            <div
              key={chiffre.libelle}
              style={{
                textAlign: 'center',
                opacity: spring({ frame, fps, delay: 26 + index * 6, config: SMOOTH, durationInFrames: 22 }),
              }}
            >
              <div style={{ fontFamily: display, fontSize: 64, fontWeight: 700, color: COLORS.gold }}>
                {chiffre.valeur}
              </div>
              <div style={{ fontFamily: body, fontSize: 25, color: COLORS.mutedOnDark, marginTop: 4 }}>
                {chiffre.libelle}
              </div>
            </div>
          ))}
        </div>

        <div
          style={{
            marginTop: 58,
            padding: '26px 56px',
            borderRadius: 999,
            backgroundColor: COLORS.gold,
            color: COLORS.inkDeep,
            fontFamily: display,
            fontSize: 38,
            fontWeight: 700,
            opacity: ctaIn,
            transform: `scale(${0.92 + ctaIn * 0.08})`,
          }}
        >
          Démarrer l’essai gratuit
        </div>

        <div
          style={{
            display: 'flex',
            alignItems: 'center',
            gap: 16,
            marginTop: 34,
            opacity: contactIn,
            transform: `translateY(${(1 - contactIn) * 16}px)`,
          }}
        >
          <svg width="34" height="34" viewBox="0 0 32 32" fill={COLORS.textOnDark}>
            <path d="M16 3C9.4 3 4 8.4 4 15c0 2.1.6 4.1 1.6 5.9L4 29l8.3-1.5c1.7.9 3.6 1.4 5.7 1.4 6.6 0 12-5.4 12-12S22.6 3 16 3zm0 21.8c-1.8 0-3.5-.5-5-1.4l-.4-.2-4.4.8.8-4.3-.2-.4c-1-1.6-1.5-3.4-1.5-5.3 0-5.4 4.4-9.8 9.8-9.8s9.8 4.4 9.8 9.8-4.5 10.8-8.9 10.8z" />
            <path d="M21.4 18.1c-.3-.2-1.8-.9-2-1-.3-.1-.5-.2-.7.2s-.8 1-.9 1.2c-.2.2-.3.2-.6.1-.3-.2-1.3-.5-2.4-1.5-.9-.8-1.5-1.8-1.7-2.1-.2-.3 0-.5.1-.6l.5-.5c.1-.2.2-.3.3-.5 0-.2 0-.4 0-.5 0-.2-.7-1.6-.9-2.2-.2-.6-.5-.5-.7-.5h-.6c-.2 0-.5.1-.8.4-.3.3-1 1-1 2.5s1.1 2.9 1.2 3.1c.2.2 2.1 3.2 5.1 4.5.7.3 1.3.5 1.7.6.7.2 1.4.2 1.9.1.6-.1 1.8-.7 2-1.5.2-.7.2-1.4.2-1.5-.1-.2-.3-.2-.6-.3z" />
          </svg>
          <span style={{ fontFamily: body, fontSize: 34, fontWeight: 600, letterSpacing: 0.5 }}>
            +223 79 82 79 79
          </span>
        </div>

        {/* Léger resserrement final : l’image se stabilise sur le contact. */}
        <AbsoluteFill
          style={{
            pointerEvents: 'none',
            backgroundColor: COLORS.inkDeep,
            opacity: interpolate(frame, [130, 150], [0, 1], {
              extrapolateLeft: 'clamp',
              extrapolateRight: 'clamp',
            }),
          }}
        />
      </AbsoluteFill>
    </Stage>
  );
};
