import { formatMoney } from './money.util';

describe('formatMoney', () => {
  // Intl insère une espace insécable fine comme séparateur de milliers : on la normalise.
  const normalize = (value: string) => value.replace(/[  ]/g, ' ');

  it('affiche FCFA par défaut, jamais le code ISO XOF', () => {
    expect(normalize(formatMoney(125000))).toBe('125 000 FCFA');
    expect(formatMoney(125000)).not.toContain('XOF');
  });

  it('traduit le code XOF renvoyé par l API en FCFA', () => {
    expect(normalize(formatMoney(5000, 'XOF'))).toBe('5 000 FCFA');
  });

  it('laisse une autre devise telle quelle', () => {
    expect(normalize(formatMoney(10, 'EUR'))).toBe('10 EUR');
  });
});
