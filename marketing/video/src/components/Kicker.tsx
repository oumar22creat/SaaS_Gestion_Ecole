import React from 'react';
import { COLORS } from '../theme';
import { body } from '../fonts';

/** Intertitre en capitales, repris de la primitive `.section-label` du produit. */
export const Kicker: React.FC<{ children: React.ReactNode; onDark?: boolean }> = ({
  children,
  onDark = true,
}) => (
  <span
    style={{
      fontFamily: body,
      fontSize: 22,
      fontWeight: 600,
      letterSpacing: 3.4,
      textTransform: 'uppercase',
      color: onDark ? COLORS.gold : COLORS.primary,
    }}
  >
    {children}
  </span>
);
