import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { HomeworkAttemptDto, HomeworkDto } from '../../../core/models/api.models';
import { AnimateurService } from '../../../core/services/animateur.service';

@Component({
  selector: 'app-animateur-homework-assignment-detail',
  imports: [CommonModule, RouterLink],
  template: `
    <div class="container py-4">
      <div class="mm-page-head">
        <div>
          <span class="mm-page-eyebrow">Detail du devoir</span>
          <h1 class="mm-page-title fs-1">{{ assignment()?.title || 'Devoir' }}</h1>
          <p class="mm-page-subtitle" *ngIf="assignment() as item">
            {{ item.enfantName }}<span *ngIf="item.activityName"> - {{ item.activityName }}</span>
          </p>
        </div>
        <div class="mm-page-actions">
          <a class="btn btn-outline-primary" *ngIf="assignment() as item" [routerLink]="['/animateur/homeworks/students', item.enfantId]">Retour a l'etudiant</a>
          <a class="btn btn-primary" routerLink="/animateur/homeworks">Vue devoirs</a>
        </div>
      </div>

      <div *ngIf="error()" class="alert alert-danger">{{ error() }}</div>
      <div *ngIf="loading()" class="text-secondary py-4">Chargement...</div>

      <ng-container *ngIf="!loading() && assignment() as item">
        <div class="row g-3 mb-4">
          <div class="col-12 col-md-3">
            <div class="mm-kpi-card h-100">
              <span class="mm-kpi-label">Statut</span>
              <span class="mm-kpi-value">{{ latestAttempt() ? 'Soumis' : 'A faire' }}</span>
              <span class="mm-kpi-meta">{{ latestAttemptStatus() }}</span>
            </div>
          </div>
          <div class="col-12 col-md-3">
            <div class="mm-kpi-card h-100">
              <span class="mm-kpi-label">Score</span>
              <span class="mm-kpi-value">{{ formatScore(latestAttempt()?.scorePercent) }}</span>
              <span class="mm-kpi-meta">Correction du dernier rendu</span>
            </div>
          </div>
          <div class="col-12 col-md-3">
            <div class="mm-kpi-card h-100">
              <span class="mm-kpi-label">Temps</span>
              <span class="mm-kpi-value">{{ formatTime(latestAttempt()?.elapsedSeconds) }}</span>
              <span class="mm-kpi-meta">Derniere soumission</span>
            </div>
          </div>
          <div class="col-12 col-md-3">
            <div class="mm-kpi-card h-100">
              <span class="mm-kpi-label">Exercices</span>
              <span class="mm-kpi-value">{{ item.exercises.length }}</span>
              <span class="mm-kpi-meta">Dans le devoir</span>
            </div>
          </div>
        </div>

        <div class="card mm-card-shadow mb-4">
          <div class="card-body">
            <h2 class="mm-panel-title">Consignes</h2>
            <p class="mb-0">{{ item.summary }}</p>
          </div>
        </div>

        <div class="card mm-card-shadow">
          <div class="card-body">
            <h2 class="mm-panel-title">Reponses et details</h2>
            <p class="mm-panel-subtitle" *ngIf="latestAttempt(); else noAttempt">
              Reponses de l'etudiant comparees a l'attendu du devoir.
            </p>

            <ng-template #noAttempt>
              <p class="text-secondary mb-0">Ce devoir a ete attribue mais n'a pas encore ete soumis.</p>
            </ng-template>

            <div class="vstack gap-3 mt-3" *ngIf="latestAttempt() as attempt">
              <article class="border rounded p-3" *ngFor="let answer of attempt.answers">
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
export class AnimateurHomeworkAssignmentDetailComponent implements OnInit {
  private readonly animateurService = inject(AnimateurService);
  private readonly route = inject(ActivatedRoute);

  readonly assignment = signal<HomeworkDto | null>(null);
  readonly latestAttempt = signal<HomeworkAttemptDto | null>(null);
  readonly loading = signal(true);
  readonly error = signal('');

  readonly latestAttemptStatus = computed(() => {
    const attempt = this.latestAttempt();
    if (!attempt) {
      return 'Pas encore de soumission';
    }
    if (attempt.status === 'PENDING_AI' || attempt.scorePercent === null || attempt.scorePercent === undefined) {
      return 'Correction IA en attente';
    }
    return attempt.status === 'SCORED_AI' ? 'Corrige par IA' : attempt.status;
  });

  ngOnInit(): void {
    const assignmentId = Number(this.route.snapshot.paramMap.get('assignmentId'));
    if (!assignmentId) {
      this.error.set('Devoir introuvable.');
      this.loading.set(false);
      return;
    }

    this.animateurService.getHomeworkAssignment(assignmentId).subscribe({
      next: (assignment) => {
        this.assignment.set(assignment);
        this.loadLatestAttempt(assignmentId);
      },
      error: () => {
        this.error.set('Erreur lors du chargement du devoir.');
        this.loading.set(false);
      },
    });
  }

  private loadLatestAttempt(assignmentId: number): void {
    this.animateurService.getHomeworkLatestAttempt(assignmentId).subscribe({
      next: (attempt) => {
        this.latestAttempt.set(attempt);
        this.loading.set(false);
      },
      error: (error: HttpErrorResponse) => {
        if (error.status !== 404) {
          this.error.set('Erreur lors du chargement de la soumission du devoir.');
        }
        this.loading.set(false);
      },
    });
  }

  formatScore(score: number | null | undefined): string {
    return score === null || score === undefined ? 'En attente IA' : `${Math.round(score)}%`;
  }

  formatTime(seconds: number | null | undefined): string {
    if (seconds === null || seconds === undefined) {
      return 'Indisponible';
    }
    const minutes = Math.floor(seconds / 60);
    const rest = seconds % 60;
    return minutes > 0 ? `${minutes}min ${rest}s` : `${rest}s`;
  }
}
