import { CommonModule, DatePipe } from '@angular/common';
import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { HomeworkAttemptDto } from '../../../core/models/api.models';
import { ParentService } from '../../../core/services/parent.service';

@Component({
  selector: 'app-parent-homework-attempt-detail',
  imports: [CommonModule, DatePipe, RouterLink],
  template: `
    <div class="container py-4">
      <div class="mm-page-head">
        <div>
          <span class="mm-page-eyebrow">Resultat du devoir</span>
          <h1 class="mm-page-title fs-1">{{ attempt()?.assignmentTitle || 'Detail du devoir' }}</h1>
          <p class="mm-page-subtitle" *ngIf="attempt() as item">
            {{ item.enfantName }} - soumis le {{ item.submittedAt | date:'dd/MM/yyyy HH:mm' }}
          </p>
        </div>
        <div class="mm-page-actions">
          <a class="btn btn-outline-primary" routerLink="/parent/homeworks">Retour aux devoirs</a>
        </div>
      </div>

      <div *ngIf="error()" class="alert alert-danger">{{ error() }}</div>
      <div *ngIf="loading()" class="text-secondary py-4">Chargement...</div>

      <ng-container *ngIf="!loading() && attempt() as item">
        <div class="row g-3 mb-4">
          <div class="col-12 col-md-4">
            <div class="mm-kpi-card h-100">
              <span class="mm-kpi-label">Note</span>
              <span class="mm-kpi-value">{{ scoreLabel() }}</span>
              <span class="mm-kpi-meta">{{ statusLabel(item.status) }}</span>
            </div>
          </div>
          <div class="col-12 col-md-4">
            <div class="mm-kpi-card h-100">
              <span class="mm-kpi-label">Temps pris</span>
              <span class="mm-kpi-value">{{ formatTime(item.elapsedSeconds) }}</span>
              <span class="mm-kpi-meta">Temps de reponse</span>
            </div>
          </div>
          <div class="col-12 col-md-4">
            <div class="mm-kpi-card h-100">
              <span class="mm-kpi-label">Exercices</span>
              <span class="mm-kpi-value">{{ item.answers.length }}</span>
              <span class="mm-kpi-meta">Reponses soumises</span>
            </div>
          </div>
        </div>

        <div class="card mm-card-shadow">
          <div class="card-body">
            <h2 class="mm-panel-title">Reponses detaillees</h2>
            <div class="vstack gap-3">
              <article class="border rounded p-3" *ngFor="let answer of item.answers">
                <div class="fw-semibold">{{ answer.axisTitle }}</div>
                <div class="text-secondary small mb-2">{{ answer.angle }}</div>
                <p class="mb-2">{{ answer.questionText }}</p>
                <div class="bg-light rounded p-3 mb-2">
                  <div class="small text-secondary mb-1">Reponse de l'etudiant</div>
                  <div>{{ answer.answerText || '-' }}</div>
                </div>
                <div class="small text-secondary">Reponse attendue</div>
                <div>{{ answer.expectedAnswer }}</div>
              </article>
            </div>
          </div>
        </div>
      </ng-container>
    </div>
  `,
})
export class ParentHomeworkAttemptDetailComponent implements OnInit {
  private readonly parentService = inject(ParentService);
  private readonly route = inject(ActivatedRoute);

  readonly attempt = signal<HomeworkAttemptDto | null>(null);
  readonly loading = signal(true);
  readonly error = signal('');

  readonly scoreLabel = computed(() => {
    const score = this.attempt()?.scorePercent;
    return score === null || score === undefined ? 'Correction en attente' : `${Math.round(score)}%`;
  });

  ngOnInit(): void {
    const attemptId = Number(this.route.snapshot.paramMap.get('attemptId'));
    if (!attemptId) {
      this.error.set('Soumission introuvable.');
      this.loading.set(false);
      return;
    }

    this.parentService.getHomeworkAttempt(attemptId).subscribe({
      next: (attempt) => {
        this.attempt.set(attempt);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Erreur lors du chargement de la soumission.');
        this.loading.set(false);
      },
    });
  }

  formatTime(seconds: number | null | undefined): string {
    if (seconds === null || seconds === undefined) {
      return 'Non disponible';
    }
    const minutes = Math.floor(seconds / 60);
    const remainingSeconds = seconds % 60;
    return minutes <= 0 ? `${remainingSeconds}s` : `${minutes}min ${remainingSeconds}s`;
  }

  statusLabel(status: string): string {
    return status === 'SCORED_LOCAL' ? 'Corrige localement' : status || 'Soumis';
  }
}
