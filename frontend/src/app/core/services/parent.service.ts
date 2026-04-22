import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { shareReplay, tap } from 'rxjs/operators';
import {
  ActionResponseDto,
  EnfantDto,
  HomeworkAttemptDto,
  HomeworkAttemptSubmitDto,
  HomeworkDto,
  InscriptionDto,
  ParentActivitiesResponseDto,
  ParentNotificationDto,
  ParentQuizAttemptDetailDto,
  ParentQuizDto,
  QuizAttemptDto,
  QuizAttemptSubmitDto,
  SportPracticePlanDto,
} from '../models/api.models';

@Injectable({ providedIn: 'root' })
export class ParentService {
  private http = inject(HttpClient);

  private enfants$?: Observable<EnfantDto[]>;
  private inscriptions$?: Observable<InscriptionDto[]>;
  private activities$?: Observable<ParentActivitiesResponseDto>;
  private notifications$?: Observable<ParentNotificationDto[]>;
  private quizzes$?: Observable<ParentQuizDto[]>;
  private quizAttempts$?: Observable<QuizAttemptDto[]>;
  private homeworks$?: Observable<HomeworkDto[]>;
  private homeworkAttempts$?: Observable<HomeworkAttemptDto[]>;
  private sportPracticePlans$?: Observable<SportPracticePlanDto[]>;

  getEnfants(forceRefresh = false): Observable<EnfantDto[]> {
    if (!this.enfants$ || forceRefresh) {
      this.enfants$ = this.http.get<EnfantDto[]>('/api/parent/enfants').pipe(shareReplay(1));
    }
    return this.enfants$;
  }

  getInscriptions(forceRefresh = false): Observable<InscriptionDto[]> {
    if (!this.inscriptions$ || forceRefresh) {
      this.inscriptions$ = this.http.get<InscriptionDto[]>('/api/parent/inscriptions').pipe(shareReplay(1));
    }
    return this.inscriptions$;
  }

  getActivities(forceRefresh = false): Observable<ParentActivitiesResponseDto> {
    if (!this.activities$ || forceRefresh) {
      this.activities$ = this.http.get<ParentActivitiesResponseDto>('/api/parent/activities').pipe(shareReplay(1));
    }
    return this.activities$;
  }

  getNotifications(forceRefresh = false): Observable<ParentNotificationDto[]> {
    if (!this.notifications$ || forceRefresh) {
      this.notifications$ = this.http.get<ParentNotificationDto[]>('/api/parent/notifications').pipe(shareReplay(1));
    }
    return this.notifications$;
  }

  markAllNotificationsAsRead(): Observable<ActionResponseDto> {
    return this.http.post<ActionResponseDto>('/api/parent/notifications/read-all', {}).pipe(
      tap(() => {
        this.notifications$ = undefined;
      }),
    );
  }

  markNotificationAsRead(notificationId: number): Observable<ActionResponseDto> {
    return this.http.post<ActionResponseDto>(`/api/parent/notifications/${notificationId}/read`, {}).pipe(
      tap(() => {
        this.notifications$ = undefined;
      }),
    );
  }

  inscrireEnfant(enfantId: number, animationId: number): Observable<ActionResponseDto> {
    return this.http.post<ActionResponseDto>('/api/parent/inscriptions', { enfantId, animationId }).pipe(
      tap(() => {
        this.inscriptions$ = undefined;
        this.activities$ = undefined;
      }),
    );
  }

  createEnfant(nom: string, prenom: string, dateDeNaissance: string): Observable<ActionResponseDto> {
    return this.http.post<ActionResponseDto>('/api/parent/enfants', { nom, prenom, dateDeNaissance }).pipe(
      tap(() => {
        this.enfants$ = undefined;
        this.activities$ = undefined;
      }),
    );
  }

  updateEnfant(id: number, nom: string, prenom: string, dateDeNaissance: string): Observable<ActionResponseDto> {
    return this.http.put<ActionResponseDto>(`/api/parent/enfants/${id}`, { nom, prenom, dateDeNaissance }).pipe(
      tap(() => {
        this.enfants$ = undefined;
        this.activities$ = undefined;
      }),
    );
  }

