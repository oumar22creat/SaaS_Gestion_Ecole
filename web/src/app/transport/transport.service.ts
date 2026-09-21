import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../environments/environment';
import { ApiResponse } from '../core/api-response.model';

export interface BusRoute {
  id: number;
  label: string;
}

export interface BusStop {
  id: number;
  busRouteId: number;
  name: string;
  sequenceOrder: number;
}

@Injectable({ providedIn: 'root' })
export class TransportService {
  private readonly http = inject(HttpClient);

  async listRoutes(): Promise<BusRoute[]> {
    const response = await firstValueFrom(
      this.http.get<ApiResponse<BusRoute[]>>(`${environment.apiUrl}/transport/routes`),
    );
    return response.data;
  }

  async createRoute(label: string): Promise<BusRoute> {
    const response = await firstValueFrom(
      this.http.post<ApiResponse<BusRoute>>(`${environment.apiUrl}/transport/routes`, { label }),
    );
    return response.data;
  }

  async addStop(routeId: number, name: string, sequenceOrder: number): Promise<BusStop> {
    const response = await firstValueFrom(
      this.http.post<ApiResponse<BusStop>>(`${environment.apiUrl}/transport/routes/${routeId}/stops`, {
        name,
        sequenceOrder,
      }),
    );
    return response.data;
  }

  async listStops(routeId: number): Promise<BusStop[]> {
    const response = await firstValueFrom(
      this.http.get<ApiResponse<BusStop[]>>(`${environment.apiUrl}/transport/routes/${routeId}/stops`),
    );
    return response.data;
  }

  async assign(studentId: number, busRouteId: number, busStopId: number): Promise<void> {
    await firstValueFrom(
      this.http.post(`${environment.apiUrl}/transport/assignments`, { studentId, busRouteId, busStopId }),
    );
  }
}
