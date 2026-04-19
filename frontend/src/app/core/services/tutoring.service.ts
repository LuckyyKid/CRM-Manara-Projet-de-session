import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';

// Service Angular pour communiquer avec l'API tutorat
@Injectable({ providedIn: 'root' })
export class TutoringService {
  private api = '/api/tutoring';

  constructor(private http: HttpClient) {}

  getTutoratAnimations() {
    return this.http.get<any[]>(`${this.api}/animations`);
  }

  createSession(animationId: number, tutorId: number, contentText: string) {
    return this.http.post<any>(`${this.api}/sessions`, { animationId, tutorId, contentText });
  }

  getSession(sessionId: number) {
    return this.http.get<any>(`${this.api}/sessions/${sessionId}`);
  }

  deleteSession(sessionId: number) {
    return this.http.delete<any>(`${this.api}/sessions/${sessionId}`);
  }

  getStudentSession(studentId: number, sessionId: number) {
    return this.http.get<any>(`${this.api}/student/${studentId}/sessions/${sessionId}`);
  }

  submitQuiz(studentId: number, sessionId: number, answers: any[]) {
    return this.http.post<any>(`${this.api}/quiz/submit`, { studentId, sessionId, answers });
  }

  getScores(studentId: number) {
    return this.http.get<any[]>(`${this.api}/student/${studentId}/scores`);
  }

  getHomework(studentId: number) {
    return this.http.get<any[]>(`${this.api}/student/${studentId}/homework`);
  }

  generateHomework(studentId: number, axisId: number) {
    return this.http.post<any>(`${this.api}/homework/0/generate`, { studentId, axisId });
  }

  submitHomework(homeworkId: number, answers: any[]) {
    return this.http.post<any>(`${this.api}/homework/${homeworkId}/submit`, { answers });
  }

  getReviewQuestions(studentId: number) {
    return this.http.get<any[]>(`${this.api}/student/${studentId}/review-questions`);
  }

  getTutorDashboard(animationId: number) {
    return this.http.get<any>(`${this.api}/tutor/dashboard/${animationId}`);
  }

  getAlerts(animationId: number) {
    return this.http.get<any[]>(`${this.api}/tutor/alerts/${animationId}`);
  }

  getParentProgress(enfantId: number) {
    return this.http.get<any[]>(`${this.api}/parent/progress/${enfantId}`);
  }

  getPendingQuizzes(enfantId: number) {
    return this.http.get<any[]>(`${this.api}/student/${enfantId}/pending-quizzes`);
  }

  getHistory(enfantId: number) {
    return this.http.get<any[]>(`${this.api}/student/${enfantId}/history`);
  }

  generateGroupHomework(animationId: number, axisId: number) {
    return this.http.post<any>(`${this.api}/homework/generate-group`, { animationId, axisId });
  }
}
