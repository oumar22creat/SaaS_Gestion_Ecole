import { currencyLabel, formatMoney } from './money.util';

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

  it('affiche FCFA quand la devise est absente', () => {
    expect(normalize(formatMoney(5000, null))).toBe('5 000 FCFA');
    expect(normalize(formatMoney(5000, ''))).toBe('5 000 FCFA');
  });

  it('laisse une autre devise telle quelle, en majuscules', () => {
    expect(normalize(formatMoney(10, 'EUR'))).toBe('10 EUR');
    expect(normalize(formatMoney(10, 'eur'))).toBe('10 EUR');
  });
});

describe('currencyLabel', () => {
  it('traduit XOF en FCFA quelle que soit la casse', () => {
    expect(currencyLabel('XOF')).toBe('FCFA');
    expect(currencyLabel('xof')).toBe('FCFA');
    expect(currencyLabel('Xof')).toBe('FCFA');
  });

  it('retombe sur FCFA pour null, undefined, vide ou blanc', () => {
    expect(currencyLabel(null)).toBe('FCFA');
    expect(currencyLabel(undefined)).toBe('FCFA');
    expect(currencyLabel('')).toBe('FCFA');
    expect(currencyLabel('  ')).toBe('FCFA');
  });

  it('met les autres codes en majuscules', () => {
    expect(currencyLabel('usd')).toBe('USD');
    expect(currencyLabel('EUR')).toBe('EUR');
  });
});
