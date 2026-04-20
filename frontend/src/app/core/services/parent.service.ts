import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  ActionResponseDto,
  EnfantDto,
  InscriptionDto,
  ParentActivitiesResponseDto,
  ParentNotificationDto,
  ParentQuizDto,
  QuizAttemptDto,
  QuizAttemptSubmitDto,
} from '../models/api.models';

@Injectable({ providedIn: 'root' })
export class ParentService {
  private http = inject(HttpClient);

  getEnfants(): Observable<EnfantDto[]> {
    return this.http.get<EnfantDto[]>('/api/parent/enfants');
  }

  getInscriptions(): Observable<InscriptionDto[]> {
    return this.http.get<InscriptionDto[]>('/api/parent/inscriptions');
  }

  getActivities(): Observable<ParentActivitiesResponseDto> {
    return this.http.get<ParentActivitiesResponseDto>('/api/parent/activities');
  }

  getNotifications(): Observable<ParentNotificationDto[]> {
    return this.http.get<ParentNotificationDto[]>('/api/parent/notifications');
  }

  inscrireEnfant(enfantId: number, animationId: number): Observable<ActionResponseDto> {
    return this.http.post<ActionResponseDto>('/api/parent/inscriptions', { enfantId, animationId });
  }

  createEnfant(nom: string, prenom: string, dateDeNaissance: string): Observable<ActionResponseDto> {
    return this.http.post<ActionResponseDto>('/api/parent/enfants', { nom, prenom, dateDeNaissance });
  }

  updateEnfant(id: number, nom: string, prenom: string, dateDeNaissance: string): Observable<ActionResponseDto> {
    return this.http.put<ActionResponseDto>(`/api/parent/enfants/${id}`, { nom, prenom, dateDeNaissance });
  }

  getEnfant(id: number): Observable<EnfantDto> {
    return this.http.get<EnfantDto>(`/api/parent/enfants/${id}`);
  }

  getQuizzes(): Observable<ParentQuizDto[]> {
    return this.http.get<ParentQuizDto[]>('/api/parent/quizzes');
  }

  submitQuiz(quizId: number, request: QuizAttemptSubmitDto): Observable<QuizAttemptDto> {
    return this.http.post<QuizAttemptDto>(`/api/parent/quizzes/${quizId}/attempts`, request);
  }

  getQuizAttempts(): Observable<QuizAttemptDto[]> {
    return this.http.get<QuizAttemptDto[]>('/api/parent/quiz-attempts');
  }
}
