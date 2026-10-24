import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatIconModule } from '@angular/material/icon';
import { AuthTokenService } from '../auth/auth-token.service';
import { extractErrorMessage } from '../core/http-error.util';
import { DIRECTION_ROLES } from '../core/teaching-scope.service';
import { roleLabelFor } from '../account/staff-account.model';
import {
  Conversation,
  MAX_RECIPIENTS,
  Message,
  MessagingService,
  Recipient,
} from './messaging.service';

/** Rôles autorisés à ouvrir une conversation (ADR-019, `MessagingService.STAFF_ROLES`). */
export const MESSAGING_STAFF_ROLES = [
  'ADMIN',
  'DIRECTION',
  'TEACHER',
  'SECRETARY',
  'VIE_SCOLAIRE',
  'ACCOUNTANT',
];

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
          <mat-label>Rechercher un destinataire</mat-label>
          <input
            matInput
            data-testid="recipient-search"
            [ngModel]="search"
            (ngModelChange)="onSearch($event)"
          />
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Destinataires</mat-label>
          <mat-select
            [ngModel]="participantIds()"
            (ngModelChange)="participantIds.set($event)"
            multiple
          >
            @for (user of recipientOptions(); track user.id) {
              <mat-option [value]="user.id"
                >{{ user.displayName }} — {{ roleLabel(user.role) }}</mat-option
              >
            }
          </mat-select>
          <mat-hint data-testid="recipient-count">
            {{ participantIds().length }} / {{ maxRecipients }} destinataires
          </mat-hint>
        </mat-form-field>
        <button
          mat-stroked-button
          (click)="create(false)"
          [disabled]="participantIds().length === 0 || tooManyRecipients()"
        >
          <mat-icon>mail</mat-icon> Nouvelle conversation
        </button>
        @if (canAnnounce) {
          <button mat-flat-button (click)="create(true)">
            <mat-icon>campaign</mat-icon> Annonce
          </button>
        }
      </div>
      @if (tooManyRecipients()) {
        <p class="flash-error" data-testid="too-many-recipients">
          Une conversation compte au plus {{ maxRecipients }} destinataires ({{
            participantIds().length
          }}
          sélectionnés).
          @if (canAnnounce) {
            Pour vous adresser à tout l’établissement, utilisez une annonce.
          } @else {
            Pour vous adresser à tout l’établissement, demandez une annonce à la direction.
          }
        </p>
      }
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
   * Nouvelle conversation : tout le personnel ; les destinataires viennent de l'annuaire
   * `GET /conversations/recipients`, déjà filtré par le serveur selon le rôle. Les familles
   * n'ont pas l'écran (lecture seule côté mobile).
   */
  protected readonly canAnnounce = DIRECTION_ROLES.includes(this.role ?? '');
  protected readonly canStartConversation = MESSAGING_STAFF_ROLES.includes(this.role ?? '');
  protected readonly maxRecipients = MAX_RECIPIENTS;

  protected readonly conversations = signal<Conversation[]>([]);
  protected readonly messages = signal<Message[]>([]);
  /** Résultat de la dernière recherche dans l'annuaire. */
  protected readonly recipients = signal<Recipient[]>([]);
  /** Destinataires déjà choisis, gardés même s'ils sortent des résultats d'une recherche. */
  private readonly chosen = signal<Map<number, Recipient>>(new Map());
  protected readonly participantIds = signal<number[]>([]);
  protected readonly active = signal<Conversation | null>(null);
  protected readonly errorMessage = signal<string | null>(null);
  protected title = '';
  protected search = '';
  protected draft = '';
  private searchTimer: ReturnType<typeof setTimeout> | undefined;

  /** Options du sélecteur : la sélection courante d'abord, puis les résultats de recherche. */
  protected readonly recipientOptions = computed(() => {
    const selected = new Set(this.participantIds());
    const kept = [...this.chosen().values()].filter((r) => selected.has(r.id));
    const keptIds = new Set(kept.map((r) => r.id));
    return [...kept, ...this.recipients().filter((r) => !keptIds.has(r.id))];
  });

  /** Plafond serveur (422 `TOO_MANY_RECIPIENTS`) : signalé avant l'envoi. */
  protected readonly tooManyRecipients = computed(
    () => this.participantIds().length > MAX_RECIPIENTS,
  );

  constructor() {
    void this.refresh();
    if (this.canStartConversation) {
      void this.loadRecipients('');
    }
  }

  protected roleLabel(role: string): string {
    return roleLabelFor(role);
  }

  protected onSearch(term: string): void {
    this.search = term;
    clearTimeout(this.searchTimer);
    this.searchTimer = setTimeout(() => void this.loadRecipients(term), 300);
  }

  private async loadRecipients(term: string): Promise<void> {
    // Mémorise les choix en cours avant de remplacer la liste affichée.
    this.rememberChosen();
    try {
      this.recipients.set(await this.messagingService.listRecipients(term));
    } catch (error) {
      this.errorMessage.set(extractErrorMessage(error));
    }
  }

  private rememberChosen(): void {
    const selected = new Set(this.participantIds());
    const next = new Map<number, Recipient>();
    for (const r of [...this.chosen().values(), ...this.recipients()]) {
      if (selected.has(r.id)) {
        next.set(r.id, r);
      }
    }
    this.chosen.set(next);
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
    if (!announcement && this.tooManyRecipients()) {
      return;
    }
    try {
      await this.messagingService.create(
        this.title,
        announcement,
        announcement ? [] : this.participantIds(),
      );
      this.title = '';
      this.participantIds.set([]);
      this.chosen.set(new Map());
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
