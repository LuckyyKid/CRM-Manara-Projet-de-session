import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  ActionResponseDto,
  ActivityRequestDto,
  ActivityDto,
  AdminAnimationRowDto,
  AdminDemandesDto,
  AdminInscriptionReviewDto,
  AdminNotificationDto,
  AdminOptionsDto,
  AnimateurDto,
  AnimateurRequestDto,
  AnimationDto,
  AnimationRequestDto,
  EnfantDto,
  ParentDto,
  QuizDto,
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

  getAnimationQuizzes(id: number): Observable<QuizDto[]> {
    return this.http.get<QuizDto[]>(`/api/admin/animations/${id}/quizzes`);
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

  getParents(): Observable<ParentDto[]> {
    return this.http.get<ParentDto[]>('/api/admin/parents');
  }

  getEnfants(): Observable<EnfantDto[]> {
    return this.http.get<EnfantDto[]>('/api/admin/enfants');
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

  searchInscriptions(filters: {
    animateurId?: number | null;
    activityId?: number | null;
    animationId?: number | null;
    parentId?: number | null;
    enfantId?: number | null;
    status?: string;
    search?: string;
  }): Observable<AdminInscriptionReviewDto[]> {
    let params = new HttpParams();
    for (const [key, value] of Object.entries(filters)) {
      if (value !== null && value !== undefined && `${value}`.trim() !== '') {
        params = params.set(key, `${value}`.trim());
      }
    }
    return this.http.get<AdminInscriptionReviewDto[]>('/api/admin/inscriptions', { params });
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
