import { DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { IonButton, IonButtons, IonContent, IonHeader, IonIcon, IonTitle, IonToolbar } from '@ionic/angular';
import { Exam } from './grade.model';
import { ExamService } from './exam.service';

@Component({
  selector: 'app-exam-list-page',
  imports: [DatePipe, RouterLink, IonHeader, IonToolbar, IonTitle, IonButtons, IonButton, IonContent, IonIcon],
  template: `
    <ion-header>
      <ion-toolbar>
        <ion-buttons slot="start">
          <ion-button routerLink="/home"><ion-icon aria-hidden="true" name="school-outline"></ion-icon> Accueil</ion-button>
        </ion-buttons>
        <ion-title>Notes</ion-title>
      </ion-toolbar>
    </ion-header>
    <ion-content class="ion-padding">
      <div class="screen-header">
        <ion-icon class="screen-icon" aria-hidden="true" name="create-outline"></ion-icon>
        <h1 class="screen-title">Évaluations</h1>
      </div>

      @if (exams().length > 0) {
        <p class="section-label">
          {{ exams().length }} {{ exams().length > 1 ? 'évaluations' : 'évaluation' }}
        </p>
        <div class="exam-list">
          @for (exam of exams(); track exam.id) {
            <a class="exam" [routerLink]="['/exams', exam.id, 'grades']">
              <ion-icon class="exam-icon" aria-hidden="true" name="calculator-outline"></ion-icon>
              <span class="exam-text">
                <span class="exam-label">{{ exam.label }}</span>
                <span class="exam-meta"
                  >{{ exam.examDate | date: 'dd/MM/yyyy' }} · barème /{{ exam.maxScore }}</span
                >
              </span>
              <span class="exam-chevron" aria-hidden="true">›</span>
            </a>
          }
        </div>
      } @else {
        <p class="empty-state">Aucune évaluation à saisir pour le moment.</p>
      }
    </ion-content>
  `,
  styles: `
    .exam-list {
      display: flex;
      flex-direction: column;
      gap: var(--space-2);
    }
    .exam {
      display: flex;
      align-items: center;
      gap: var(--space-3);
      min-height: 64px;
      padding: var(--space-3) var(--space-4);
      background: var(--color-surface);
      border: 1px solid var(--color-border);
      border-radius: var(--radius);
      box-shadow: var(--shadow-1);
      color: inherit;
      text-decoration: none;
    }
    .exam-icon {
      display: grid;
      place-items: center;
      flex: 0 0 auto;
      width: 36px;
      height: 36px;
      border-radius: var(--radius-sm);
      background: var(--tenant-primary-soft);
      font-size: 18px;
    }
    .exam-text {
      display: flex;
      flex: 1;
      flex-direction: column;
      gap: 2px;
      min-width: 0;
    }
    .exam-label {
      font-weight: var(--font-weight-semibold);
    }
    .exam-meta {
      color: var(--color-text-secondary);
      font-size: var(--font-size-caption);
    }
    .exam-chevron {
      color: var(--color-border-strong);
      font-size: 22px;
      line-height: 1;
    }
  `,
})
export class ExamListPage {
  private readonly examService = inject(ExamService);
  protected readonly exams = signal<Exam[]>([]);

  constructor() {
    void this.examService.list().then((exams) => this.exams.set(exams));
  }
}
