import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../environments/environment';
import { ApiResponse } from '../core/api-response.model';
import { Room, RoomRequest } from './room.model';

const BASE_URL = `${environment.apiUrl}/rooms`;

@Injectable({ providedIn: 'root' })
export class RoomService {
  private readonly http = inject(HttpClient);

  async list(): Promise<Room[]> {
    const response = await firstValueFrom(this.http.get<ApiResponse<Room[]>>(BASE_URL, { params: { pageSize: 200 } }));
    return response.data;
  }

  async create(request: RoomRequest): Promise<Room> {
    const response = await firstValueFrom(this.http.post<ApiResponse<Room>>(BASE_URL, request));
    return response.data;
  }

  async update(id: number, request: RoomRequest): Promise<Room> {
    const response = await firstValueFrom(this.http.put<ApiResponse<Room>>(`${BASE_URL}/${id}`, request));
    return response.data;
  }

  async remove(id: number): Promise<void> {
    await firstValueFrom(this.http.delete<void>(`${BASE_URL}/${id}`));
  }
}
