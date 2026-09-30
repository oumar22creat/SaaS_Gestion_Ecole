import React from 'react';

export type GlyphName =
  | 'book'
  | 'calendar'
  | 'chat'
  | 'money'
  | 'bus'
  | 'chart'
  | 'phone'
  | 'shield'
  | 'clock';

/** Tracés au trait, dessinés à la main : aucune police d’icônes à charger au rendu. */
const PATHS: Record<GlyphName, React.ReactNode> = {
  book: (
    <>
      <path d="M6 5h9a4 4 0 0 1 4 4v14a4 4 0 0 0-4-4H6z" />
      <path d="M34 5h-9a4 4 0 0 0-4 4v14a4 4 0 0 1 4-4h9z" />
    </>
  ),
  calendar: (
    <>
      <rect x="5" y="8" width="30" height="27" rx="4" />
      <path d="M5 17h30M13 4v8M27 4v8" />
      <path d="M14 26l4 4 8-8" />
    </>
  ),
  chat: (
    <>
      <path d="M6 9h28a3 3 0 0 1 3 3v13a3 3 0 0 1-3 3H17l-8 7v-7H6a3 3 0 0 1-3-3V12a3 3 0 0 1 3-3z" />
      <path d="M12 16h16M12 22h10" />
    </>
  ),
  money: (
    <>
      <rect x="3" y="10" width="34" height="20" rx="3" />
      <circle cx="20" cy="20" r="5" />
      <path d="M9 20h.02M31 20h.02" />
    </>
  ),
  bus: (
    <>
      <rect x="6" y="7" width="28" height="22" rx="4" />
      <path d="M6 20h28M14 7v13M26 7v13" />
      <circle cx="13" cy="33" r="3" />
      <circle cx="27" cy="33" r="3" />
    </>
  ),
  chart: (
    <>
      <path d="M5 34h30" />
      <rect x="9" y="20" width="6" height="14" rx="2" />
      <rect x="18" y="12" width="6" height="22" rx="2" />
      <rect x="27" y="24" width="6" height="10" rx="2" />
    </>
  ),
  phone: (
    <>
      <rect x="11" y="3" width="18" height="34" rx="4" />
      <path d="M17 7h6" />
    </>
  ),
  clock: (
    <>
      <circle cx="20" cy="20" r="15" />
      <path d="M20 11v9l6 4" />
    </>
  ),
  shield: (
    <>
      <path d="M20 4l13 5v11c0 8-5.5 14-13 16-7.5-2-13-8-13-16V9z" />
      <path d="M14 20l4.5 4.5L27 16" />
    </>
  ),
};

export const Glyph: React.FC<{ name: GlyphName; size?: number; color: string; strokeWidth?: number }> = ({
  name,
  size = 40,
  color,
  strokeWidth = 2.2,
}) => (
  <svg
    width={size}
    height={size}
    viewBox="0 0 40 40"
    fill="none"
    stroke={color}
    strokeWidth={strokeWidth}
    strokeLinecap="round"
    strokeLinejoin="round"
  >
    {PATHS[name]}
  </svg>
);
