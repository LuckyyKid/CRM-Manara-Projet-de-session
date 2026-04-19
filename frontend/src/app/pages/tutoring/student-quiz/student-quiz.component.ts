import { Component, inject, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { timeout } from 'rxjs';
import { TutoringService } from '../../../core/services/tutoring.service';

@Component({
  selector: 'app-student-quiz',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="container py-4" style="max-width: 700px;">
      <div class="mm-page-head">
        <div>
          <span class="mm-page-eyebrow">Quiz</span>
          <h1 class="mm-page-title">Évaluation</h1>
        </div>
      </div>

      <div *ngIf="loading" class="text-center py-5">
        <div class="spinner-border text-primary"></div>
        <p class="mt-2 mb-1">Chargement du quiz…</p>
        <p class="text-muted small mb-1">
          Temps écoulé : {{ loadingElapsedSeconds }} s.
          Arrêt automatique dans {{ loadingRemainingSeconds }} s si le serveur ne répond pas.
        </p>
        <p class="text-muted small mb-0" *ngIf="loadingElapsedSeconds >= 5">
          Le quiz est encore récupéré. La page affichera une erreur si le chargement dépasse {{ loadTimeoutSeconds }} s.
        </p>
      </div>

      <div *ngIf="!loading && errorMessage" class="alert alert-danger">
        {{ errorMessage }}
      </div>

      <div *ngIf="!loading && !errorMessage && allQuestions.length === 0" class="alert alert-warning">
        Ce quiz ne contient aucune question. Demandez à l'animateur de regénérer la séance.
      </div>

      <!-- Quiz en cours -->
      <div *ngIf="!loading && !errorMessage && !quizDone && currentQuestion">
        <div class="card mm-card-shadow">
          <div class="card-body">
            <p class="text-muted mb-3">Question {{ currentIndex + 1 }} / {{ allQuestions.length }}</p>
            <div style="background: #e9ecef; border-radius: 4px; height: 4px; margin-bottom: 1.5rem;">
              <div
                [style.width.%]="((currentIndex + 1) / allQuestions.length) * 100"
                style="height: 100%; background: #0d6efd; border-radius: 4px; transition: width 0.3s;">
              </div>
            </div>

            <h3 class="mb-3">{{ currentQuestion.content }}</h3>

            <div *ngIf="currentQuestion.type === 'mcq'" class="mb-3">
              <div *ngFor="let option of getOptions(currentQuestion)" class="form-check mb-2">
                <input
                  class="form-check-input"
                  type="radio"
                  [id]="'opt_' + option"
                  [value]="option"
                  [(ngModel)]="currentAnswer"
                  [name]="'question_' + currentQuestion.id"
                >
                <label class="form-check-label" [for]="'opt_' + option">{{ option }}</label>
              </div>
            </div>

            <div *ngIf="currentQuestion.type === 'dev'" class="mb-3">
              <textarea
                class="form-control"
                rows="5"
                [(ngModel)]="currentAnswer"
                placeholder="Écrivez votre réponse ici…"
              ></textarea>
              <div *ngIf="devSubmitted" class="mt-3 p-3 bg-light border rounded">
                <strong>Réponse attendue :</strong>
                <p>{{ currentQuestion.correctAnswer }}</p>
                <p class="text-muted small">{{ currentQuestion.explanation }}</p>
                <div class="d-flex gap-2 mt-2">
                  <button class="btn btn-success btn-sm" (click)="markDevCorrect(true)">J'ai eu l'essentiel</button>
                  <button class="btn btn-danger btn-sm" (click)="markDevCorrect(false)">J'ai manqué des éléments</button>
                </div>
              </div>
            </div>

            <div class="d-flex gap-2">
              <button
                *ngIf="currentQuestion.type === 'mcq' || !devSubmitted"
                class="btn btn-primary"
                (click)="submitAnswer()"
                [disabled]="!currentAnswer.trim()"
              >
                {{ currentQuestion.type === 'dev' && !devSubmitted ? 'Voir la réponse' : 'Suivant' }}
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- Résumé final -->
      <div *ngIf="quizDone" class="card mm-card-shadow">
        <div class="card-body text-center">
          <h2 class="mb-3">Quiz terminé !</h2>
          <p class="lead">Score : {{ correctCount }} / {{ allQuestions.length }}</p>

          <div class="mt-4 text-start">
            <h3 class="mm-panel-title">Résultats par axe</h3>
            <div *ngFor="let axisScore of axisResults" class="mb-3">
              <div class="d-flex justify-content-between align-items-center mb-1">
                <span>{{ axisScore.axisName }}</span>
                <span>
                  <span class="badge me-2" [class]="masteryBadge(axisScore.masteryStatus)">
                    {{ masteryLabel(axisScore.masteryStatus) }}
                  </span>
                  <span class="text-muted">{{ (axisScore.score * 100).toFixed(0) }}%</span>
                </span>
              </div>
              <div style="background: #e9ecef; border-radius: 4px; height: 8px;">
                <div
                  [style.width.%]="axisScore.score * 100"
                  [style.background-color]="axisScore.score >= 0.66 ? '#198754' : axisScore.score >= 0.33 ? '#ffc107' : '#dc3545'"
                  style="height: 100%; border-radius: 4px; transition: width 0.3s;">
                </div>
              </div>
            </div>

            <div *ngIf="weakAxes.length > 0" class="alert alert-warning mt-3">
              Des devoirs personnalisés ont été générés pour les axes :
              <strong>{{ weakAxes.join(', ') }}</strong>.
              Consultez la section "Devoirs en attente" dans votre espace.
            </div>
          </div>
        </div>
      </div>
    </div>
  `,
})
export class StudentQuizComponent implements OnInit, OnDestroy {
  private route = inject(ActivatedRoute);
  private tutoringService = inject(TutoringService);

  sessionId = 0;
  enfantId = 0;
  loading = true;
  session: any = null;
  allQuestions: any[] = [];
  currentIndex = 0;
  currentAnswer = '';
  devSubmitted = false;
  devCorrect = false;
  quizDone = false;
  correctCount = 0;
  axisResults: any[] = [];
  weakAxes: string[] = [];
  answers: any[] = [];
  errorMessage = '';
  questionStartedAt = Date.now();
  loadTimeoutSeconds = 20;
  loadingElapsedSeconds = 0;
  loadingRemainingSeconds = this.loadTimeoutSeconds;
  private loadingTimerId: ReturnType<typeof setInterval> | null = null;

  ngOnDestroy() {
    this.stopLoadingTimer();
  }

  ngOnInit() {
    this.sessionId = Number(this.route.snapshot.paramMap.get('sessionId'));
    this.enfantId = Number(this.route.snapshot.paramMap.get('enfantId'));

    if (!this.sessionId || !this.enfantId) {
      this.errorMessage = 'Lien de quiz invalide.';
      this.loading = false;
      return;
    }

    this.startLoadingTimer();

    this.tutoringService.getStudentSession(this.enfantId, this.sessionId).pipe(
      timeout(this.loadTimeoutSeconds * 1000),
    ).subscribe({
      next: (session) => {
        this.session = session;
        this.allQuestions = session.axes?.flatMap((a: any) => a.questions) ?? [];
        this.questionStartedAt = Date.now();
        this.loading = false;
        this.stopLoadingTimer();
      },
      error: (err) => {
        this.errorMessage = err?.name === 'TimeoutError'
          ? 'Le chargement du quiz prend trop de temps. Réessayez dans quelques secondes.'
          : err?.error?.error ?? 'Impossible de charger ce quiz.';
        this.loading = false;
        this.stopLoadingTimer();
      }
    });
  }

  private startLoadingTimer() {
    this.stopLoadingTimer();
    this.loadingElapsedSeconds = 0;
    this.loadingRemainingSeconds = this.loadTimeoutSeconds;
    this.loadingTimerId = setInterval(() => {
      this.loadingElapsedSeconds++;
      this.loadingRemainingSeconds = Math.max(0, this.loadTimeoutSeconds - this.loadingElapsedSeconds);
    }, 1000);
  }

  private stopLoadingTimer() {
    if (this.loadingTimerId) {
      clearInterval(this.loadingTimerId);
      this.loadingTimerId = null;
    }
  }

  get currentQuestion() {
    return this.allQuestions[this.currentIndex] ?? null;
  }

  getOptions(question: any): string[] {
    try { return JSON.parse(question.optionsJson); } catch { return []; }
  }

  submitAnswer() {
    const q = this.currentQuestion;
    if (!q) return;

    if (q.type === 'dev' && !this.devSubmitted) {
      this.devSubmitted = true;
      return;
    }

    const isCorrect = q.type === 'mcq'
      ? this.currentAnswer === q.correctAnswer
      : this.devCorrect;
    const responseTimeMs = Math.max(0, Date.now() - this.questionStartedAt);

    if (isCorrect) this.correctCount++;

    this.answers.push({
      questionId: q.id,
      answer: this.currentAnswer,
      correct: isCorrect,
      responseTimeMs
    });

    this.goToNext();
  }

  markDevCorrect(correct: boolean) {
    this.devCorrect = correct;
    this.submitAnswer();
  }

  goToNext() {
    this.currentAnswer = '';
    this.devSubmitted = false;
    this.devCorrect = false;

    if (this.currentIndex < this.allQuestions.length - 1) {
      this.currentIndex++;
      this.questionStartedAt = Date.now();
    } else {
      this.finishQuiz();
    }
  }

  finishQuiz() {
    this.quizDone = true;
    this.tutoringService.submitQuiz(this.enfantId, this.sessionId, this.answers).subscribe({
      next: () => {
        this.tutoringService.getScores(this.enfantId).subscribe({
          next: (scores) => {
            this.axisResults = scores;
            this.weakAxes = scores
              .filter((s: any) => s.masteryStatus === 'weak' || s.score < 0.33)
              .map((s: any) => s.axisName);
          }
        });
      }
    });
  }

  masteryLabel(status: string): string {
    switch (status) {
      case 'mastered': return 'Maîtrisé';
      case 'learning': return 'En apprentissage';
      default: return 'À renforcer';
    }
  }

  masteryBadge(status: string): string {
    switch (status) {
      case 'mastered': return 'bg-success';
      case 'learning': return 'bg-warning text-dark';
      default: return 'bg-danger';
    }
  }
}
