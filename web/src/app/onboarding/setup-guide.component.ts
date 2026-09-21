import { Component, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { RouterLink } from '@angular/router';
import { OnboardingService, OnboardingStep } from './onboarding.service';

/** Libellés et destinations : l'API ne renvoie que des clés, elle ne connaît pas les écrans. */
const STEP_LABELS: Record<string, { title: string; hint: string; route: string; action: string }> =
  {
    CLASSES: {
      title: 'Créer vos classes',
      hint: 'Les classes structurent tout le reste : élèves, emploi du temps, appel et bulletins.',
      route: '/classes',
      action: 'Créer une classe',
    },
    SUBJECTS: {
      title: 'Déclarer les matières',
      hint: 'Avec leur coefficient : elles alimentent la saisie des notes et le calcul des moyennes.',
      route: '/subjects',
      action: 'Ajouter une matière',
    },
    STUDENTS: {
      title: 'Inscrire les élèves',
      hint: 'Un par un, ou toute une promotion depuis un fichier CSV exporté de votre tableur.',
      route: '/students',
      action: 'Inscrire des élèves',
    },
    TEACHERS: {
      title: 'Enregistrer les enseignants',
      hint: "Puis leur ouvrir un accès depuis l'écran Comptes, pour qu'ils fassent l'appel sur mobile.",
      route: '/teachers',
      action: 'Ajouter un enseignant',
    },
    TIMETABLE: {
      title: "Bâtir l'emploi du temps",
      hint: 'Classe, matière, enseignant, salle et horaire. Les conflits sont détectés à la saisie.',
      route: '/timetable',
      action: 'Créer un créneau',
    },
  };

@Component({
  selector: 'app-setup-guide',
  imports: [MatButtonModule, MatIconModule, RouterLink],
  template: `
    @if (visible()) {
      <section class="setup-guide" aria-labelledby="setup-guide-title">
        <header class="setup-head">
          <div>
            <h2 id="setup-guide-title">Mise en route de votre établissement</h2>
            <p class="setup-lead">
              {{ doneCount() }} étape{{ doneCount() > 1 ? 's' : '' }} sur {{ steps().length }}.
              Chaque étape s'appuie sur la précédente, mais vous pouvez les faire dans l'ordre qui
              vous arrange et revenir plus tard.
            </p>
          </div>
          <span class="setup-progress" aria-hidden="true">
            {{ doneCount() }}/{{ steps().length }}
          </span>
        </header>

        <ol class="setup-steps">
          @for (step of steps(); track step.key) {
            <li class="setup-step" [class.step-done]="step.done">
              <mat-icon class="step-state" aria-hidden="true">
                {{ step.done ? 'check_circle' : 'radio_button_unchecked' }}
              </mat-icon>
              <span class="step-text">
                <span class="step-title">
                  {{ labelFor(step.key).title }}
                  @if (step.count > 0) {
                    <span class="step-count">{{ step.count }}</span>
                  }
                </span>
                <span class="step-hint">{{ labelFor(step.key).hint }}</span>
              </span>
              @if (!step.done) {
                <a mat-stroked-button [routerLink]="labelFor(step.key).route">
                  {{ labelFor(step.key).action }}
                </a>
              }
            </li>
          }
        </ol>
      </section>
    }
  `,
  styles: `
    .setup-guide {
      margin-bottom: var(--space-6);
      padding: var(--space-5);
      border-radius: var(--radius);
      background: var(--color-surface);
      border: 1px solid var(--tenant-primary-ring);
      box-shadow: var(--shadow-1);
    }

    .setup-head {
      display: flex;
      align-items: flex-start;
      justify-content: space-between;
      gap: var(--space-4);
      margin-bottom: var(--space-5);
    }

    h2 {
      margin: 0 0 var(--space-2);
      font-size: var(--font-size-title-2);
      font-weight: var(--font-weight-semibold);
    }

    .setup-lead {
      margin: 0;
      max-width: 62ch;
      color: var(--color-text-secondary);
      font-size: var(--font-size-small);
    }

    .setup-progress {
      flex: 0 0 auto;
      padding: var(--space-1) var(--space-3);
      border-radius: var(--radius-pill);
      background: var(--tenant-primary-soft);
      color: var(--tenant-primary);
      font-weight: var(--font-weight-semibold);
    }

    .setup-steps {
      list-style: none;
      margin: 0;
      padding: 0;
    }

    .setup-step {
      display: flex;
      align-items: center;
      gap: var(--space-4);
      padding: var(--space-4) 0;
    }

    .setup-step + .setup-step {
      border-top: 1px solid var(--color-border);
    }

    .step-state {
      flex: 0 0 auto;
      color: var(--color-border-strong);
    }

    .step-done .step-state {
      color: var(--color-success);
    }

    .step-text {
      display: flex;
      flex-direction: column;
      flex: 1 1 auto;
      min-width: 0;
    }

    .step-title {
      display: flex;
      align-items: center;
      gap: var(--space-2);
      font-weight: var(--font-weight-semibold);
    }

    /* Une étape faite reste lisible : on l'atténue sans la barrer ni la masquer. */
    .step-done .step-title {
      color: var(--color-text-secondary);
    }

    .step-count {
      padding: 0 var(--space-2);
      border-radius: var(--radius-pill);
      background: var(--color-surface-muted);
      color: var(--color-text-secondary);
      font-size: var(--font-size-caption);
    }

    .step-hint {
      color: var(--color-text-secondary);
      font-size: var(--font-size-small);
    }

    @media (max-width: 720px) {
      .setup-step {
        flex-wrap: wrap;
      }
    }
  `,
})
export class SetupGuideComponent {
  private readonly onboardingService = inject(OnboardingService);

  protected readonly steps = signal<OnboardingStep[]>([]);
  protected readonly visible = signal(false);

  constructor() {
    void this.load();
  }

  protected doneCount(): number {
    return this.steps().filter((step) => step.done).length;
  }

  protected labelFor(key: string): { title: string; hint: string; route: string; action: string } {
    return STEP_LABELS[key] ?? { title: key, hint: '', route: '/dashboard', action: 'Ouvrir' };
  }

  private async load(): Promise<void> {
    try {
      const status = await this.onboardingService.status();
      this.steps.set(status.steps);
      // Une fois la configuration terminée, la liste disparaît : elle n'a plus rien à dire.
      this.visible.set(!status.complete);
    } catch {
      // Un rôle sans droit sur l'endpoint ne doit pas casser le tableau de bord.
      this.visible.set(false);
    }
  }
}
