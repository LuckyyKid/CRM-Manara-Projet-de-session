import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
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

  getAnimations(): Observable<AnimationDto[]> {
    return this.http.get<AnimationDto[]>('/api/animateur/animations');
  }

  getNotifications(): Observable<AnimateurNotificationDto[]> {
    return this.http.get<AnimateurNotificationDto[]>('/api/animateur/notifications');
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
    });
  }

  getQuizzes(): Observable<QuizDto[]> {
    return this.http.get<QuizDto[]>('/api/animateur/quizzes');
  }

  getQuiz(quizId: number): Observable<QuizDto> {
    return this.http.get<QuizDto>(`/api/animateur/quizzes/${quizId}`);
  }

  createQuiz(request: QuizCreateRequestDto): Observable<QuizDto> {
    return this.http.post<QuizDto>('/api/animateur/quizzes', request);
  }

  backfillHomeworks(): Observable<ActionResponseDto> {
    return this.http.post<ActionResponseDto>('/api/animateur/quizzes/backfill-homeworks', {});
  }

  deleteQuiz(quizId: number): Observable<void> {
    return this.http.delete<void>(`/api/animateur/quizzes/${quizId}`);
  }

  getTutorDashboard(): Observable<TutorDashboardDto> {
    return this.http.get<TutorDashboardDto>('/api/animateur/quizzes/dashboard');
  }

  getQuizSubmissions(): Observable<TutorQuizSubmissionDto[]> {
    return this.http.get<TutorQuizSubmissionDto[]>('/api/animateur/quizzes/submissions');
  }

  getHomeworkOverview(): Observable<AnimateurHomeworkOverviewDto> {
    return this.http.get<AnimateurHomeworkOverviewDto>('/api/animateur/homeworks');
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

  getSportPracticePlans(): Observable<SportPracticePlanDto[]> {
    return this.http.get<SportPracticePlanDto[]>('/api/animateur/sport-practice-plans');
  }

  getSportPracticePlan(planId: number): Observable<SportPracticePlanDto> {
    return this.http.get<SportPracticePlanDto>(`/api/animateur/sport-practice-plans/${planId}`);
  }

  createSportPracticePlan(request: SportPracticePlanCreateRequestDto): Observable<SportPracticePlanDto> {
    return this.http.post<SportPracticePlanDto>('/api/animateur/sport-practice-plans', request);
  }
}
