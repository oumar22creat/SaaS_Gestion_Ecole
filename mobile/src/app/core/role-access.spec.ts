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
    // Le portail, plus le réglage des notifications, offert à tous les rôles.
    expect(tiles.map((tile) => tile.path)).toEqual(['/portal', '/notifications']);
    expect(canAccessMobilePath('PARENT', '/portal')).toBe(true);
    expect(canAccessMobilePath('PARENT', '/attendance')).toBe(false);
    expect(canAccessMobilePath('PARENT', '/home')).toBe(true);
  });

  it('sends students to their own record, without any staff capability', () => {
    const tiles = homeTilesForRole('STUDENT');
    expect(homeEyebrowForRole('STUDENT')).toBe('Espace élève');
    expect(tiles.map((tile) => tile.path)).toEqual(['/portal', '/notifications']);
    expect(tiles.map((tile) => tile.label)).not.toContain("Feuille d'appel");
    expect(canAccessMobilePath('STUDENT', '/portal')).toBe(true);
    // `/exams` est la saisie de notes du personnel, pas la consultation de l'élève.
    expect(canAccessMobilePath('STUDENT', '/exams')).toBe(false);
    expect(canAccessMobilePath('TEACHER', '/exams')).toBe(true);
    expect(canAccessMobilePath('TEACHER', '/portal')).toBe(false);
  });

  it('ne montre à chaque rôle du personnel que les tuiles que la garde laisse ouvrir', () => {
    for (const role of ['ADMIN', 'DIRECTION', 'TEACHER', 'VIE_SCOLAIRE']) {
      for (const tile of homeTilesForRole(role)) {
        expect(tile.path === null || canAccessMobilePath(role, tile.path)).withContext(`${role} ${tile.path}`).toBe(true);
      }
    }
    // La vie scolaire garde la feuille d'appel, sans notes ni emploi du temps.
    expect(homeTilesForRole('VIE_SCOLAIRE').map((tile) => tile.path)).toEqual(['/attendance', '/notifications']);
    expect(homeTilesForRole('TEACHER').map((tile) => tile.path)).toContain('/timetable');
  });
});
