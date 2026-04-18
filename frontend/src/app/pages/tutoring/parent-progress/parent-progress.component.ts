import { Component, inject, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { CommonModule } from '@angular/common';
import { TutoringService } from '../../../core/services/tutoring.service';

@Component({
  selector: 'app-parent-progress',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="container py-4">
      <div class="mm-page-head">
        <div>
          <span class="mm-page-eyebrow">Tutorat adaptatif</span>
          <h1 class="mm-page-title">Progression de votre enfant</h1>
          <p class="mm-page-subtitle">Suivi des axes pédagogiques du tutorat.</p>
        </div>
      </div>

      <div *ngIf="loading" class="text-center py-5">
        <div class="spinner-border text-primary"></div>
      </div>

      <div *ngIf="!loading && scores.length === 0" class="alert alert-info">
        Votre enfant n'a pas encore participé à une séance de tutorat.
      </div>

      <!-- Scores par axe -->
      <div *ngIf="!loading && scores.length > 0" class="card mm-card-shadow">
        <div class="card-body">
          <h2 class="mm-panel-title">Résultats par axe pédagogique</h2>
          <div *ngFor="let score of scores" class="mb-4">
            <div class="d-flex justify-content-between align-items-center mb-1">
              <span class="fw-semibold">{{ score.axisName }}</span>
              <span>
                <span class="badge" [class]="masteryBadge(score.masteryStatus)">
                  {{ masteryLabel(score.masteryStatus) }}
                </span>
                <span class="ms-2 text-muted">{{ (score.score * 100).toFixed(0) }}%</span>
              </span>
            </div>
            <div style="background: #e9ecef; border-radius: 4px; height: 8px; width: 100%;">
              <div
                [style.width.%]="score.score * 100"
                [style.background-color]="score.score >= 0.66 ? '#198754' : score.score >= 0.33 ? '#ffc107' : '#dc3545'"
                style="height: 100%; border-radius: 4px; transition: width 0.3s;">
              </div>
            </div>
            <small class="text-muted">Mis à jour le {{ score.updatedAt | date:'dd/MM/yyyy' }}</small>
          </div>
        </div>
      </div>

      <!-- Historique des 10 derniers événements -->
      <div *ngIf="!loading && history.length > 0" class="card mm-card-shadow mt-4">
        <div class="card-body">
          <h2 class="mm-panel-title">Historique récent</h2>
          <ul class="list-group list-group-flush">
            <li *ngFor="let ev of history" class="list-group-item d-flex justify-content-between align-items-center">
              <span>
                <span class="badge me-2"
                  [class.bg-primary]="ev.type === 'quiz'"
                  [class.bg-secondary]="ev.type === 'homework'">
                  {{ ev.type === 'quiz' ? 'Quiz' : 'Devoir' }}
                </span>
                {{ ev.label }}
              </span>
              <small class="text-muted">{{ ev.date | date:'dd/MM/yyyy' }}</small>
            </li>
          </ul>
        </div>
      </div>
    </div>
  `,
})
export class ParentProgressComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private tutoringService = inject(TutoringService);

  enfantId = 0;
  scores: any[] = [];
  history: any[] = [];
  loading = true;

  ngOnInit() {
    this.enfantId = Number(this.route.snapshot.paramMap.get('enfantId'));
    this.tutoringService.getParentProgress(this.enfantId).subscribe({
      next: (scores) => {
        this.scores = scores;
        this.loading = false;
      },
      error: () => { this.loading = false; }
    });
    this.tutoringService.getHistory(this.enfantId).subscribe({
      next: (h) => { this.history = h; },
      error: () => {}
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
