import React from 'react';
import { AbsoluteFill } from 'remotion';
import { COLORS } from '../theme';
import { body } from '../fonts';

/**
 * Fond commun à toutes les scènes sombres. La lueur radiale décentrée évite l’aplat mort
 * d’un fond uni sans rien coûter en lisibilité : le texte reste sur la zone la plus dense.
 */
export const Stage: React.FC<{
  children: React.ReactNode;
  tone?: 'dark' | 'light';
}> = ({ children, tone = 'dark' }) => {
  const dark = tone === 'dark';
  return (
    <AbsoluteFill
      style={{
        backgroundColor: dark ? COLORS.ink : COLORS.paper,
        fontFamily: body,
        color: dark ? COLORS.textOnDark : COLORS.text,
      }}
    >
      {dark ? (
        <AbsoluteFill
          style={{
            background:
              'radial-gradient(900px 520px at 78% 8%, rgba(21,128,106,0.42), transparent 62%),' +
              'radial-gradient(700px 480px at 12% 96%, rgba(201,162,39,0.14), transparent 60%)',
          }}
        />
      ) : null}
      {children}
    </AbsoluteFill>
  );
};
