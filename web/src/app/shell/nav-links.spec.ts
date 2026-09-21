import { firstPathForRole, navSectionsForRole, rolesForNavPath } from './nav-links';

describe('nav-links', () => {
  it('sends an administrator to the establishment dashboard', () => {
    expect(firstPathForRole('ADMIN')).toBe('/dashboard');
    const paths = navSectionsForRole('ADMIN').flatMap((section) =>
      section.links.map((link) => link.path),
    );
    expect(paths).toContain('/students');
    expect(paths).toContain('/settings');
    expect(rolesForNavPath('/students')).toContain('ADMIN');
  });

  it('sends a Super-Admin to the platform console, not the tenant shell', () => {
    expect(firstPathForRole('SUPER_ADMIN')).toBe('/admin');
    expect(navSectionsForRole('SUPER_ADMIN')).toEqual([]);
  });

  it('sends teachers to attendance first', () => {
    expect(firstPathForRole('TEACHER')).toBe('/attendance');
  });

  it('groups staff links by section', () => {
    const sections = navSectionsForRole('ADMIN');
    expect(sections.map((section) => section.title)).toContain('Pédagogie');
    expect(sections.flatMap((section) => section.links.map((link) => link.icon))).toContain(
      'space_dashboard',
    );
  });
});
