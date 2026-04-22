import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { shareReplay, tap } from 'rxjs/operators';
import {
  ActionResponseDto,
  AnimateurHomeworkOverviewDto,
  AnimateurHomeworkStudentDetailDto,
  AnimateurNotificationDto,
  AnimationDto,
  HomeworkAttemptDto,
  HomeworkDto,
  InscriptionDto,
  QuizCreateRequestDto,
  QuizDto,
  SportPracticePlanCreateRequestDto,
  SportPracticePlanDto,
  TutorDashboardDto,
  TutorQuizSubmissionDto,
} from '../models/api.models';

@Injectable({ providedIn: 'root' })
export class AnimateurService {
  private http = inject(HttpClient);

  private animations$?: Observable<AnimationDto[]>;
  private notifications$?: Observable<AnimateurNotificationDto[]>;
  private quizzes$?: Observable<QuizDto[]>;
  private tutorDashboard$?: Observable<TutorDashboardDto>;
  private homeworkOverview$?: Observable<AnimateurHomeworkOverviewDto>;
  private sportPracticePlans$?: Observable<SportPracticePlanDto[]>;

  getAnimations(forceRefresh = false): Observable<AnimationDto[]> {
    if (!this.animations$ || forceRefresh) {
      this.animations$ = this.http.get<AnimationDto[]>('/api/animateur/animations').pipe(shareReplay(1));
    }
    return this.animations$;
  }

  getNotifications(forceRefresh = false): Observable<AnimateurNotificationDto[]> {
    if (!this.notifications$ || forceRefresh) {
      this.notifications$ = this.http.get<AnimateurNotificationDto[]>('/api/animateur/notifications').pipe(shareReplay(1));
    }
    return this.notifications$;
  }

  markAllNotificationsAsRead(): Observable<ActionResponseDto> {
    return this.http.put<ActionResponseDto>('/api/animateur/notifications/read-all', {}).pipe(
      tap(() => {
        this.notifications$ = undefined;
      }),
    );
  }

  markNotificationAsRead(notificationId: number): Observable<ActionResponseDto> {
    return this.http.put<ActionResponseDto>(`/api/animateur/notifications/${notificationId}/read`, {}).pipe(
      tap(() => {
        this.notifications$ = undefined;
      }),
    );
  }

  getInscriptionsForAnimation(animationId: number): Observable<InscriptionDto[]> {
    return this.http.get<InscriptionDto[]>(`/api/animateur/animations/${animationId}/inscriptions`);
  }

  searchInscriptions(animationId: number | null, search: string): Observable<InscriptionDto[]> {
    let params = new HttpParams();
    if (animationId !== null) {
      params = params.set('animationId', animationId);
    }
    if (search.trim()) {
      params = params.set('search', search.trim());
    }
    return this.http.get<InscriptionDto[]>('/api/animateur/inscriptions', { params });
  }

  updatePresence(
    inscriptionId: number,
    presenceStatus: string,
    incidentNote: string,
  ): Observable<ActionResponseDto> {
    return this.http.post<ActionResponseDto>(`/api/animateur/inscriptions/${inscriptionId}/presence`, {
      presenceStatus,
      incidentNote,
    }).pipe(
      tap(() => this.invalidateHomeworkData()),
    );
  }

  getQuizzes(forceRefresh = false): Observable<QuizDto[]> {
    if (!this.quizzes$ || forceRefresh) {
      this.quizzes$ = this.http.get<QuizDto[]>('/api/animateur/quizzes').pipe(shareReplay(1));
    }
    return this.quizzes$;
  }

  getQuiz(quizId: number): Observable<QuizDto> {
    return this.http.get<QuizDto>(`/api/animateur/quizzes/${quizId}`);
  }

  createQuiz(request: QuizCreateRequestDto): Observable<QuizDto> {
    return this.http.post<QuizDto>('/api/animateur/quizzes', request).pipe(
      tap(() => {
        this.quizzes$ = undefined;
        this.tutorDashboard$ = undefined;
      }),
    );
  }

  backfillHomeworks(): Observable<ActionResponseDto> {
    return this.http.post<ActionResponseDto>('/api/animateur/quizzes/backfill-homeworks', {}).pipe(
      tap(() => this.invalidateHomeworkData()),
    );
  }

  deleteQuiz(quizId: number): Observable<void> {
    return this.http.delete<void>(`/api/animateur/quizzes/${quizId}`).pipe(
      tap(() => {
        this.quizzes$ = undefined;
        this.tutorDashboard$ = undefined;
        this.homeworkOverview$ = undefined;
      }),
    );
  }

  getTutorDashboard(forceRefresh = false): Observable<TutorDashboardDto> {
    if (!this.tutorDashboard$ || forceRefresh) {
      this.tutorDashboard$ = this.http.get<TutorDashboardDto>('/api/animateur/quizzes/dashboard').pipe(shareReplay(1));
    }
    return this.tutorDashboard$;
  }

  getQuizSubmissions(): Observable<TutorQuizSubmissionDto[]> {
    return this.http.get<TutorQuizSubmissionDto[]>('/api/animateur/quizzes/submissions');
  }

  getHomeworkOverview(forceRefresh = false): Observable<AnimateurHomeworkOverviewDto> {
    if (!this.homeworkOverview$ || forceRefresh) {
      this.homeworkOverview$ = this.http.get<AnimateurHomeworkOverviewDto>('/api/animateur/homeworks').pipe(shareReplay(1));
    }
    return this.homeworkOverview$;
  }

  getHomeworkStudentDetail(enfantId: number): Observable<AnimateurHomeworkStudentDetailDto> {
    return this.http.get<AnimateurHomeworkStudentDetailDto>(`/api/animateur/homeworks/students/${enfantId}`);
  }

  getHomeworkAssignment(assignmentId: number): Observable<HomeworkDto> {
    return this.http.get<HomeworkDto>(`/api/animateur/homeworks/${assignmentId}`);
  }

  getHomeworkLatestAttempt(assignmentId: number): Observable<HomeworkAttemptDto> {
    return this.http.get<HomeworkAttemptDto>(`/api/animateur/homeworks/${assignmentId}/latest-attempt`);
  }

  getSportPracticePlans(forceRefresh = false): Observable<SportPracticePlanDto[]> {
    if (!this.sportPracticePlans$ || forceRefresh) {
      this.sportPracticePlans$ = this.http.get<SportPracticePlanDto[]>('/api/animateur/sport-practice-plans').pipe(shareReplay(1));
    }
    return this.sportPracticePlans$;
  }

  getSportPracticePlan(planId: number): Observable<SportPracticePlanDto> {
    return this.http.get<SportPracticePlanDto>(`/api/animateur/sport-practice-plans/${planId}`);
  }

  createSportPracticePlan(request: SportPracticePlanCreateRequestDto): Observable<SportPracticePlanDto> {
    return this.http.post<SportPracticePlanDto>('/api/animateur/sport-practice-plans', request).pipe(
      tap(() => {
        this.sportPracticePlans$ = undefined;
        this.animations$ = undefined;
      }),
    );
  }

  private invalidateHomeworkData(): void {
    this.homeworkOverview$ = undefined;
    this.tutorDashboard$ = undefined;
  }
}
