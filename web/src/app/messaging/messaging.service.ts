import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../environments/environment';
import { ApiResponse } from '../core/api-response.model';

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

export interface StaffUser {
  id: number;
  email: string;
  firstName: string;
  lastName: string;
  role: string;
}

@Injectable({ providedIn: 'root' })
export class MessagingService {
  private readonly http = inject(HttpClient);

  async list(): Promise<Conversation[]> {
    const response = await firstValueFrom(
      this.http.get<ApiResponse<Conversation[]>>(`${environment.apiUrl}/conversations`),
    );
    return response.data;
  }

  async create(title: string, announcement: boolean, participantUserIds: number[]): Promise<Conversation> {
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
      this.http.post<ApiResponse<Message>>(`${environment.apiUrl}/conversations/${id}/messages`, { content }),
    );
    return response.data;
  }

  async markRead(id: number): Promise<void> {
    await firstValueFrom(this.http.post(`${environment.apiUrl}/conversations/${id}/read`, {}));
  }

  async listUsers(): Promise<StaffUser[]> {
    const response = await firstValueFrom(
      this.http.get<ApiResponse<StaffUser[]>>(`${environment.apiUrl}/users`, { params: { pageSize: 200 } }),
    );
    return response.data;
  }
}
