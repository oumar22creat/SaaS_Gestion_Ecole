import { Component, effect, input, model, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';

/**
 * Champ de recherche des écrans de liste.
 *
 * <p>La frappe n'est pas envoyée telle quelle : chaque caractère déclencherait une requête, et
 * sur une connexion lente les réponses reviendraient dans le désordre — le tableau afficherait
 * alors le résultat d'une recherche abandonnée. Le terme n'est transmis qu'après une pause de
 * saisie.
 */
@Component({
  selector: 'app-list-search',
  imports: [FormsModule, MatFormFieldModule, MatInputModule, MatIconModule, MatButtonModule],
  template: `
    <mat-form-field appearance="outline" class="list-search">
      <mat-label>{{ label() }}</mat-label>
      <mat-icon matPrefix aria-hidden="true">search</mat-icon>
      <input matInput [ngModel]="draft()" (ngModelChange)="onType($event)" [attr.aria-label]="label()" />
      @if (draft() !== '') {
        <button matSuffix mat-icon-button type="button" aria-label="Effacer la recherche" (click)="clear()">
          <mat-icon>close</mat-icon>
        </button>
      }
      <mat-hint>{{ hint() }}</mat-hint>
    </mat-form-field>
  `,
  styles: `
    .list-search {
      width: 100%;
      max-width: 420px;
    }
  `,
})
export class ListSearchComponent {
  readonly label = input('Rechercher');
  readonly hint = input('');

  /** Terme effectivement appliqué au filtre, publié après la pause de saisie. */
  readonly term = model('');

  protected readonly draft = signal('');
  private timer: ReturnType<typeof setTimeout> | null = null;

  constructor() {
    // Garde le champ aligné si le parent réinitialise le terme (changement d'onglet, reset).
    effect(() => {
      const applied = this.term();
      if (this.timer === null) {
        this.draft.set(applied);
      }
    });
  }

  protected onType(value: string): void {
    this.draft.set(value);
    if (this.timer !== null) {
      clearTimeout(this.timer);
    }
    this.timer = setTimeout(() => {
      this.timer = null;
      this.term.set(value);
    }, 300);
  }

  protected clear(): void {
    if (this.timer !== null) {
      clearTimeout(this.timer);
      this.timer = null;
    }
    this.draft.set('');
    this.term.set('');
  }
}
