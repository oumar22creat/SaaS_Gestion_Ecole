import { canAccessMobilePath, homeEyebrowForRole, homeTilesForRole } from './role-access';

describe('role-access', () => {
  it('gives teachers the roll-call and grade tiles', () => {
    const labels = homeTilesForRole('TEACHER').map((tile) => tile.label);
    expect(labels).toContain("Feuille d'appel");
    expect(labels).toContain('Notes');
    expect(homeEyebrowForRole('TEACHER')).toBe('Espace enseignant');
  });

  it('sends parents to the family portal, never to staff screens', () => {
    const tiles = homeTilesForRole('PARENT');
    expect(homeEyebrowForRole('PARENT')).toBe('Espace parent');
    expect(tiles.map((tile) => tile.label)).toContain('Mes enfants');
    // Les tuiles menaient auparavant nulle part (path null) : elles sont désormais actives.
    expect(tiles.every((tile) => tile.path === '/portal')).toBe(true);
    expect(canAccessMobilePath('PARENT', '/portal')).toBe(true);
    expect(canAccessMobilePath('PARENT', '/attendance')).toBe(false);
    expect(canAccessMobilePath('PARENT', '/home')).toBe(true);
  });

  it('sends students to their own record, without any staff capability', () => {
    const tiles = homeTilesForRole('STUDENT');
    expect(homeEyebrowForRole('STUDENT')).toBe('Espace élève');
    expect(tiles.map((tile) => tile.path)).toEqual(['/portal']);
    expect(tiles.map((tile) => tile.label)).not.toContain("Feuille d'appel");
    expect(canAccessMobilePath('STUDENT', '/portal')).toBe(true);
    // `/exams` est la saisie de notes du personnel, pas la consultation de l'élève.
    expect(canAccessMobilePath('STUDENT', '/exams')).toBe(false);
    expect(canAccessMobilePath('TEACHER', '/exams')).toBe(true);
    expect(canAccessMobilePath('TEACHER', '/portal')).toBe(false);
  });
});
