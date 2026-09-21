import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatIconModule } from '@angular/material/icon';
import { extractErrorMessage } from '../core/http-error.util';
import { Conversation, Message, MessagingService, StaffUser } from './messaging.service';

@Component({
  selector: 'app-messaging-page',
  imports: [FormsModule, MatFormFieldModule, MatInputModule, MatSelectModule, MatButtonModule, MatIconModule],
  template: `
    <div class="page-header">
      <h1><mat-icon class="page-icon" aria-hidden="true">forum</mat-icon>Messagerie</h1>
    </div>
    <div class="filters-row">
      <mat-form-field appearance="outline">
        <mat-label>Titre</mat-label>
        <input matInput [(ngModel)]="title" />
      </mat-form-field>
      <mat-form-field appearance="outline">
        <mat-label>Destinataires</mat-label>
        <mat-select [(ngModel)]="participantIds" multiple>
          @for (user of users(); track user.id) {
            <mat-option [value]="user.id">{{ user.firstName }} {{ user.lastName }}</mat-option>
          }
        </mat-select>
      </mat-form-field>
      <button mat-stroked-button (click)="create(false)"><mat-icon>mail</mat-icon> Nouvelle conversation</button>
      <button mat-flat-button (click)="create(true)"><mat-icon>campaign</mat-icon> Annonce</button>
    </div>
    @if (errorMessage()) {
      <p class="flash-error">{{ errorMessage() }}</p>
    }
    <div class="filters-row">
      <div>
        @for (conversation of conversations(); track conversation.id) {
          <p>
            <button mat-button (click)="open(conversation)">
              {{ conversation.announcement ? 'campaign' : 'chat' }}
              {{ conversation.title || 'Conversation' }}
              @if (conversation.unreadCount > 0) {
                ({{ conversation.unreadCount }})
              }
            </button>
          </p>
        }
      </div>
      <div style="flex:1">
        @if (active()) {
          @for (message of messages(); track message.id) {
            <p><strong>#{{ message.senderId }}</strong> — {{ message.content }}</p>
          }
          <mat-form-field appearance="outline">
            <mat-label>Message</mat-label>
            <input matInput [(ngModel)]="draft" />
          </mat-form-field>
          <button mat-flat-button (click)="send()">Envoyer</button>
        }
      </div>
    </div>
  `,
})
export class MessagingPage {
  private readonly messagingService = inject(MessagingService);

  protected readonly conversations = signal<Conversation[]>([]);
  protected readonly messages = signal<Message[]>([]);
  protected readonly users = signal<StaffUser[]>([]);
  protected readonly active = signal<Conversation | null>(null);
  protected readonly errorMessage = signal<string | null>(null);
  protected title = '';
  protected participantIds: number[] = [];
  protected draft = '';

  constructor() {
    void this.refresh();
    void this.messagingService
      .listUsers()
      .then((users) => this.users.set(users))
      .catch(() => undefined);
  }

  async refresh(): Promise<void> {
    this.conversations.set(await this.messagingService.list());
  }

  async create(announcement: boolean): Promise<void> {
    try {
      await this.messagingService.create(this.title, announcement, this.participantIds);
      this.title = '';
      await this.refresh();
    } catch (error) {
      this.errorMessage.set(extractErrorMessage(error));
    }
  }

  async open(conversation: Conversation): Promise<void> {
    this.active.set(conversation);
    this.messages.set(await this.messagingService.messages(conversation.id));
    await this.messagingService.markRead(conversation.id);
    await this.refresh();
  }

  async send(): Promise<void> {
    const conversation = this.active();
    if (!conversation || !this.draft.trim()) {
      return;
    }
    await this.messagingService.send(conversation.id, this.draft);
    this.draft = '';
    this.messages.set(await this.messagingService.messages(conversation.id));
  }
}
