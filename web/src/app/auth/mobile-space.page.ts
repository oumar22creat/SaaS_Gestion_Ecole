import { Component } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'app-mobile-space-page',
  imports: [MatCardModule, MatIconModule],
  template: `
    <div class="page-header">
      <h1><mat-icon class="page-icon" aria-hidden="true">phone_iphone</mat-icon>Application mobile</h1>
    </div>
    <mat-card>
      <p class="lede">
        Le suivi parent et élève (notes, absences, emploi du temps) se fait depuis
        l'application mobile, pas depuis cette console établissement.
      </p>
    </mat-card>
  `,
})
export class MobileSpacePage {}
