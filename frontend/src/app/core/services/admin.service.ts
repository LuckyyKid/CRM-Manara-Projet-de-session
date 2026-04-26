import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { shareReplay, tap } from 'rxjs/operators';
import {
  ActionResponseDto,
  ActivityRequestDto,
  ActivityDto,
  AdminAnimationRowDto,
  AdminDemandesDto,
  AdminInscriptionReviewDto,
  AdminNotificationDto,
  AdminOptionsDto,
  AdminSubscriptionRowDto,
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

  private activities$?: Observable<ActivityDto[]>;
  private animations$?: Observable<AdminAnimationRowDto[]>;
  private animateurs$?: Observable<AnimateurDto[]>;
  private parents$?: Observable<ParentDto[]>;
  private enfants$?: Observable<EnfantDto[]>;
  private options$?: Observable<AdminOptionsDto>;
  private demandes$?: Observable<AdminDemandesDto>;
  private notifications$?: Observable<AdminNotificationDto[]>;
  private subscriptions$?: Observable<AdminSubscriptionRowDto[]>;

  getActivities(forceRefresh = false): Observable<ActivityDto[]> {
    if (!this.activities$ || forceRefresh) {
      this.activities$ = this.http.get<ActivityDto[]>('/api/admin/activities').pipe(shareReplay(1));
    }
    return this.activities$;
  }

  getActivity(id: number): Observable<ActivityDto> {
    return this.http.get<ActivityDto>(`/api/admin/activities/${id}`);
  }

  createActivity(request: ActivityRequestDto): Observable<ActivityDto> {
    return this.http.post<ActivityDto>('/api/admin/activities', request).pipe(
      tap(() => this.invalidateAdminLists()),
    );
  }

  updateActivity(id: number, request: ActivityRequestDto): Observable<ActivityDto> {
    return this.http.put<ActivityDto>(`/api/admin/activities/${id}`, request).pipe(
      tap(() => this.invalidateAdminLists()),
    );
  }

  deleteActivity(id: number): Observable<ActionResponseDto> {
    return this.http.delete<ActionResponseDto>(`/api/admin/activities/${id}`).pipe(
      tap(() => this.invalidateAdminLists()),
    );
  }

  getAnimations(forceRefresh = false): Observable<AdminAnimationRowDto[]> {
    if (!this.animations$ || forceRefresh) {
      this.animations$ = this.http.get<AdminAnimationRowDto[]>('/api/admin/animations').pipe(shareReplay(1));
    }
    return this.animations$;
  }

  getAnimation(id: number): Observable<AnimationDto> {
    return this.http.get<AnimationDto>(`/api/admin/animations/${id}`);
  }

  getAnimationQuizzes(id: number): Observable<QuizDto[]> {
    return this.http.get<QuizDto[]>(`/api/admin/animations/${id}/quizzes`);
  }

  createAnimation(request: AnimationRequestDto): Observable<AnimationDto> {
    return this.http.post<AnimationDto>('/api/admin/animations', request).pipe(
      tap(() => this.invalidateAdminLists()),
    );
  }

  updateAnimation(id: number, request: AnimationRequestDto): Observable<AnimationDto> {
    return this.http.put<AnimationDto>(`/api/admin/animations/${id}`, request).pipe(
      tap(() => this.invalidateAdminLists()),
    );
  }

  deleteAnimation(id: number): Observable<ActionResponseDto> {
    return this.http.delete<ActionResponseDto>(`/api/admin/animations/${id}`).pipe(
      tap(() => this.invalidateAdminLists()),
    );
  }

  getAnimateurs(forceRefresh = false): Observable<AnimateurDto[]> {
    if (!this.animateurs$ || forceRefresh) {
      this.animateurs$ = this.http.get<AnimateurDto[]>('/api/admin/animateurs').pipe(shareReplay(1));
    }
    return this.animateurs$;
  }

  getAnimateur(id: number): Observable<AnimateurDto> {
    return this.http.get<AnimateurDto>(`/api/admin/animateurs/${id}`);
  }

  getParents(forceRefresh = false): Observable<ParentDto[]> {
    if (!this.parents$ || forceRefresh) {
      this.parents$ = this.http.get<ParentDto[]>('/api/admin/parents').pipe(shareReplay(1));
    }
    return this.parents$;
  }

  getEnfants(forceRefresh = false): Observable<EnfantDto[]> {
    if (!this.enfants$ || forceRefresh) {
      this.enfants$ = this.http.get<EnfantDto[]>('/api/admin/enfants').pipe(shareReplay(1));
    }
    return this.enfants$;
  }

  createAnimateur(request: AnimateurRequestDto): Observable<AnimateurDto> {
    return this.http.post<AnimateurDto>('/api/admin/animateurs', request).pipe(
      tap(() => this.invalidateAdminLists()),
    );
  }

  updateAnimateur(id: number, request: AnimateurRequestDto): Observable<AnimateurDto> {
    return this.http.put<AnimateurDto>(`/api/admin/animateurs/${id}`, request).pipe(
      tap(() => this.invalidateAdminLists()),
    );
  }

  deleteAnimateur(id: number): Observable<ActionResponseDto> {
    return this.http.delete<ActionResponseDto>(`/api/admin/animateurs/${id}`).pipe(
      tap(() => this.invalidateAdminLists()),
    );
  }

  getOptions(forceRefresh = false): Observable<AdminOptionsDto> {
    if (!this.options$ || forceRefresh) {
      this.options$ = this.http.get<AdminOptionsDto>('/api/admin/options').pipe(shareReplay(1));
    }
    return this.options$;
  }

  getDemandes(forceRefresh = false): Observable<AdminDemandesDto> {
    if (!this.demandes$ || forceRefresh) {
      this.demandes$ = this.http.get<AdminDemandesDto>('/api/admin/demandes').pipe(shareReplay(1));
    }
    return this.demandes$;
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

  getNotifications(forceRefresh = false): Observable<AdminNotificationDto[]> {
    if (!this.notifications$ || forceRefresh) {
      this.notifications$ = this.http.get<AdminNotificationDto[]>('/api/admin/notifications').pipe(shareReplay(1));
    }
    return this.notifications$;
  }

  getSubscriptions(forceRefresh = false): Observable<AdminSubscriptionRowDto[]> {
    if (!this.subscriptions$ || forceRefresh) {
      this.subscriptions$ = this.http.get<AdminSubscriptionRowDto[]>('/api/admin/subscriptions').pipe(shareReplay(1));
    }
    return this.subscriptions$;
  }

  updateParentStatus(id: number, enabled: boolean): Observable<ActionResponseDto> {
    return this.http.post<ActionResponseDto>(`/api/admin/parents/${id}/status?enabled=${enabled}`, {}).pipe(
      tap(() => this.invalidateAdminLists()),
    );
  }

  deleteParent(id: number): Observable<ActionResponseDto> {
    return this.http.delete<ActionResponseDto>(`/api/admin/parents/${id}`).pipe(
      tap(() => this.invalidateAdminLists()),
    );
  }

  updateEnfantStatus(id: number, active: boolean): Observable<ActionResponseDto> {
    return this.http.post<ActionResponseDto>(`/api/admin/enfants/${id}/status?active=${active}`, {}).pipe(
      tap(() => this.invalidateAdminLists()),
    );
  }

  deleteEnfant(id: number): Observable<ActionResponseDto> {
    return this.http.delete<ActionResponseDto>(`/api/admin/enfants/${id}`).pipe(
      tap(() => this.invalidateAdminLists()),
    );
  }

  updateAnimateurStatus(id: number, enabled: boolean): Observable<ActionResponseDto> {
    return this.http.post<ActionResponseDto>(`/api/admin/animateurs/${id}/status?enabled=${enabled}`, {}).pipe(
      tap(() => this.invalidateAdminLists()),
    );
  }

  approveInscription(id: number): Observable<ActionResponseDto> {
    return this.http.post<ActionResponseDto>(`/api/admin/inscriptions/${id}/approve`, {}).pipe(
      tap(() => this.invalidateAdminLists()),
    );
  }

  rejectInscription(id: number): Observable<ActionResponseDto> {
    return this.http.post<ActionResponseDto>(`/api/admin/inscriptions/${id}/reject`, {}).pipe(
      tap(() => this.invalidateAdminLists()),
    );
  }

  invalidateActivities(): void {
    this.activities$ = undefined;
  }

  invalidateAnimations(): void {
    this.animations$ = undefined;
  }

  invalidateAnimateurs(): void {
    this.animateurs$ = undefined;
  }

  invalidateParents(): void {
    this.parents$ = undefined;
  }

  invalidateEnfants(): void {
    this.enfants$ = undefined;
  }

  invalidateDemandes(): void {
    this.demandes$ = undefined;
  }

  invalidateNotifications(): void {
    this.notifications$ = undefined;
  }

  invalidateSubscriptions(): void {
    this.subscriptions$ = undefined;
  }

  invalidateAdminLists(): void {
    this.invalidateActivities();
    this.invalidateAnimations();
    this.invalidateAnimateurs();
    this.invalidateParents();
    this.invalidateEnfants();
    this.invalidateDemandes();
    this.invalidateNotifications();
    this.invalidateSubscriptions();
  }
}
