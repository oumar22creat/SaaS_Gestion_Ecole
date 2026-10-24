import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../environments/environment';
import { ApiResponse } from '../core/api-response.model';
import { PARAMS_LISTE_COMPLETE, PARAMS_REFERENTIEL } from '../core/referentiel.params';

export interface Conversation {
  id: number;
  title: string | null;
  announcement: boolean;
  createdByUserId: number;
  createdAt: string;
  participantUserIds: number[];
  unreadCount: number;
}

export interface Message {
  id: number;
  conversationId: number;
  senderId: number;
  content: string;
  attachmentDocumentId: number | null;
  createdAt: string;
}

/**
 * Entrée de l'annuaire de messagerie (`GET /conversations/recipients`, ADR-019) : identité
 * et rôle seulement, jamais d'e-mail ni de téléphone.
 */
export interface Recipient {
  id: number;
  firstName: string;
  lastName: string;
  displayName: string;
  role: string;
}

/** Plafond serveur d'une conversation ordinaire (`MessagingService.MAX_RECIPIENTS`). */
export const MAX_RECIPIENTS = 30;

@Injectable({ providedIn: 'root' })
export class MessagingService {
  private readonly http = inject(HttpClient);

  async list(): Promise<Conversation[]> {
    const response = await firstValueFrom(
      this.http.get<ApiResponse<Conversation[]>>(`${environment.apiUrl}/conversations`, {
        params: PARAMS_LISTE_COMPLETE,
      }),
    );
    return response.data;
  }

  async create(
    title: string,
    announcement: boolean,
    participantUserIds: number[],
  ): Promise<Conversation> {
    const response = await firstValueFrom(
      this.http.post<ApiResponse<Conversation>>(`${environment.apiUrl}/conversations`, {
        title,
        announcement,
        participantUserIds,
      }),
    );
    return response.data;
  }

  async messages(id: number): Promise<Message[]> {
    const response = await firstValueFrom(
      this.http.get<ApiResponse<Message[]>>(`${environment.apiUrl}/conversations/${id}/messages`, {
        params: { pageSize: 100 },
      }),
    );
    return response.data;
  }

  async send(id: number, content: string): Promise<Message> {
    const response = await firstValueFrom(
      this.http.post<ApiResponse<Message>>(`${environment.apiUrl}/conversations/${id}/messages`, {
        content,
      }),
    );
    return response.data;
  }

  async markRead(id: number): Promise<void> {
    await firstValueFrom(this.http.post(`${environment.apiUrl}/conversations/${id}/read`, {}));
  }

  /**
   * Destinataires possibles pour le compte connecté, déjà filtrés par le serveur selon son
   * rôle (personnel ; familles de ses classes pour un enseignant ; tout compte pour la
   * direction). Recherche facultative sur le nom.
   */
  async listRecipients(search = ''): Promise<Recipient[]> {
    const term = search.trim();
    const response = await firstValueFrom(
      this.http.get<ApiResponse<Recipient[]>>(`${environment.apiUrl}/conversations/recipients`, {
        params: term ? { ...PARAMS_REFERENTIEL, search: term } : PARAMS_REFERENTIEL,
      }),
    );
    return response.data;
  }
}
