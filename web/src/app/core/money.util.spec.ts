import { currencyLabel, formatMoney } from './money.util';

describe('formatMoney', () => {
  // Intl insère une espace insécable (fine) comme séparateur de milliers : on la normalise.
  const normalize = (value: string) => value.replace(/[  ]/g, ' ');

  it('affiche FCFA par défaut, jamais le code ISO XOF', () => {
    expect(normalize(formatMoney(150000))).toBe('150 000 FCFA');
  });

  it('traduit le code XOF renvoyé par l API en FCFA', () => {
    expect(normalize(formatMoney(2500, 'XOF'))).toBe('2 500 FCFA');
  });

  it('affiche FCFA sans erreur quand la devise est absente', () => {
    expect(normalize(formatMoney(2500, null))).toBe('2 500 FCFA');
    expect(normalize(formatMoney(2500, undefined))).toBe('2 500 FCFA');
    expect(normalize(formatMoney(2500, ''))).toBe('2 500 FCFA');
  });

  it('laisse une autre devise telle quelle', () => {
    expect(normalize(formatMoney(10, 'EUR'))).toBe('10 EUR');
  });
});

describe('currencyLabel', () => {
  it('accepte un code en minuscules et une devise nulle', () => {
    expect(currencyLabel('xof')).toBe('FCFA');
    expect(currencyLabel(null)).toBe('FCFA');
  });
});
