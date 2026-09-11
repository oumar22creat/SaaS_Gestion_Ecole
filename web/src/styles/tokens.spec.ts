describe('design tokens', () => {
  function tokenValue(name: string): string {
    return getComputedStyle(document.documentElement).getPropertyValue(name).trim();
  }

  it('defines a neutral fallback for the tenant brand color', () => {
    expect(tokenValue('--tenant-primary')).toBe('#3880ff');
  });

  it('maps the Material system primary color onto the tenant token', () => {
    expect(tokenValue('--mat-sys-primary')).toBe(tokenValue('--tenant-primary'));
  });

  it('defines the 4px spacing grid', () => {
    expect(tokenValue('--space-4')).toBe('16px');
  });
});
