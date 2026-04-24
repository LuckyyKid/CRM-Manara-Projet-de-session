import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { HomeworkAttemptDto, HomeworkDto } from '../../../core/models/api.models';
import { AnimateurService } from '../../../core/services/animateur.service';

@Component({
  selector: 'app-animateur-homework-assignment-detail',
  imports: [CommonModule, RouterLink],
  templateUrl: './animateur-homework-assignment-detail.component.html',
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