  getEnfant(id: number): Observable<EnfantDto> {
    return this.http.get<EnfantDto>(`/api/parent/enfants/${id}`);
  }

  getQuizzes(forceRefresh = false): Observable<ParentQuizDto[]> {
    if (!this.quizzes$ || forceRefresh) {
      this.quizzes$ = this.http.get<ParentQuizDto[]>('/api/parent/quizzes').pipe(shareReplay(1));
    }
    return this.quizzes$;
  }

  getQuiz(quizId: number): Observable<ParentQuizDto> {
    return this.http.get<ParentQuizDto>(`/api/parent/quizzes/${quizId}`);
  }

  submitQuiz(quizId: number, request: QuizAttemptSubmitDto): Observable<QuizAttemptDto> {
    return this.http.post<QuizAttemptDto>(`/api/parent/quizzes/${quizId}/attempts`, request).pipe(
      tap(() => {
        this.quizAttempts$ = undefined;
        this.quizzes$ = undefined;
        this.homeworks$ = undefined;
      }),
    );
  }

  getQuizAttempts(forceRefresh = false): Observable<QuizAttemptDto[]> {
    if (!this.quizAttempts$ || forceRefresh) {
      this.quizAttempts$ = this.http.get<QuizAttemptDto[]>('/api/parent/quiz-attempts').pipe(shareReplay(1));
    }
    return this.quizAttempts$;
  }

  getQuizAttempt(attemptId: number): Observable<ParentQuizAttemptDetailDto> {
    return this.http.get<ParentQuizAttemptDetailDto>(`/api/parent/quiz-attempts/${attemptId}`);
  }

  generateHomeworkFromQuizAttempt(attemptId: number): Observable<HomeworkDto> {
    return this.http.post<HomeworkDto>(`/api/parent/quiz-attempts/${attemptId}/generate-homework`, {}).pipe(
      tap(() => {
        this.homeworks$ = undefined;
        this.homeworkAttempts$ = undefined;
      }),
    );
  }

  getHomeworks(forceRefresh = false): Observable<HomeworkDto[]> {
    if (!this.homeworks$ || forceRefresh) {
      this.homeworks$ = this.http.get<HomeworkDto[]>('/api/parent/homeworks').pipe(shareReplay(1));
    }
    return this.homeworks$;
  }

  getHomework(homeworkId: number): Observable<HomeworkDto> {
    return this.http.get<HomeworkDto>(`/api/parent/homeworks/${homeworkId}`);
  }

  submitHomework(homeworkId: number, request: HomeworkAttemptSubmitDto): Observable<HomeworkAttemptDto> {
    return this.http.post<HomeworkAttemptDto>(`/api/parent/homeworks/${homeworkId}/attempts`, request).pipe(
      tap(() => {
        this.homeworkAttempts$ = undefined;
        this.homeworks$ = undefined;
      }),
    );
  }

  getHomeworkAttempts(forceRefresh = false): Observable<HomeworkAttemptDto[]> {
    if (!this.homeworkAttempts$ || forceRefresh) {
      this.homeworkAttempts$ = this.http.get<HomeworkAttemptDto[]>('/api/parent/homework-attempts').pipe(shareReplay(1));
    }
    return this.homeworkAttempts$;
  }

  getHomeworkAttempt(attemptId: number): Observable<HomeworkAttemptDto> {
    return this.http.get<HomeworkAttemptDto>(`/api/parent/homework-attempts/${attemptId}`);
  }

  getSportPracticePlans(forceRefresh = false): Observable<SportPracticePlanDto[]> {
    if (!this.sportPracticePlans$ || forceRefresh) {
      this.sportPracticePlans$ = this.http.get<SportPracticePlanDto[]>('/api/parent/sport-practice-plans').pipe(shareReplay(1));
    }
    return this.sportPracticePlans$;
  }

  getSportPracticePlan(planId: number): Observable<SportPracticePlanDto> {
    return this.http.get<SportPracticePlanDto>(`/api/parent/sport-practice-plans/${planId}`);
  }
}
