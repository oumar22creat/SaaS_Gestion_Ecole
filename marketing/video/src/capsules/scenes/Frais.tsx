import React from 'react';
import { AbsoluteFill, interpolate, spring, useCurrentFrame, useVideoConfig } from 'remotion';
import { Stage } from '../../components/Stage';
import { COLORS, SMOOTH, SNAPPY } from '../../theme';
import { display, body } from '../../fonts';

/** Ce que le comptable regarde en premier : qui doit encore, et combien. */
const LIGNES = [
  { nom: 'Aminata Traoré', classe: '6e A', du: 0, statut: 'à jour' },
  { nom: 'Modibo Keïta', classe: '6e A', du: 45000, statut: 'en retard' },
  { nom: 'Fatoumata Sidibé', classe: '5e B', du: 0, statut: 'à jour' },
  { nom: 'Sékou Coulibaly', classe: '5e B', du: 30000, statut: 'en retard' },
  { nom: 'Awa Diarra', classe: '4e A', du: 0, statut: 'à jour' },
];

const francs = (v: number) => v.toLocaleString('fr-FR').replace(/[  ]/g, ' ');

export const Frais: React.FC = () => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();

  const titreIn = spring({ frame, fps, config: SMOOTH, durationInFrames: 22 });
  // Le total impayé se compose comme un compteur : c'est le chiffre qui fait réagir.
  const total = Math.round(
    interpolate(frame, [30, 90], [0, 75000], { extrapolateLeft: 'clamp', extrapolateRight: 'clamp' }) / 1000,
  ) * 1000;

  return (
    <Stage>
      <AbsoluteFill style={{ alignItems: 'center', justifyContent: 'center', padding: '0 50px' }}>
        <div
          style={{
            fontFamily: display,
            fontSize: 60,
            fontWeight: 700,
            letterSpacing: -1,
            textAlign: 'center',
            marginBottom: 34,
            opacity: titreIn,
          }}
        >
          Qui a payé, qui n’a pas payé
        </div>

        <div
          style={{
            width: '100%',
            borderRadius: 24,
            overflow: 'hidden',
            border: `1px solid ${COLORS.border}`,
            backgroundColor: 'rgba(255,255,255,0.05)',
          }}
        >
          {LIGNES.map((ligne, index) => {
            const enter = spring({ frame, fps, delay: 18 + index * 8, config: SNAPPY });
            const retard = ligne.du > 0;
            return (
              <div
                key={ligne.nom}
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'space-between',
                  padding: '26px 30px',
                  borderBottom: `1px solid ${COLORS.border}`,
                  backgroundColor: retard ? 'rgba(179,38,30,0.16)' : 'transparent',
                  opacity: enter,
                  transform: `translateX(${(1 - enter) * 24}px)`,
                }}
              >
                <div>
                  <div style={{ fontFamily: body, fontSize: 30, fontWeight: 600 }}>{ligne.nom}</div>
                  <div style={{ fontFamily: body, fontSize: 23, color: COLORS.mutedOnDark, marginTop: 2 }}>
                    {ligne.classe}
                  </div>
                </div>
                <div
                  style={{
                    fontFamily: display,
                    fontSize: 30,
                    fontWeight: 700,
                    color: retard ? '#ff8a80' : COLORS.mutedOnDark,
                    textAlign: 'right',
                  }}
                >
                  {retard ? francs(ligne.du) : '—'}
                  <div style={{ fontFamily: body, fontSize: 21, fontWeight: 400 }}>{ligne.statut}</div>
                </div>
              </div>
            );
          })}
        </div>

        <div
          style={{
            marginTop: 34,
            padding: '26px 40px',
            borderRadius: 20,
            border: `1px solid ${COLORS.gold}`,
            backgroundColor: 'rgba(201,162,39,0.14)',
            textAlign: 'center',
            opacity: spring({ frame, fps, delay: 40, config: SMOOTH, durationInFrames: 22 }),
          }}
        >
          <div style={{ fontFamily: body, fontSize: 25, color: COLORS.mutedOnDark }}>
            Reste à recouvrer
          </div>
          <div
            style={{
              fontFamily: display,
              fontSize: 60,
              fontWeight: 700,
              color: COLORS.gold,
              fontVariantNumeric: 'tabular-nums',
            }}
          >
            {francs(total)} <span style={{ fontSize: 30 }}>FCFA</span>
          </div>
        </div>
      </AbsoluteFill>
    </Stage>
  );
};
