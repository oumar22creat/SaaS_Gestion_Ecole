import { currencyLabel, formatMoney } from './money.util';

describe('formatMoney', () => {
  it('affiche FCFA pour la devise XOF par défaut', () => {
    expect(formatMoney(150000).replace(/\s/g, ' ')).toBe('150 000 FCFA');
  });

  it('affiche FCFA quand le code XOF est fourni explicitement', () => {
    expect(formatMoney(2500, 'XOF')).toMatch(/FCFA$/);
    expect(formatMoney(2500, 'XOF')).not.toContain('XOF');
  });

  it('laisse les autres codes devise inchangés', () => {
    expect(formatMoney(100, 'EUR')).toMatch(/EUR$/);
    expect(currencyLabel('xof')).toBe('FCFA');
  });
});
