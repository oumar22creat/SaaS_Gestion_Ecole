// Choisit une couleur de texte (noir ou blanc) lisible sur le fond donné.
// Approximation simple (luminance perçue) : suffisant pour un choix binaire noir/blanc,
// pas une implémentation complète de l'algorithme de contraste WCAG.
export function contrastColor(hex: string): string {
  const rgb = parseHexColor(hex);
  if (!rgb) {
    return '#ffffff';
  }
  const luminance = (0.299 * rgb.r + 0.587 * rgb.g + 0.114 * rgb.b) / 255;
  return luminance > 0.6 ? '#000000' : '#ffffff';
}

function parseHexColor(hex: string): { r: number; g: number; b: number } | null {
  const match = /^#?([0-9a-f]{6})$/i.exec(hex.trim());
  if (!match) {
    return null;
  }
  const value = parseInt(match[1], 16);
  return {
    r: (value >> 16) & 255,
    g: (value >> 8) & 255,
    b: value & 255,
  };
}
