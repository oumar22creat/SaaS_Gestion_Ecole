import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatIconModule } from '@angular/material/icon';
import { AuthTokenService } from '../auth/auth-token.service';
import { extractErrorMessage } from '../core/http-error.util';
import { DIRECTION_ROLES } from '../core/teaching-scope.service';
import { Conversation, Message, MessagingService, StaffUser } from './messaging.service';

@Component({
  selector: 'app-messaging-page',
  imports: [
    FormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatIconModule,
  ],
  template: `
    <div class="page-header">
      <h1><mat-icon class="page-icon" aria-hidden="true">forum</mat-icon>Messagerie</h1>
    </div>
    @if (canStartConversation) {
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
        <button mat-stroked-button (click)="create(false)">
          <mat-icon>mail</mat-icon> Nouvelle conversation
        </button>
        @if (canAnnounce) {
          <button mat-flat-button (click)="create(true)">
            <mat-icon>campaign</mat-icon> Annonce
          </button>
        }
      </div>
    } @else {
      <p class="empty-state" data-testid="no-new-conversation">
        Vous pouvez lire vos conversations et y répondre. Pour en démarrer une nouvelle,
        adressez-vous à la direction : la liste des destinataires n’est pas encore disponible
        pour votre profil.
      </p>
    }
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
            <p>
              <strong>#{{ message.senderId }}</strong> — {{ message.content }}
            </p>
          }
          @if (canReply(active()!)) {
            <mat-form-field appearance="outline">
              <mat-label>Message</mat-label>
              <input matInput [(ngModel)]="draft" />
            </mat-form-field>
            <button mat-flat-button (click)="send()">Envoyer</button>
          } @else {
            <p class="cell-muted" data-testid="announcement-read-only">
              Annonce de l’établissement : lecture seule.
            </p>
          }
        }
      </div>
    </div>
  `,
})
export class MessagingPage {
  private readonly messagingService = inject(MessagingService);
  private readonly role = inject(AuthTokenService).role();

  /**
   * Annonces : ADMIN et DIRECTION seulement (ADR-019, sinon 403 `ANNOUNCEMENT_NOT_ALLOWED`).
   * Nouvelle conversation : la liste des destinataires vient de `GET /users`, réservé à ces
   * mêmes rôles ; pour les autres membres du personnel l'écran n'aurait proposé aucun
   * destinataire. On masque donc la création (sans annuaire pour l'instant) ; répondre dans
   * une conversation existante reste possible.
   */
  protected readonly canAnnounce = DIRECTION_ROLES.includes(this.role ?? '');
  protected readonly canStartConversation = this.canAnnounce;

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
    if (this.canStartConversation) {
      void this.messagingService
        .listUsers()
        .then((users) => this.users.set(users))
        .catch(() => undefined);
    }
  }

  /** Personne n'écrit dans une annonce, hormis la direction (ADR-019). */
  protected canReply(conversation: Conversation): boolean {
    return !conversation.announcement || this.canAnnounce;
  }

  async refresh(): Promise<void> {
    try {
      this.conversations.set(await this.messagingService.list());
    } catch (error) {
      this.errorMessage.set(extractErrorMessage(error));
    }
  }

  async create(announcement: boolean): Promise<void> {
    this.errorMessage.set(null);
    try {
      await this.messagingService.create(this.title, announcement, this.participantIds);
      this.title = '';
      await this.refresh();
    } catch (error) {
      this.errorMessage.set(extractErrorMessage(error));
    }
  }

  async open(conversation: Conversation): Promise<void> {
    this.errorMessage.set(null);
    this.active.set(conversation);
    try {
      this.messages.set(await this.messagingService.messages(conversation.id));
      await this.messagingService.markRead(conversation.id);
      await this.refresh();
    } catch (error) {
      this.errorMessage.set(extractErrorMessage(error));
    }
  }

  async send(): Promise<void> {
    const conversation = this.active();
    if (!conversation || !this.draft.trim()) {
      return;
    }
    this.errorMessage.set(null);
    try {
      await this.messagingService.send(conversation.id, this.draft);
      this.draft = '';
      this.messages.set(await this.messagingService.messages(conversation.id));
    } catch (error) {
      // 403 encore possible (défense en profondeur) : le message du serveur est explicite.
      this.errorMessage.set(extractErrorMessage(error));
    }
  }
}
