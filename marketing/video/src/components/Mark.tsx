import React from 'react';
import { COLORS } from '../theme';

/**
 * Toque de diplômé, dessinée en SVG plutôt qu’importée : la vidéo ne dépend d’aucun fichier
 * binaire et reste nette à toutes les résolutions, y compris en 1080×1920.
 */
export const Mark: React.FC<{ size?: number; draw?: number }> = ({ size = 128, draw = 1 }) => (
  <svg width={size} height={size} viewBox="0 0 100 100" fill="none">
    <rect width="100" height="100" rx="26" fill={COLORS.primary} />
    <g
      style={{
        transformOrigin: '50px 52px',
        transform: `scale(${draw})`,
      }}
    >
      <path d="M50 26 L80 40 L50 54 L20 40 Z" fill={COLORS.paper} />
      <path
        d="M32 46 L32 62 C32 62 38 70 50 70 C62 70 68 62 68 62 L68 46"
        stroke={COLORS.gold}
        strokeWidth="6"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
      <path d="M78 41 L78 60" stroke={COLORS.gold} strokeWidth="5" strokeLinecap="round" />
    </g>
  </svg>
);
