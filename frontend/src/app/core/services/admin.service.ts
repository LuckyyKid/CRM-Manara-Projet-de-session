import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  ActionResponseDto,
  ActivityRequestDto,
  ActivityDto,
  AdminAnimationRowDto,
  AdminDemandesDto,
  AdminNotificationDto,
  AdminOptionsDto,
  AnimateurDto,
  AnimateurRequestDto,
  AnimationDto,
  AnimationRequestDto,
} from '../models/api.models';

@Injectable({ providedIn: 'root' })
export class AdminService {
  private http = inject(HttpClient);

  getActivities(): Observable<ActivityDto[]> {
    return this.http.get<ActivityDto[]>('/api/admin/activities');
  }

  getActivity(id: number): Observable<ActivityDto> {
    return this.http.get<ActivityDto>(`/api/admin/activities/${id}`);
  }

  createActivity(request: ActivityRequestDto): Observable<ActivityDto> {
    return this.http.post<ActivityDto>('/api/admin/activities', request);
  }

  updateActivity(id: number, request: ActivityRequestDto): Observable<ActivityDto> {
    return this.http.put<ActivityDto>(`/api/admin/activities/${id}`, request);
  }

  getAnimations(): Observable<AdminAnimationRowDto[]> {
    return this.http.get<AdminAnimationRowDto[]>('/api/admin/animations');
  }

  getAnimation(id: number): Observable<AnimationDto> {
    return this.http.get<AnimationDto>(`/api/admin/animations/${id}`);
  }

  createAnimation(request: AnimationRequestDto): Observable<AnimationDto> {
    return this.http.post<AnimationDto>('/api/admin/animations', request);
  }

  updateAnimation(id: number, request: AnimationRequestDto): Observable<AnimationDto> {
    return this.http.put<AnimationDto>(`/api/admin/animations/${id}`, request);
  }

  getAnimateurs(): Observable<AnimateurDto[]> {
    return this.http.get<AnimateurDto[]>('/api/admin/animateurs');
  }

  getAnimateur(id: number): Observable<AnimateurDto> {
    return this.http.get<AnimateurDto>(`/api/admin/animateurs/${id}`);
  }

  createAnimateur(request: AnimateurRequestDto): Observable<AnimateurDto> {
    return this.http.post<AnimateurDto>('/api/admin/animateurs', request);
  }

  updateAnimateur(id: number, request: AnimateurRequestDto): Observable<AnimateurDto> {
    return this.http.put<AnimateurDto>(`/api/admin/animateurs/${id}`, request);
  }

  getOptions(): Observable<AdminOptionsDto> {
    return this.http.get<AdminOptionsDto>('/api/admin/options');
  }

  getDemandes(): Observable<AdminDemandesDto> {
    return this.http.get<AdminDemandesDto>('/api/admin/demandes');
  }

  getNotifications(): Observable<AdminNotificationDto[]> {
    return this.http.get<AdminNotificationDto[]>('/api/admin/notifications');
  }

  updateParentStatus(id: number, enabled: boolean): Observable<ActionResponseDto> {
    return this.http.post<ActionResponseDto>(`/api/admin/parents/${id}/status?enabled=${enabled}`, {});
  }

  updateEnfantStatus(id: number, active: boolean): Observable<ActionResponseDto> {
    return this.http.post<ActionResponseDto>(`/api/admin/enfants/${id}/status?active=${active}`, {});
  }

  updateAnimateurStatus(id: number, enabled: boolean): Observable<ActionResponseDto> {
    return this.http.post<ActionResponseDto>(`/api/admin/animateurs/${id}/status?enabled=${enabled}`, {});
  }

  approveInscription(id: number): Observable<ActionResponseDto> {
    return this.http.post<ActionResponseDto>(`/api/admin/inscriptions/${id}/approve`, {});
  }

  rejectInscription(id: number): Observable<ActionResponseDto> {
    return this.http.post<ActionResponseDto>(`/api/admin/inscriptions/${id}/reject`, {});
  }
}
